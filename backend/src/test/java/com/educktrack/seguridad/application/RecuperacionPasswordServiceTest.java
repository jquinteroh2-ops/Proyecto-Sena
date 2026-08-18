package com.educktrack.seguridad.application;

import com.educktrack.auditoria.application.AuditoriaService;
import com.educktrack.auditoria.domain.TipoOperacion;
import com.educktrack.seguridad.domain.evento.EventosDeSeguridad.PasswordRestablecida;
import com.educktrack.seguridad.infrastructure.persistence.TokenRecuperacionJpaEntity;
import com.educktrack.seguridad.infrastructure.persistence.TokenRecuperacionRepository;
import com.educktrack.shared.domain.ReglaNegocioException;
import com.educktrack.usuarios.infrastructure.persistence.UsuarioJpaEntity;
import com.educktrack.usuarios.infrastructure.persistence.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de la recuperacion de contrasena (RF-64, HU-04).
 *
 * <p>Lo que se fija aqui son las propiedades de seguridad de las que depende
 * todo lo demas: que solicitar no revele si la cuenta existe, que el token no
 * quede almacenado en claro, que el enlace se consuma una sola vez y que los
 * intentos fallidos queden registrados.</p>
 */
@ExtendWith(MockitoExtension.class)
class RecuperacionPasswordServiceTest {

    private static final int MINUTOS = 30;
    private static final Long USUARIO_ID = 42L;
    private static final String CORREO = "docente@colegio.edu.co";
    private static final String PASSWORD_NUEVA = "nuevaClave123";

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private TokenRecuperacionRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EnvioEnlaceRecuperacion envio;
    @Mock private AuditoriaService auditoria;
    @Mock private ApplicationEventPublisher eventos;

    private RecuperacionPasswordService service;

    @BeforeEach
    void prepararServicio() {
        // Los minutos de validez son un @Value, no un colaborador.
        service = new RecuperacionPasswordService(usuarioRepository, tokenRepository,
                passwordEncoder, envio, auditoria, eventos, MINUTOS);
    }

    // ---------- solicitar ----------

    @Test
    void noEmiteEnlaceNiRevelaNadaSiElCorreoNoTieneCuenta() {
        when(usuarioRepository.findByCorreoInstitucional(CORREO)).thenReturn(Optional.empty());

        // No lanza: la respuesta debe ser indistinguible del caso con cuenta.
        service.solicitar(CORREO);

        verify(tokenRepository, never()).save(any());
        verify(envio, never()).enviar(anyString(), anyString(), anyInt());
        // HU-04: "los intentos de recuperacion fallidos quedan registrados".
        verify(auditoria).registrarPeseARollback(eq(TipoOperacion.RECUPERACION_FALLIDA), anyString());
    }

    @Test
    void noEmiteEnlaceParaUnaCuentaDesactivada() {
        UsuarioJpaEntity usuario = usuario();
        usuario.setActivo(false);
        when(usuarioRepository.findByCorreoInstitucional(CORREO)).thenReturn(Optional.of(usuario));

        service.solicitar(CORREO);

        verify(tokenRepository, never()).save(any());
        verify(auditoria).registrarPeseARollback(eq(TipoOperacion.RECUPERACION_FALLIDA), anyString());
    }

    @Test
    void guardaElHashDelTokenYNoElTokenEnviado() {
        when(usuarioRepository.findByCorreoInstitucional(CORREO)).thenReturn(Optional.of(usuario()));

        service.solicitar(CORREO);

        String tokenEnviado = capturarTokenEnviado();
        TokenRecuperacionJpaEntity guardado = capturarTokenGuardado();

        // Un enlace vivo permite tomar la cuenta sin conocer la contrasena:
        // guardarlo en claro seria repartir la credencial (V13).
        assertNotEquals(tokenEnviado, guardado.getTokenHash());
        assertEquals(sha256(tokenEnviado), guardado.getTokenHash());
        assertNull(guardado.getFechaUso());
    }

    @Test
    void elEnlaceCaducaALosMinutosConfigurados() {
        when(usuarioRepository.findByCorreoInstitucional(CORREO)).thenReturn(Optional.of(usuario()));

        service.solicitar(CORREO);

        TokenRecuperacionJpaEntity guardado = capturarTokenGuardado();
        long minutos = java.time.Duration.between(
                guardado.getFechaSolicitud(), guardado.getFechaExpiracion()).toMinutes();
        assertEquals(MINUTOS, minutos);
    }

    @Test
    void unEnlaceNuevoAnulaLosAnterioresDeEsaCuenta() {
        // Si pedir otro enlace no anulara el anterior, un correo antiguo
        // reenviado o filtrado seguiria abriendo la cuenta.
        when(usuarioRepository.findByCorreoInstitucional(CORREO)).thenReturn(Optional.of(usuario()));

        service.solicitar(CORREO);

        verify(tokenRepository).invalidarPendientes(eq(USUARIO_ID), any(LocalDateTime.class));
    }

    // ---------- restablecer ----------

    @Test
    void restableceLaPasswordYConsumeElEnlace() {
        String token = "token-de-prueba";
        tokenVigente(token);
        UsuarioJpaEntity usuario = usuario();
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode(PASSWORD_NUEVA)).thenReturn("$2a$10$hash");

        service.restablecer(token, PASSWORD_NUEVA);

        assertEquals("$2a$10$hash", usuario.getPasswordHash()); // RS-05
        // La contrasena la acaba de elegir la persona: nada que forzar despues.
        assertTrue(!usuario.isDebeCambiarPassword());
        assertNotNull(capturarTokenGuardado().getFechaUso()); // HU-04: un solo uso
        verify(eventos).publishEvent(any(PasswordRestablecida.class));
        verify(auditoria).registrar(eq(TipoOperacion.PASSWORD_RESTABLECIDA), eq("usuario"),
                eq(USUARIO_ID), anyString());
    }

    @Test
    void rechazaUnEnlaceYaUsadoYRegistraElIntento() {
        String token = "token-gastado";
        TokenRecuperacionJpaEntity registro = tokenVigente(token);
        registro.setFechaUso(LocalDateTime.now().minusMinutes(1));

        assertThrows(ReglaNegocioException.class, () -> service.restablecer(token, PASSWORD_NUEVA));

        verify(auditoria).registrarPeseARollback(eq(TipoOperacion.RECUPERACION_FALLIDA), anyString());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void rechazaUnEnlaceCaducado() {
        String token = "token-viejo";
        TokenRecuperacionJpaEntity registro = tokenVigente(token);
        registro.setFechaExpiracion(LocalDateTime.now().minusMinutes(1));

        assertThrows(ReglaNegocioException.class, () -> service.restablecer(token, PASSWORD_NUEVA));

        verify(auditoria).registrarPeseARollback(eq(TipoOperacion.RECUPERACION_FALLIDA), anyString());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void rechazaUnTokenDesconocido() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThrows(ReglaNegocioException.class, () -> service.restablecer("inventado", PASSWORD_NUEVA));

        verify(auditoria).registrarPeseARollback(eq(TipoOperacion.RECUPERACION_FALLIDA), anyString());
    }

    @Test
    void noConsumeElEnlaceSiLaPasswordIncumpleLaPolitica() {
        // Importa el orden: consumir el enlace antes de validar obligaria a
        // pedir otro por haber escrito una contrasena demasiado corta.
        String token = "token-valido";
        tokenVigente(token);

        assertThrows(ReglaNegocioException.class, () -> service.restablecer(token, "corta"));

        verify(tokenRepository, never()).save(any());
        verify(usuarioRepository, never()).save(any());
    }

    // ---------- utilidades ----------

    private TokenRecuperacionJpaEntity tokenVigente(String token) {
        TokenRecuperacionJpaEntity registro = new TokenRecuperacionJpaEntity();
        registro.setId(1L);
        registro.setUsuarioId(USUARIO_ID);
        registro.setTokenHash(sha256(token));
        registro.setFechaSolicitud(LocalDateTime.now().minusMinutes(5));
        registro.setFechaExpiracion(LocalDateTime.now().plusMinutes(25));
        lenient().when(tokenRepository.findByTokenHash(sha256(token))).thenReturn(Optional.of(registro));
        return registro;
    }

    private String capturarTokenEnviado() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(envio).enviar(eq(CORREO), captor.capture(), eq(MINUTOS));
        return captor.getValue();
    }

    private TokenRecuperacionJpaEntity capturarTokenGuardado() {
        ArgumentCaptor<TokenRecuperacionJpaEntity> captor =
                ArgumentCaptor.forClass(TokenRecuperacionJpaEntity.class);
        verify(tokenRepository).save(captor.capture());
        return captor.getValue();
    }

    private static UsuarioJpaEntity usuario() {
        UsuarioJpaEntity u = new UsuarioJpaEntity();
        u.setId(USUARIO_ID);
        u.setNombre("Ana Ruiz");
        u.setCorreoInstitucional(CORREO);
        u.setPasswordHash("$2a$10$anterior");
        u.setActivo(true);
        u.setDebeCambiarPassword(true);
        u.setFechaCreacion(LocalDateTime.now());
        return u;
    }

    private static String sha256(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
