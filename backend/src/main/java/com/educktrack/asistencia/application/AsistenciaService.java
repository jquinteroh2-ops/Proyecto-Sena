package com.educktrack.asistencia.application;

import com.educktrack.asistencia.domain.Asistencia;
import com.educktrack.asistencia.domain.EstadoAsistencia;
import com.educktrack.asistencia.infrastructure.persistence.AsistenciaJpaEntity;
import com.educktrack.asistencia.infrastructure.persistence.AsistenciaRepository;
import com.educktrack.asistencia.infrastructure.rest.AsistenciaDtos.AsistenciaDto;
import com.educktrack.asistencia.infrastructure.rest.AsistenciaDtos.EditarAsistenciaRequest;
import com.educktrack.asistencia.infrastructure.rest.AsistenciaDtos.EstudianteRiesgoDto;
import com.educktrack.asistencia.infrastructure.rest.AsistenciaDtos.ItemAsistencia;
import com.educktrack.asistencia.infrastructure.rest.AsistenciaDtos.RegistrarAsistenciaRequest;
import com.educktrack.asistencia.infrastructure.rest.AsistenciaDtos.ReporteAsistenciaDto;
import com.educktrack.asistencia.domain.evento.EventosDeAsistencia.AsistenciaBajoMinimo;
import com.educktrack.auditoria.application.AuditoriaService;
import com.educktrack.auditoria.domain.TipoOperacion;
import com.educktrack.identidad.application.ContextoUsuario;
import com.educktrack.shared.domain.ReglaNegocioException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Casos de uso de asistencia (RF-26..RF-30). Aplica RB-06 (registro unico por
 * bloque/fecha y ventana de edicion de 48h) y RB-04 (porcentaje minimo del 80%).
 */
@Service
public class AsistenciaService {

    /** RB-04: porcentaje minimo de asistencia para conservar derecho a evaluacion. */
    public static final double PORCENTAJE_MINIMO = 80.0;

    private final AsistenciaRepository asistenciaRepository;
    private final ContextoUsuario contexto;
    private final AuditoriaService auditoria;
    private final ApplicationEventPublisher eventos;

    public AsistenciaService(AsistenciaRepository asistenciaRepository, ContextoUsuario contexto,
                             AuditoriaService auditoria, ApplicationEventPublisher eventos) {
        this.asistenciaRepository = asistenciaRepository;
        this.contexto = contexto;
        this.auditoria = auditoria;
        this.eventos = eventos;
    }

    /**
     * RF-26 / HU-14 / RB-06: registra la asistencia del curso en un bloque y fecha.
     *
     * <p>RNF-07: solo el docente que dicta esa materia en ese curso puede pasar
     * lista de ella.</p>
     */
    @Transactional
    public List<AsistenciaDto> registrar(RegistrarAsistenciaRequest req) {
        contexto.exigirGestionMateria(req.cursoId(), req.materiaId());

        // RF-30: solo una ausencia injustificada puede empujar el porcentaje
        // por debajo del minimo; registrar PRESENTE o TARDE solo puede subirlo.
        // Basta con medir a esos estudiantes antes y despues para detectar el
        // cruce, en vez de a todo el curso.
        Map<Long, Double> porcentajePrevio = porcentajesDe(
                req.registros().stream()
                        .filter(i -> i.estado() == EstadoAsistencia.AUSENTE)
                        .map(ItemAsistencia::estudianteId)
                        .distinct()
                        .toList(),
                req.materiaId(), req.periodoAcademicoId());

        List<AsistenciaJpaEntity> guardados = new ArrayList<>();
        LocalDateTime ahora = LocalDateTime.now();
        for (ItemAsistencia item : req.registros()) {
            if (asistenciaRepository.existsByEstudianteIdAndBloqueIdAndFecha(
                    item.estudianteId(), req.bloqueId(), req.fecha())) {
                throw new ReglaNegocioException("RB-06",
                        "Ya existe asistencia registrada para el estudiante " + item.estudianteId()
                                + " en ese bloque y fecha.");
            }
            AsistenciaJpaEntity a = new AsistenciaJpaEntity();
            a.setEstudianteId(item.estudianteId());
            a.setCursoId(req.cursoId());
            a.setMateriaId(req.materiaId());
            a.setBloqueId(req.bloqueId());
            a.setPeriodoAcademicoId(req.periodoAcademicoId());
            a.setFecha(req.fecha());
            a.setEstado(item.estado());
            a.setJustificada(false);
            a.setFechaRegistro(ahora);
            guardados.add(asistenciaRepository.save(a));
        }

        publicarCrucesDelMinimo(porcentajePrevio, req.materiaId(), req.periodoAcademicoId());
        return guardados.stream().map(AsistenciaService::toDto).toList();
    }

    /**
     * RF-30 / RB-04: avisa solo de quien <strong>acaba de cruzar</strong> el
     * minimo hacia abajo. Avisar en cada registro de quien ya estaba por debajo
     * convertiria la alerta en ruido diario, y una alerta que se ignora no
     * cumple su funcion.
     */
    private void publicarCrucesDelMinimo(Map<Long, Double> porcentajePrevio,
                                         Long materiaId, Long periodoAcademicoId) {
        if (porcentajePrevio.isEmpty()) {
            return;
        }
        Map<Long, Double> ahora = porcentajesDe(
                List.copyOf(porcentajePrevio.keySet()), materiaId, periodoAcademicoId);

        porcentajePrevio.forEach((estudianteId, antes) -> {
            double despues = ahora.getOrDefault(estudianteId, 100.0);
            if (antes >= PORCENTAJE_MINIMO && despues < PORCENTAJE_MINIMO) {
                eventos.publishEvent(new AsistenciaBajoMinimo(
                        estudianteId, materiaId, periodoAcademicoId, despues));
            }
        });
    }

    private Map<Long, Double> porcentajesDe(List<Long> estudianteIds, Long materiaId, Long periodoAcademicoId) {
        Map<Long, Double> porcentajes = new LinkedHashMap<>();
        for (Long estudianteId : estudianteIds) {
            porcentajes.put(estudianteId, calcularPorcentaje(
                    asistenciaRepository.findByEstudianteIdAndMateriaIdAndPeriodoAcademicoId(
                            estudianteId, materiaId, periodoAcademicoId)));
        }
        return porcentajes;
    }

    /** RF-27 / HU-15 / RS-07: justifica una inasistencia (no afecta el % minimo, RB-04). */
    @Transactional
    public AsistenciaDto justificar(Long id, String motivo) {
        AsistenciaJpaEntity e = obtener(id);
        Asistencia dominio = toDominio(e);
        dominio.justificar(motivo);
        e.setJustificada(dominio.isJustificada());
        e.setMotivoJustificacion(dominio.getMotivoJustificacion());
        AsistenciaDto dto = toDto(asistenciaRepository.save(e));

        // HU-15: "la accion queda registrada en el log de auditoria". Justificar
        // altera el porcentaje que decide el derecho a evaluacion (RB-04), de
        // modo que debe poder rastrearse quien lo hizo.
        auditoria.registrar(TipoOperacion.ASISTENCIA_JUSTIFICADA, "asistencia", id,
                "Inasistencia del estudiante " + e.getEstudianteId() + " del " + e.getFecha()
                        + " justificada. Motivo: " + motivo + ".");
        return dto;
    }

    /** RF-29 / RB-06: edita el estado dentro de las 48h siguientes al registro. */
    @Transactional
    public AsistenciaDto editar(Long id, EditarAsistenciaRequest req) {
        AsistenciaJpaEntity e = obtener(id);
        contexto.exigirGestionMateria(e.getCursoId(), e.getMateriaId()); // RNF-07
        Asistencia dominio = toDominio(e);
        dominio.cambiarEstado(req.estado(), LocalDateTime.now());
        e.setEstado(dominio.getEstado());
        return toDto(asistenciaRepository.save(e));
    }

    /** RF-28 / RB-04 / RNF-07: reporte de asistencia de un estudiante en una materia y periodo. */
    @Transactional(readOnly = true)
    public ReporteAsistenciaDto reporteEstudiante(Long estudianteId, Long materiaId, Long periodoAcademicoId) {
        contexto.exigirAccesoEstudiante(estudianteId);
        List<AsistenciaJpaEntity> registros = asistenciaRepository
                .findByEstudianteIdAndMateriaIdAndPeriodoAcademicoId(estudianteId, materiaId, periodoAcademicoId);
        return construirReporte(estudianteId, materiaId, periodoAcademicoId, registros);
    }

    /** RF-30 / RB-04 / RNF-07: estudiantes de un curso/materia por debajo del minimo. */
    @Transactional(readOnly = true)
    public List<EstudianteRiesgoDto> estudiantesEnRiesgo(Long cursoId, Long materiaId, Long periodoAcademicoId) {
        contexto.exigirAccesoCurso(cursoId);
        Map<Long, List<AsistenciaJpaEntity>> porEstudiante = asistenciaRepository
                .findByCursoIdAndMateriaIdAndPeriodoAcademicoId(cursoId, materiaId, periodoAcademicoId)
                .stream().collect(Collectors.groupingBy(AsistenciaJpaEntity::getEstudianteId));

        List<EstudianteRiesgoDto> enRiesgo = new ArrayList<>();
        porEstudiante.forEach((estId, regs) -> {
            double porcentaje = calcularPorcentaje(regs);
            if (porcentaje < PORCENTAJE_MINIMO) {
                enRiesgo.add(new EstudianteRiesgoDto(estId, porcentaje));
            }
        });
        return enRiesgo;
    }

    /**
     * RB-04: indica si el estudiante conserva el derecho a evaluacion ordinaria
     * en una materia, <strong>sin control de acceso</strong>.
     *
     * <p>Mismo patron que {@code CalificacionService.calcularPromedio}: solo
     * debe invocarse desde un metodo que ya haya autorizado al solicitante sobre
     * ese estudiante. Existe para que el boletin (RF-35) pueda decir si la
     * materia perdio el derecho sin duplicar la formula del porcentaje, que es
     * lo unico que define RB-04 y debe vivir en un solo sitio.</p>
     *
     * <p>Es una consulta, no un bloqueo: el sistema informa de la perdida y no
     * impide registrar la nota. Una justificacion que llega tarde (RF-27)
     * recalcula el porcentaje hacia arriba, de modo que bloquear el registro
     * convertiria un dato reversible en una puerta cerrada.</p>
     */
    @Transactional(readOnly = true)
    public boolean conservaDerechoAEvaluacion(Long estudianteId, Long materiaId, Long periodoAcademicoId) {
        return calcularPorcentaje(asistenciaRepository
                .findByEstudianteIdAndMateriaIdAndPeriodoAcademicoId(estudianteId, materiaId, periodoAcademicoId))
                >= PORCENTAJE_MINIMO;
    }

    /**
     * RB-04: materias del periodo en las que el estudiante <strong>perdio</strong>
     * el derecho a evaluacion ordinaria, resueltas en una sola consulta y
     * <strong>sin control de acceso</strong> (mismas condiciones de uso que
     * {@link #conservaDerechoAEvaluacion}).
     *
     * <p>Existe para el boletin: preguntar materia por materia convertia RB-04
     * en una consulta por cada asignatura del plan. Solo devuelve las materias
     * con asistencia registrada, porque sin registros el porcentaje es 100% y no
     * hay derecho que perder.</p>
     */
    @Transactional(readOnly = true)
    public Set<Long> materiasSinDerechoAEvaluacion(Long estudianteId, Long periodoAcademicoId) {
        return asistenciaRepository
                .findByEstudianteIdAndPeriodoAcademicoId(estudianteId, periodoAcademicoId)
                .stream()
                .collect(Collectors.groupingBy(AsistenciaJpaEntity::getMateriaId))
                .entrySet().stream()
                .filter(e -> calcularPorcentaje(e.getValue()) < PORCENTAJE_MINIMO)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private ReporteAsistenciaDto construirReporte(Long estudianteId, Long materiaId, Long periodoId,
                                                  List<AsistenciaJpaEntity> registros) {
        long total = registros.size();
        long presentes = registros.stream().filter(r -> r.getEstado() == EstadoAsistencia.PRESENTE).count();
        long tardes = registros.stream().filter(r -> r.getEstado() == EstadoAsistencia.TARDE).count();
        long ausencias = registros.stream().filter(r -> r.getEstado() == EstadoAsistencia.AUSENTE).count();
        long ausenciasInjustificadas = registros.stream()
                .filter(r -> r.getEstado() == EstadoAsistencia.AUSENTE && !r.isJustificada()).count();
        double porcentaje = calcularPorcentaje(registros);
        return new ReporteAsistenciaDto(estudianteId, materiaId, periodoId, total, presentes, tardes,
                ausencias, ausenciasInjustificadas, porcentaje, porcentaje < PORCENTAJE_MINIMO);
    }

    /**
     * RB-04: el porcentaje de asistencia descuenta solo las ausencias
     * injustificadas (HU-15: las justificadas no afectan el minimo).
     */
    private double calcularPorcentaje(List<AsistenciaJpaEntity> registros) {
        if (registros.isEmpty()) {
            return 100.0;
        }
        long total = registros.size();
        long ausenciasInjustificadas = registros.stream()
                .filter(r -> r.getEstado() == EstadoAsistencia.AUSENTE && !r.isJustificada()).count();
        return Math.round((total - ausenciasInjustificadas) * 10000.0 / total) / 100.0;
    }

    private AsistenciaJpaEntity obtener(Long id) {
        return asistenciaRepository.findById(id)
                .orElseThrow(() -> new ReglaNegocioException("RF-29", "El registro de asistencia no existe."));
    }

    private static Asistencia toDominio(AsistenciaJpaEntity e) {
        return new Asistencia(e.getEstado(), e.isJustificada(), e.getMotivoJustificacion(), e.getFechaRegistro());
    }

    private static AsistenciaDto toDto(AsistenciaJpaEntity e) {
        return new AsistenciaDto(e.getId(), e.getEstudianteId(), e.getCursoId(), e.getMateriaId(),
                e.getBloqueId(), e.getFecha(), e.getEstado(), e.isJustificada(), e.getMotivoJustificacion());
    }
}
