package com.educktrack.notificaciones.application;

import com.educktrack.configuracion.infrastructure.persistence.PeriodoAcademicoJpaEntity;
import com.educktrack.configuracion.infrastructure.persistence.PeriodoAcademicoRepository;
import com.educktrack.notificaciones.domain.TipoNotificacion;
import com.educktrack.tareas.infrastructure.persistence.TareaJpaEntity;
import com.educktrack.tareas.infrastructure.persistence.TareaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

/**
 * Alertas que dependen del paso del tiempo y no de una accion concreta
 * (RF-42, RF-55).
 *
 * <p>Estas dos no encajan en el modelo de eventos del resto de la fase: nadie
 * "hace" que una tarea se acerque a su fecha limite. El disparador es el
 * calendario, de modo que van por planificador.</p>
 *
 * <p>Se ejecutan una vez al dia a primera hora. La ventana diaria tambien es lo
 * que evita duplicados: al comparar contra la fecha de hoy, cada tarea entra en
 * la franja de aviso un numero acotado de veces.</p>
 *
 * <p><strong>Ojo al escalar:</strong> con varias instancias del backend
 * (RNF-13) este planificador correria en todas y los avisos saldrian por
 * duplicado. Cuando eso ocurra hara falta un cerrojo compartido; hoy el
 * despliegue es de una sola instancia.</p>
 */
@Component
public class AlertasProgramadas {

    private static final Logger log = LoggerFactory.getLogger(AlertasProgramadas.class);

    private final TareaRepository tareaRepository;
    private final PeriodoAcademicoRepository periodoRepository;
    private final NotificacionService notificaciones;
    private final DestinatariosService destinatarios;
    private final int diasAvisoTarea;
    private final int diasAvisoCierre;

    public AlertasProgramadas(TareaRepository tareaRepository,
                              PeriodoAcademicoRepository periodoRepository,
                              NotificacionService notificaciones,
                              DestinatariosService destinatarios,
                              @Value("${educktrack.alertas.dias-aviso-tarea:3}") int diasAvisoTarea,
                              @Value("${educktrack.alertas.dias-aviso-cierre:7}") int diasAvisoCierre) {
        this.tareaRepository = tareaRepository;
        this.periodoRepository = periodoRepository;
        this.notificaciones = notificaciones;
        this.destinatarios = destinatarios;
        this.diasAvisoTarea = diasAvisoTarea;
        this.diasAvisoCierre = diasAvisoCierre;
    }

    /**
     * RF-42: avisa a estudiantes y acudientes de las tareas proximas a vencer.
     *
     * <p>El aviso va a los estudiantes activos del curso de la tarea, no a
     * quienes ya entregaron: recordar una entrega hecha es ruido. Esa distincion
     * se resuelve consultando el curso, porque el modelo no guarda a quien se
     * asigno la tarea individualmente.</p>
     */
    @Scheduled(cron = "${educktrack.alertas.cron:0 0 7 * * *}")
    @Transactional(readOnly = true)
    public void avisarTareasProximasAVencer() {
        LocalDate hoy = LocalDate.now();
        List<TareaJpaEntity> proximas = tareaRepository.findByFechaLimiteBetween(hoy, hoy.plusDays(diasAvisoTarea));

        for (TareaJpaEntity tarea : proximas) {
            long dias = ChronoUnit.DAYS.between(hoy, tarea.getFechaLimite());
            String plazo = dias <= 0 ? "hoy" : "en " + dias + " dia(s)";
            enviar(destinatarios.delCursoCompleto(tarea.getCursoId()),
                    TipoNotificacion.TAREA_POR_VENCER,
                    "Tarea proxima a vencer",
                    "La tarea \"" + tarea.getTitulo() + "\" vence " + plazo + " ("
                            + tarea.getFechaLimite() + ").",
                    "tarea " + tarea.getId());
        }
        log.debug("Revision de tareas proximas a vencer: {} tarea(s) en la ventana de aviso.", proximas.size());
    }

    /**
     * RF-55 / HU-20: avisa a los docentes de la fecha limite de cierre del
     * corte, tomando como plazo el fin del periodo academico activo.
     *
     * <p>El aviso se emite dentro de la ventana previa al cierre, no el mismo
     * dia: avisar de un plazo cuando ya se agoto no sirve de nada.</p>
     */
    @Scheduled(cron = "${educktrack.alertas.cron:0 0 7 * * *}")
    @Transactional(readOnly = true)
    public void avisarCierreDePeriodoProximo() {
        PeriodoAcademicoJpaEntity periodo = periodoRepository.findByActivoTrue().orElse(null);
        if (periodo == null || periodo.getFechaFin() == null || periodo.isCerrado()) {
            return;
        }
        long dias = ChronoUnit.DAYS.between(LocalDate.now(), periodo.getFechaFin());
        if (dias < 0 || dias > diasAvisoCierre) {
            return;
        }
        enviar(destinatarios.deLosDocentesDelPeriodo(periodo.getId()),
                TipoNotificacion.CIERRE_PERIODO,
                "Fecha limite de cierre del corte",
                "El periodo \"" + periodo.getNombre() + "\" termina el " + periodo.getFechaFin()
                        + (dias == 0 ? " (hoy)" : " (en " + dias + " dia(s))")
                        + ". Registre las notas pendientes antes del cierre.",
                "cierre del periodo " + periodo.getId());
    }

    /** Un fallo de canal no debe cortar el recorrido de los demas destinatarios. */
    private void enviar(Set<Long> usuarios, TipoNotificacion tipo, String titulo,
                        String mensaje, String contexto) {
        for (Long usuarioId : usuarios) {
            try {
                notificaciones.notificar(usuarioId, titulo, mensaje, tipo);
            } catch (RuntimeException ex) {
                log.error("No se pudo notificar a {} sobre {}: {}", usuarioId, contexto, ex.getMessage());
            }
        }
    }
}
