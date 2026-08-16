package com.educktrack.notas.application;

import com.educktrack.auditoria.application.AuditoriaService;
import com.educktrack.auditoria.domain.TipoOperacion;
import com.educktrack.identidad.application.ContextoUsuario;
import com.educktrack.notas.domain.Calificacion;
import com.educktrack.notas.domain.TipoEvaluacion;
import com.educktrack.notas.infrastructure.persistence.CalificacionJpaEntity;
import com.educktrack.notas.infrastructure.persistence.CalificacionRepository;
import com.educktrack.notas.infrastructure.persistence.CierreCorteJpaEntity;
import com.educktrack.notas.infrastructure.persistence.CierreCorteRepository;
import com.educktrack.notas.infrastructure.persistence.NovedadNotaJpaEntity;
import com.educktrack.notas.infrastructure.persistence.NovedadNotaRepository;
import com.educktrack.notas.infrastructure.persistence.PonderacionEvaluacionJpaEntity;
import com.educktrack.notas.infrastructure.persistence.PonderacionRepository;
import com.educktrack.notas.infrastructure.rest.NotaDtos.BoletinDto;
import com.educktrack.notas.infrastructure.rest.NotaDtos.BoletinMateriaDto;
import com.educktrack.notas.infrastructure.rest.NotaDtos.CalificacionDto;
import com.educktrack.notas.infrastructure.rest.NotaDtos.DetalleTipoDto;
import com.educktrack.notas.infrastructure.rest.NotaDtos.PromedioDto;
import com.educktrack.notas.infrastructure.rest.NotaDtos.RegistrarCalificacionRequest;
import com.educktrack.notas.domain.evento.EventosDeNotas.CorteCerrado;
import com.educktrack.notas.domain.evento.EventosDeNotas.NotaBajaRegistrada;
import com.educktrack.shared.domain.ReglaNegocioException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Casos de uso de calificaciones (RF-31..RF-37). Concentra RB-03 (escala),
 * RB-07 (promedio ponderado), RB-15 (historico inmutable / novedad) y RB-19
 * (cierre de corte bloquea modificaciones y habilita el boletin).
 */
@Service
public class CalificacionService {

    private final CalificacionRepository calificacionRepository;
    private final PonderacionRepository ponderacionRepository;
    private final CierreCorteRepository cierreRepository;
    private final NovedadNotaRepository novedadRepository;
    private final ContextoUsuario contexto;
    private final AuditoriaService auditoria;
    private final ApplicationEventPublisher eventos;

    public CalificacionService(CalificacionRepository calificacionRepository,
                               PonderacionRepository ponderacionRepository,
                               CierreCorteRepository cierreRepository,
                               NovedadNotaRepository novedadRepository,
                               ContextoUsuario contexto,
                               AuditoriaService auditoria,
                               ApplicationEventPublisher eventos) {
        this.calificacionRepository = calificacionRepository;
        this.ponderacionRepository = ponderacionRepository;
        this.cierreRepository = cierreRepository;
        this.novedadRepository = novedadRepository;
        this.contexto = contexto;
        this.auditoria = auditoria;
        this.eventos = eventos;
    }

    /**
     * RF-31 / RB-03 / HU-18: registra una calificacion en la escala 1.0-5.0.
     *
     * <p>RNF-07: el docente solo califica las materias que tiene asignadas en
     * ese curso; ser director de grupo (RB-02) da visibilidad sobre el curso
     * pero no potestad para poner notas de materias ajenas.</p>
     */
    @Transactional
    public CalificacionDto registrar(RegistrarCalificacionRequest req) {
        contexto.exigirGestionMateria(req.cursoId(), req.materiaId());
        exigirCorteAbierto(req.cursoId(), req.periodoAcademicoId());
        Calificacion nota = new Calificacion(req.valor()); // valida RB-03
        CalificacionJpaEntity e = new CalificacionJpaEntity();
        e.setEstudianteId(req.estudianteId());
        e.setMateriaId(req.materiaId());
        e.setCursoId(req.cursoId());
        e.setPeriodoAcademicoId(req.periodoAcademicoId());
        e.setTipo(req.tipo());
        e.setValor(nota.getValor());
        e.setDescripcion(req.descripcion());
        e.setFechaRegistro(LocalDateTime.now());
        CalificacionJpaEntity guardada = calificacionRepository.save(e);

        // RS-07 nombra los cambios de nota como operacion critica.
        auditoria.registrar(TipoOperacion.NOTA_REGISTRADA, "calificacion", guardada.getId(),
                "Nota " + guardada.getValor() + " (" + guardada.getTipo() + ") al estudiante "
                        + guardada.getEstudianteId() + " en la materia " + guardada.getMateriaId()
                        + ", curso " + guardada.getCursoId() + ".");

        // RB-13: el umbral lo evalua este modulo, porque la regla es de
        // calificaciones; quien escucha decide a quien avisa y con que texto.
        if (guardada.getValor() < Calificacion.NOTA_APROBATORIA) {
            eventos.publishEvent(new NotaBajaRegistrada(
                    guardada.getEstudianteId(), guardada.getMateriaId(), guardada.getCursoId(),
                    guardada.getPeriodoAcademicoId(), guardada.getValor()));
        }
        return toDto(guardada);
    }

    /** RF-32 / RB-15: edita una nota solo si el corte no esta cerrado. */
    @Transactional
    public CalificacionDto editar(Long id, double nuevoValor) {
        CalificacionJpaEntity e = obtener(id);
        contexto.exigirGestionMateria(e.getCursoId(), e.getMateriaId()); // RNF-07
        if (cierreRepository.existsByCursoIdAndPeriodoAcademicoId(e.getCursoId(), e.getPeriodoAcademicoId())) {
            throw new ReglaNegocioException("RB-15",
                    "El corte esta cerrado; use una novedad de nota para corregir (RF-36).");
        }
        double valorAnterior = e.getValor();
        e.setValor(new Calificacion(nuevoValor).getValor()); // RB-03
        CalificacionDto dto = toDto(calificacionRepository.save(e));

        // RS-07: el log guarda el valor anterior, porque una nota editada sin
        // saber de que valor venia no es auditable.
        auditoria.registrar(TipoOperacion.NOTA_EDITADA, "calificacion", id,
                "Nota del estudiante " + e.getEstudianteId() + " en la materia " + e.getMateriaId()
                        + " cambiada de " + valorAnterior + " a " + e.getValor() + ".");
        return dto;
    }

    /** RF-33 / RB-07 / RNF-07: promedio ponderado del estudiante en una materia/periodo. */
    @Transactional(readOnly = true)
    public PromedioDto promedio(Long estudianteId, Long materiaId, Long periodoAcademicoId) {
        contexto.exigirAccesoEstudiante(estudianteId);
        return calcularPromedio(estudianteId, materiaId, periodoAcademicoId);
    }

    /**
     * RB-07: el calculo puro del promedio, <strong>sin control de acceso</strong>.
     * Solo debe invocarse desde un metodo que ya haya autorizado al solicitante
     * sobre ese estudiante o sobre el curso completo; existe para que los
     * reportes de grupo (RF-47) no repitan la comprobacion una vez por nota.
     */
    @Transactional(readOnly = true)
    public PromedioDto calcularPromedio(Long estudianteId, Long materiaId, Long periodoAcademicoId) {
        List<CalificacionJpaEntity> notas = calificacionRepository
                .findByEstudianteIdAndMateriaIdAndPeriodoAcademicoId(estudianteId, materiaId, periodoAcademicoId);
        List<PonderacionEvaluacionJpaEntity> ponderaciones = ponderacionRepository
                .findByMateriaIdAndPeriodoAcademicoId(materiaId, periodoAcademicoId);

        Map<TipoEvaluacion, Double> promedioPorTipo = notas.stream().collect(Collectors.groupingBy(
                CalificacionJpaEntity::getTipo, Collectors.averagingDouble(CalificacionJpaEntity::getValor)));

        List<DetalleTipoDto> detalle = new ArrayList<>();
        List<TipoEvaluacion> pendientes = new ArrayList<>();
        double promedio = 0.0;

        if (ponderaciones.isEmpty()) {
            // DECISION DE DISENO: sin ponderaciones configuradas se usa promedio simple.
            promedio = notas.isEmpty() ? 0.0
                    : notas.stream().mapToDouble(CalificacionJpaEntity::getValor).average().orElse(0.0);
        } else {
            for (PonderacionEvaluacionJpaEntity p : ponderaciones) {
                Double avg = promedioPorTipo.get(p.getTipo());
                if (avg == null) {
                    pendientes.add(p.getTipo());
                    detalle.add(new DetalleTipoDto(p.getTipo(), null, p.getPorcentaje(), 0.0));
                } else {
                    double aporte = avg * p.getPorcentaje() / 100.0;
                    promedio += aporte;
                    detalle.add(new DetalleTipoDto(p.getTipo(), redondear(avg), p.getPorcentaje(), redondear(aporte)));
                }
            }
        }
        promedio = redondear(promedio);
        return new PromedioDto(estudianteId, materiaId, periodoAcademicoId, promedio,
                promedio >= Calificacion.NOTA_APROBATORIA, pendientes, detalle);
    }

    /** RF-34 / RB-19: cierra el corte de un curso/periodo (bloquea modificaciones). */
    @Transactional
    public void cerrarCorte(Long cursoId, Long periodoAcademicoId) {
        if (cierreRepository.existsByCursoIdAndPeriodoAcademicoId(cursoId, periodoAcademicoId)) {
            throw new ReglaNegocioException("RB-19", "El corte de este curso ya esta cerrado.");
        }
        CierreCorteJpaEntity c = new CierreCorteJpaEntity();
        c.setCursoId(cursoId);
        c.setPeriodoAcademicoId(periodoAcademicoId);
        c.setFechaCierre(LocalDateTime.now());
        c.setCerradoPor(usuarioActual());
        cierreRepository.save(c);

        // HU-20: "el cierre queda registrado en el log de auditoria".
        auditoria.registrar(TipoOperacion.CORTE_CERRADO, "cierre_corte", c.getId(),
                "Cierre del corte del curso " + cursoId + " en el periodo " + periodoAcademicoId
                        + ". A partir de aqui las notas solo se corrigen por novedad (RB-15).");

        // RF-55 y RF-56: el cierre bloquea las notas para los docentes y a la
        // vez habilita el boletin para estudiantes y acudientes (RB-19).
        eventos.publishEvent(new CorteCerrado(cursoId, periodoAcademicoId));
    }

    /** RF-36 / RB-15: correccion auditada de una nota de un corte cerrado. */
    @Transactional
    public CalificacionDto registrarNovedad(Long calificacionId, double nuevoValor, String motivo) {
        CalificacionJpaEntity e = obtener(calificacionId);
        if (!cierreRepository.existsByCursoIdAndPeriodoAcademicoId(e.getCursoId(), e.getPeriodoAcademicoId())) {
            throw new ReglaNegocioException("RB-15",
                    "La novedad solo aplica a cortes cerrados; edite la nota directamente (RF-32).");
        }
        double valorAnterior = e.getValor();
        double valorNuevo = new Calificacion(nuevoValor).getValor(); // RB-03

        NovedadNotaJpaEntity n = new NovedadNotaJpaEntity();
        n.setCalificacionId(calificacionId);
        n.setValorAnterior(valorAnterior);
        n.setValorNuevo(valorNuevo);
        n.setMotivo(motivo);
        n.setUsuario(usuarioActual());
        n.setFecha(LocalDateTime.now());
        novedadRepository.save(n);

        e.setValor(valorNuevo);
        CalificacionDto dto = toDto(calificacionRepository.save(e));

        // HU-22: la correccion sobre un corte cerrado deja rastro en dos sitios,
        // 'novedad_nota' (el historico que ve el docente, RB-15) y el log de
        // auditoria (RS-07). Son publicos distintos, no una duplicacion.
        auditoria.registrar(TipoOperacion.NOTA_NOVEDAD, "calificacion", calificacionId,
                "Novedad sobre corte cerrado: nota del estudiante " + e.getEstudianteId()
                        + " corregida de " + valorAnterior + " a " + valorNuevo + ". Motivo: " + motivo + ".");
        return dto;
    }

    /** RF-35 / RB-19 / RD-11 / RNF-07: genera el boletin de un estudiante (corte cerrado). */
    @Transactional(readOnly = true)
    public BoletinDto boletin(Long estudianteId, Long cursoId, Long periodoAcademicoId) {
        contexto.exigirAccesoEstudiante(estudianteId);
        if (!cierreRepository.existsByCursoIdAndPeriodoAcademicoId(cursoId, periodoAcademicoId)) {
            throw new ReglaNegocioException("RB-19",
                    "El boletin solo puede generarse cuando el corte del curso esta cerrado.");
        }
        List<CalificacionJpaEntity> notas = calificacionRepository
                .findByEstudianteIdAndPeriodoAcademicoId(estudianteId, periodoAcademicoId);
        Map<Long, List<CalificacionJpaEntity>> porMateria = notas.stream()
                .collect(Collectors.groupingBy(CalificacionJpaEntity::getMateriaId));

        List<BoletinMateriaDto> materias = new ArrayList<>();
        for (Map.Entry<Long, List<CalificacionJpaEntity>> entry : porMateria.entrySet()) {
            // El acceso ya quedo autorizado al entrar al boletin (RNF-07).
            double prom = calcularPromedio(estudianteId, entry.getKey(), periodoAcademicoId).promedio();
            materias.add(new BoletinMateriaDto(entry.getKey(), prom, prom >= Calificacion.NOTA_APROBATORIA));
        }
        double promedioGeneral = redondear(materias.stream()
                .mapToDouble(BoletinMateriaDto::promedio).average().orElse(0.0));
        // RB-12: aprueba si todas las materias son aprobatorias.
        boolean aprobado = !materias.isEmpty() && materias.stream().allMatch(BoletinMateriaDto::aprobada);
        return new BoletinDto(estudianteId, cursoId, periodoAcademicoId, materias, promedioGeneral, aprobado);
    }

    /** RF-37 / RNF-07: historico completo de calificaciones de un estudiante. */
    @Transactional(readOnly = true)
    public List<CalificacionDto> historico(Long estudianteId) {
        contexto.exigirAccesoEstudiante(estudianteId);
        return calificacionRepository.findByEstudianteId(estudianteId).stream()
                .map(CalificacionService::toDto).toList();
    }

    private void exigirCorteAbierto(Long cursoId, Long periodoAcademicoId) {
        if (cierreRepository.existsByCursoIdAndPeriodoAcademicoId(cursoId, periodoAcademicoId)) {
            throw new ReglaNegocioException("RB-19",
                    "El corte del curso esta cerrado; no se pueden registrar notas.");
        }
    }

    private CalificacionJpaEntity obtener(Long id) {
        return calificacionRepository.findById(id)
                .orElseThrow(() -> new ReglaNegocioException("RF-32", "La calificacion no existe."));
    }

    private static String usuarioActual() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "sistema";
    }

    private static double redondear(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static CalificacionDto toDto(CalificacionJpaEntity e) {
        return new CalificacionDto(e.getId(), e.getEstudianteId(), e.getMateriaId(), e.getCursoId(),
                e.getPeriodoAcademicoId(), e.getTipo(), e.getValor(), e.getDescripcion());
    }
}
