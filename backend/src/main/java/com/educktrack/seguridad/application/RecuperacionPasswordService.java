package com.educktrack.seguridad.application;

import com.educktrack.auditoria.application.AuditoriaService;
import com.educktrack.auditoria.domain.TipoOperacion;
import com.educktrack.seguridad.domain.evento.EventosDeSeguridad.PasswordRestablecida;
import com.educktrack.seguridad.infrastructure.persistence.TokenRecuperacionJpaEntity;
import com.educktrack.seguridad.infrastructure.persistence.TokenRecuperacionRepository;
import com.educktrack.shared.domain.ReglaNegocioException;
import com.educktrack.usuarios.domain.PoliticaPassword;
import com.educktrack.usuarios.infrastructure.persistence.UsuarioJpaEntity;
import com.educktrack.usuarios.infrastructure.persistence.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Caso de uso de recuperacion de contrasena (RF-64, HU-04).
 *
 * <p>Hasta la Fase 7 el endpoint existia pero no hacia nada: recibia un cuerpo,
 * lo ignoraba y devolvia un mensaje fijo. Ninguno de los criterios de HU-04
 * estaba implementado.</p>
 *
 * <h2>Las dos propiedades que sostienen todo lo demas</h2>
 *
 * <ol>
 *   <li><strong>Solicitar no revela nada.</strong> La respuesta es identica
 *       exista o no la cuenta, este activa o no, y salga o no el correo. Un
 *       endpoint publico que responde distinto segun el caso es un oraculo para
 *       averiguar que correos estan dados de alta, por el mismo motivo por el
 *       que el acceso fallido no distingue "no existe" de "contrasena erronea"
 *       (Fase 4).</li>
 *   <li><strong>El token solo existe en claro en el correo.</strong> En base de
 *       datos vive su hash (V13), y ni el log de auditoria ni los eventos lo
 *       llevan: un enlace vivo permite tomar la cuenta sin conocer la
 *       contrasena, de modo que anotarlo seria repartir la credencial.</li>
 * </ol>
 */
@Service
public class RecuperacionPasswordService {

    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final int BYTES_TOKEN = 32; // 256 bits

    private final UsuarioRepository usuarioRepository;
    private final TokenRecuperacionRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EnvioEnlaceRecuperacion envio;
    private final AuditoriaService auditoria;
    private final ApplicationEventPublisher eventos;
    private final int minutosValidez;
    private final int maxSolicitudes;
    private final int ventanaMinutos;

    public RecuperacionPasswordService(UsuarioRepository usuarioRepository,
                                       TokenRecuperacionRepository tokenRepository,
                                       PasswordEncoder passwordEncoder,
                                       EnvioEnlaceRecuperacion envio,
                                       AuditoriaService auditoria,
                                       ApplicationEventPublisher eventos,
                                       @Value("${educktrack.seguridad.recuperacion.minutos-validez:30}")
                                       int minutosValidez,
                                       @Value("${educktrack.seguridad.recuperacion.max-solicitudes:3}")
                                       int maxSolicitudes,
                                       @Value("${educktrack.seguridad.recuperacion.ventana-minutos:15}")
                                       int ventanaMinutos) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.envio = envio;
        this.auditoria = auditoria;
        this.eventos = eventos;
        this.minutosValidez = minutosValidez;
        this.maxSolicitudes = maxSolicitudes;
        this.ventanaMinutos = ventanaMinutos;
    }

    /**
     * RF-64 / HU-04: emite un enlace de recuperacion valido {@code minutosValidez}
     * minutos si el correo corresponde a una cuenta activa.
     *
     * <p>No devuelve nada y no falla nunca por el estado de la cuenta: quien
     * llama no debe poder distinguir los casos.</p>
     */
    @Transactional
    public void solicitar(String correo) {
        Optional<UsuarioJpaEntity> encontrado = usuarioRepository.findByCorreoInstitucional(correo);
        if (encontrado.isEmpty() || !encontrado.get().isActivo()) {
            // HU-04: "los intentos de recuperacion fallidos quedan registrados".
            // Se anota el correo tal cual se pidio, que es el dato util para
            // detectar a alguien probando cuentas.
            auditoria.registrarPeseARollback(TipoOperacion.RECUPERACION_FALLIDA,
                    "Solicitud de recuperacion para un correo que no corresponde a una cuenta activa: "
                            + correo + ".");
            return;
        }

        UsuarioJpaEntity usuario = encontrado.get();
        LocalDateTime ahora = LocalDateTime.now();

        // Sin limite, cualquiera puede pedir enlaces en bucle para un correo
        // conocido y llenarle el buzon. Se corta en silencio, con la misma
        // respuesta de siempre: contestar "demasiadas peticiones" confirmaria
        // que ese correo tiene cuenta, que es justo lo que el endpoint evita.
        int recientes = tokenRepository.countByUsuarioIdAndFechaSolicitudAfter(
                usuario.getId(), ahora.minusMinutes(ventanaMinutos));
        if (recientes >= maxSolicitudes) {
            auditoria.registrarPeseARollback(TipoOperacion.RECUPERACION_FALLIDA,
                    "Se supero el limite de " + maxSolicitudes + " solicitudes de recuperacion en "
                            + ventanaMinutos + " minutos para la cuenta " + usuario.getId() + ".");
            return;
        }

        // Un enlace nuevo anula los anteriores: si no, un correo antiguo
        // reenviado o filtrado seguiria abriendo la cuenta.
        tokenRepository.invalidarPendientes(usuario.getId(), ahora);

        String token = generarToken();
        TokenRecuperacionJpaEntity registro = new TokenRecuperacionJpaEntity();
        registro.setUsuarioId(usuario.getId());
        registro.setTokenHash(hash(token));
        registro.setFechaSolicitud(ahora);
        registro.setFechaExpiracion(ahora.plusMinutes(minutosValidez));
        tokenRepository.save(registro);

        envio.enviar(usuario.getCorreoInstitucional(), token, minutosValidez);

        // El log anota que se pidio, nunca el token.
        auditoria.registrar(TipoOperacion.RECUPERACION_SOLICITADA, "usuario", usuario.getId(),
                "Se emitio un enlace de recuperacion de contrasena, valido "
                        + minutosValidez + " minutos.");
    }

    /**
     * RF-64 / HU-04: restablece la contrasena consumiendo el enlace.
     *
     * <p>El enlace caduca a los {@code minutosValidez} minutos y ademas se
     * consume al primer uso, que son dos condiciones distintas: la primera
     * limita cuanto tiempo vive la credencial en el buzon, la segunda impide
     * que quien lea el correo mas tarde la vuelva a usar.</p>
     */
    @Transactional
    public void restablecer(String token, String nuevaPassword) {
        Optional<TokenRecuperacionJpaEntity> encontrado = token == null || token.isBlank()
                ? Optional.empty()
                : tokenRepository.findByTokenHash(hash(token));

        LocalDateTime ahora = LocalDateTime.now();
        if (encontrado.isEmpty()
                || encontrado.get().getFechaUso() != null
                || encontrado.get().getFechaExpiracion().isBefore(ahora)) {
            // El registro debe sobrevivir al rollback que provoca la excepcion:
            // el intento que mas interesa auditar es justo el que falla (Fase 4).
            auditoria.registrarPeseARollback(TipoOperacion.RECUPERACION_FALLIDA,
                    "Intento de restablecer una contrasena con un enlace invalido, ya usado o caducado.");
            throw new ReglaNegocioException("HU-04",
                    "El enlace de recuperacion no es valido, ya se uso o caduco. Solicite uno nuevo.");
        }

        // Se valida antes de tocar nada, para no consumir el enlace por una
        // contrasena que la politica va a rechazar: obligaria a pedir otro.
        PoliticaPassword.exigirCumplimiento(nuevaPassword);

        TokenRecuperacionJpaEntity registro = encontrado.get();
        UsuarioJpaEntity usuario = usuarioRepository.findById(registro.getUsuarioId())
                .orElseThrow(() -> new ReglaNegocioException("HU-04",
                        "El enlace de recuperacion no es valido. Solicite uno nuevo."));

        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword)); // RS-05
        // La contrasena la acaba de elegir la persona: no hay nada que forzarle
        // a cambiar en el siguiente acceso.
        usuario.setDebeCambiarPassword(false);
        usuarioRepository.save(usuario);

        registro.setFechaUso(ahora); // HU-04: de un solo uso.
        tokenRepository.save(registro);

        auditoria.registrar(TipoOperacion.PASSWORD_RESTABLECIDA, "usuario", usuario.getId(),
                "Contrasena restablecida mediante enlace de recuperacion.");

        // HU-04: avisar de que el cambio ocurrio. Si quien lo recibe no lo pidio,
        // es su unica senal de que alguien mas entro a su correo.
        eventos.publishEvent(new PasswordRestablecida(usuario.getId(), usuario.getCorreoInstitucional()));
    }

    private static String generarToken() {
        byte[] bytes = new byte[BYTES_TOKEN];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256 y no BCrypt, a diferencia de la contrasena: BCrypt es lento a
     * proposito para que no se pueda adivinar un secreto de baja entropia
     * elegido por una persona, mientras que aqui el token son 256 bits
     * aleatorios y no hay diccionario que probar. Ademas BCrypt lleva sal
     * propia, lo que obligaria a recorrer la tabla fila por fila en lugar de
     * buscar por indice.
     */
    private static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible en esta JVM", ex);
        }
    }
}
