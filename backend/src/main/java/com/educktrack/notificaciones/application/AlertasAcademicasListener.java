package com.educktrack.notificaciones.application;

import com.educktrack.asistencia.domain.evento.EventosDeAsistencia.AsistenciaBajoMinimo;
import com.educktrack.estudiantes.domain.evento.EventosDeEstudiantes.EstudianteRetirado;
import com.educktrack.notas.domain.evento.EventosDeNotas.CorteCerrado;
import com.educktrack.notas.domain.evento.EventosDeNotas.NotaBajaRegistrada;
import com.educktrack.notificaciones.domain.TipoNotificacion;
import com.educktrack.seguridad.domain.evento.EventosDeSeguridad.PasswordRestablecida;
import com.educktrack.usuarios.domain.evento.EventosDeUsuarios.UsuarioDesactivado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;

/**
 * Convierte los hechos academicos en avisos concretos (RS-08).
 *
 * <p>Aqui vive toda la decision de <em>a quien</em> se avisa y <em>con que
 * palabras</em>. Los servicios de negocio solo publican lo que ocurrio, de modo
 * que cambiar el texto de una alerta, o dejar de enviarla, no obliga a tocar el
 * modulo de calificaciones ni el de asistencia.</p>
 *
 * <h2>Por que AFTER_COMMIT</h2>
 *
 * <p>Todos los listeners esperan a que la transaccion de negocio confirme. Un
 * correo no se puede deshacer: si la nota que provoco la alerta acaba en
 * rollback, el aviso de bajo rendimiento ya habria salido y no habria forma de
 * retirarlo. Avisar de algo que finalmente no ocurrio es peor que avisar tarde.</p>
 *
 * <p>La contrapartida es que un fallo aqui ya no puede deshacer la operacion de
 * negocio, que es exactamente lo que se quiere: que no se pueda calificar
 * porque el servidor de correo esta caido seria un acoplamiento absurdo. Por eso
 * cada listener protege su cuerpo y deja constancia en el log en vez de
 * propagar.</p>
 */
@Component
public class AlertasAcademicasListener {

    private static final Logger log = LoggerFactory.getLogger(AlertasAcademicasListener.class);

    private final NotificacionService notificaciones;
    private final DestinatariosService destinatarios;

    public AlertasAcademicasListener(NotificacionService notificaciones, DestinatariosService destinatarios) {
        this.notificaciones = notificaciones;
        this.destinatarios = destinatarios;
    }

    /** RB-13: aviso de bajo rendimiento al estudiante y a sus acudientes. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alRegistrarUnaNotaBaja(NotaBajaRegistrada evento) {
        enviar(destinatarios.delEstudianteYSusAcudientes(evento.estudianteId()),
                TipoNotificacion.BAJO_RENDIMIENTO,
                "Nota por debajo de lo aprobatorio",
                "Se registro una calificacion de " + evento.valor() + " en la materia "
                        + evento.materiaId() + ". Conviene reforzar antes del cierre del corte.",
                "bajo rendimiento del estudiante " + evento.estudianteId());
    }

    /** RF-30 / RB-04: aviso de riesgo de perder el derecho a evaluacion. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCaerBajoElMinimoDeAsistencia(AsistenciaBajoMinimo evento) {
        enviar(destinatarios.delEstudianteYSusAcudientes(evento.estudianteId()),
                TipoNotificacion.BAJA_ASISTENCIA,
                "Asistencia por debajo del minimo",
                "La asistencia en la materia " + evento.materiaId() + " es del "
                        + evento.porcentaje() + "%, por debajo del minimo exigido. "
                        + "Esto pone en riesgo el derecho a evaluacion.",
                "baja asistencia del estudiante " + evento.estudianteId());
    }

    /**
     * RF-55 y RF-56 / HU-20 y HU-21: el cierre del corte afecta a dos publicos
     * distintos y por motivos distintos, de ahi los dos avisos.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCerrarUnCorte(CorteCerrado evento) {
        // A los docentes: ya no pueden modificar notas directamente (RB-15).
        enviar(destinatarios.deLosDocentesDelCurso(evento.cursoId()),
                TipoNotificacion.CIERRE_PERIODO,
                "Corte cerrado",
                "El corte del curso " + evento.cursoId() + " quedo cerrado. Las notas ya no "
                        + "pueden modificarse directamente; las correcciones requieren una novedad.",
                "cierre de corte del curso " + evento.cursoId());

        // A estudiantes y acudientes: el boletin ya puede generarse (RF-56).
        enviar(destinatarios.delCursoCompleto(evento.cursoId()),
                TipoNotificacion.BOLETIN_DISPONIBLE,
                "Boletin disponible",
                "El corte quedo cerrado y el boletin de calificaciones ya puede consultarse.",
                "boletin disponible del curso " + evento.cursoId());
    }

    /** HU-07: el retiro se comunica al acudiente, que puede no haber estado presente. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alRetirarUnEstudiante(EstudianteRetirado evento) {
        enviar(destinatarios.deLosAcudientes(evento.estudianteId()),
                TipoNotificacion.GENERAL,
                "Retiro registrado",
                "Se registro el retiro de " + evento.nombreCompleto() + ". Motivo: " + evento.motivo() + ".",
                "retiro del estudiante " + evento.estudianteId());
    }

    /** HU-02: la persona afectada se entera de la baja, y no al intentar entrar. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alDesactivarUnaCuenta(UsuarioDesactivado evento) {
        enviar(Set.of(evento.usuarioId()),
                TipoNotificacion.GENERAL,
                "Cuenta desactivada",
                "La cuenta " + evento.correoInstitucional() + " fue desactivada y ya no permite "
                        + "iniciar sesion. El historial academico asociado se conserva intacto.",
                "desactivacion de la cuenta " + evento.usuarioId());
    }

    /**
     * HU-04: se avisa de que la contrasena se restablecio.
     *
     * <p>No es una cortesia: si quien lo recibe no pidio el cambio, este aviso
     * es la unica senal de que alguien mas tiene acceso a su correo, y llega a
     * tiempo de reaccionar.</p>
     *
     * <p>El aviso va <em>despues</em> del cambio y no lleva ningun secreto. El
     * enlace de recuperacion, que si lo es, nunca pasa por aqui: lo envia
     * {@code EnvioEnlaceRecuperacion} por correo directo, porque este metodo
     * deja copia en la bandeja interna y esa bandeja solo se lee entrando al
     * sistema.</p>
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alRestablecerLaPassword(PasswordRestablecida evento) {
        enviar(Set.of(evento.usuarioId()),
                TipoNotificacion.GENERAL,
                "Contrasena actualizada",
                "La contrasena de la cuenta " + evento.correoInstitucional() + " se restablecio "
                        + "mediante un enlace de recuperacion. Si no fuiste tu, avisa a coordinacion "
                        + "de inmediato.",
                "restablecimiento de contrasena de la cuenta " + evento.usuarioId());
    }

    /**
     * Envia el aviso a cada destinatario, sin dejar que un fallo del canal
     * arrastre a la operacion de negocio que ya confirmo.
     */
    private void enviar(Set<Long> usuarios, TipoNotificacion tipo, String titulo,
                        String mensaje, String contexto) {
        if (usuarios.isEmpty()) {
            // Normal cuando los perfiles aun no tienen cuenta asociada (V9).
            log.debug("Sin destinatarios con cuenta para el aviso de {}.", contexto);
            return;
        }
        for (Long usuarioId : usuarios) {
            try {
                notificaciones.notificar(usuarioId, titulo, mensaje, tipo);
            } catch (RuntimeException ex) {
                log.error("No se pudo notificar a {} sobre {}: {}", usuarioId, contexto, ex.getMessage());
            }
        }
    }
}
