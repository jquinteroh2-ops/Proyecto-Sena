package com.educktrack.auditoria.application;

import com.educktrack.auditoria.domain.TipoOperacion;
import com.educktrack.auditoria.infrastructure.persistence.AuditoriaJpaEntity;
import com.educktrack.auditoria.infrastructure.persistence.AuditoriaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Registro del log de auditoria (RS-07, RF-63).
 *
 * <p>RS-07 exige que toda operacion critica quede registrada con usuario, fecha
 * y descripcion. Este servicio es el unico punto por el que se escribe el log,
 * de modo que el formato del registro no dependa de quien lo llame.</p>
 *
 * <h2>Decisiones de diseno</h2>
 *
 * <p><strong>El registro participa de la transaccion del llamador.</strong> Si
 * la operacion se deshace, su anotacion en el log tambien: auditar algo que
 * finalmente no ocurrio es peor que no auditarlo, porque induce a error a quien
 * despues revisa el historial. La unica excepcion es
 * {@link #registrarAcceso(String, boolean, String)}, que abre transaccion
 * propia porque un intento de acceso fallido debe quedar anotado precisamente
 * cuando la operacion no prospera.</p>
 *
 * <p><strong>El usuario se resuelve del contexto de seguridad</strong>, no lo
 * pasa el llamador. Un log donde cada servicio decide que nombre escribe es un
 * log falsificable por descuido; asi la atribucion es siempre la del token que
 * ejecuto la peticion. Cuando no hay sesion (arranque, tareas del sistema) se
 * anota {@code sistema}.</p>
 */
@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    /** Autor de las operaciones que no nacen de una peticion autenticada. */
    public static final String USUARIO_SISTEMA = "sistema";

    private static final int MAX_DESCRIPCION = 500;

    private final AuditoriaRepository repository;

    public AuditoriaService(AuditoriaRepository repository) {
        this.repository = repository;
    }

    /**
     * RF-63: registra una operacion critica sobre un registro concreto.
     *
     * @param operacion   que se hizo (RS-07)
     * @param entidad     concepto afectado ("calificacion", "estudiante")
     * @param entidadId   identificador del registro afectado
     * @param descripcion que cambio, en terminos legibles para quien audita
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void registrar(TipoOperacion operacion, String entidad, Long entidadId, String descripcion) {
        guardar(usuarioActual(), operacion, entidad, entidadId, descripcion);
    }

    /**
     * RF-05: registra un intento de inicio de sesion, prospere o no.
     *
     * <p>Abre transaccion propia ({@code REQUIRES_NEW}) porque el caso que mas
     * importa auditar es precisamente el que falla, y no debe arrastrarlo el
     * rollback de la autenticacion rechazada.</p>
     *
     * @param correo   cuenta con la que se intento entrar, exista o no
     * @param exitoso  si la autenticacion prospero
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarAcceso(String correo, boolean exitoso, String descripcion) {
        TipoOperacion operacion = exitoso ? TipoOperacion.ACCESO_EXITOSO : TipoOperacion.ACCESO_FALLIDO;
        // El autor es la cuenta que intenta entrar: en un acceso todavia no hay
        // contexto de seguridad del que resolverlo.
        guardar(correo == null || correo.isBlank() ? USUARIO_SISTEMA : correo,
                operacion, null, null, descripcion);
    }

    private void guardar(String usuario, TipoOperacion operacion, String entidad,
                         Long entidadId, String descripcion) {
        AuditoriaJpaEntity registro = new AuditoriaJpaEntity();
        registro.setUsuario(recortar(usuario, 150));
        registro.setOperacion(operacion);
        registro.setEntidad(entidad);
        registro.setEntidadId(entidadId);
        registro.setDescripcion(recortar(descripcion, MAX_DESCRIPCION));
        registro.setFecha(LocalDateTime.now());
        repository.save(registro);
    }

    /**
     * Correo del token que ejecuta la peticion, o {@code sistema} si no hay
     * ninguno. No lanza: dejar sin auditar es malo, pero tumbar la operacion
     * de negocio por no poder resolver el nombre lo es mas.
     */
    private String usuarioActual() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
                return USUARIO_SISTEMA;
            }
            return auth.getName();
        } catch (RuntimeException ex) {
            log.warn("No se pudo resolver el usuario para el log de auditoria: {}", ex.getMessage());
            return USUARIO_SISTEMA;
        }
    }

    /**
     * La descripcion la componen los servicios de negocio y puede incluir texto
     * escrito por el usuario (motivos de retiro, de justificacion). Recortar
     * evita que un motivo largo aborte la operacion que se esta auditando.
     */
    private static String recortar(String texto, int maximo) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= maximo ? texto : texto.substring(0, maximo - 3) + "...";
    }
}
