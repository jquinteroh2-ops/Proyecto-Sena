package com.educktrack.reportes.application;

import com.educktrack.asistencia.domain.EstadoAsistencia;
import com.educktrack.asistencia.infrastructure.persistence.AsistenciaJpaEntity;
import com.educktrack.asistencia.infrastructure.persistence.AsistenciaRepository;
import com.educktrack.cursos.infrastructure.persistence.PlanEstudiosJpaEntity;
import com.educktrack.cursos.infrastructure.persistence.PlanEstudiosRepository;
import com.educktrack.estudiantes.infrastructure.persistence.EstudianteRepository;
import com.educktrack.matriculas.domain.EstadoMatriculaCurso;
import com.educktrack.matriculas.infrastructure.persistence.MatriculaJpaEntity;
import com.educktrack.matriculas.infrastructure.persistence.MatriculaRepository;
import com.educktrack.notas.application.CalificacionService;
import com.educktrack.notas.domain.Calificacion;
import com.educktrack.notas.infrastructure.persistence.CalificacionJpaEntity;
import com.educktrack.notas.infrastructure.persistence.CalificacionRepository;
import com.educktrack.reportes.infrastructure.rest.ReporteDtos.AsistenciaInstitucionalDto;
import com.educktrack.reportes.infrastructure.rest.ReporteDtos.PanelIndicadoresDto;
import com.educktrack.reportes.infrastructure.rest.ReporteDtos.RendimientoCursoDto;
import com.educktrack.reportes.infrastructure.rest.ReporteDtos.RendimientoEstudianteDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Casos de uso de reportes e indicadores (RF-47, RF-48, RF-51). Consolida datos
 * de calificaciones y asistencia para coordinacion y rectoria (RS-06).
 */
@Service
public class ReporteService {

    private final CalificacionRepository calificacionRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final MatriculaRepository matriculaRepository;
    private final PlanEstudiosRepository planRepository;
    private final EstudianteRepository estudianteRepository;
    private final CalificacionService calificacionService;

    public ReporteService(CalificacionRepository calificacionRepository,
                          AsistenciaRepository asistenciaRepository,
                          MatriculaRepository matriculaRepository,
                          PlanEstudiosRepository planRepository,
                          EstudianteRepository estudianteRepository,
                          CalificacionService calificacionService) {
        this.calificacionRepository = calificacionRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.matriculaRepository = matriculaRepository;
        this.planRepository = planRepository;
        this.estudianteRepository = estudianteRepository;
        this.calificacionService = calificacionService;
    }

    /** RF-51 / HU-29: panel de indicadores institucionales. */
    @Transactional(readOnly = true)
    public PanelIndicadoresDto panelIndicadores() {
        List<CalificacionJpaEntity> notas = calificacionRepository.findAll();
        double promedioGeneral = notas.isEmpty() ? 0.0
                : redondear(notas.stream().mapToDouble(CalificacionJpaEntity::getValor).average().orElse(0.0));

        Map<Long, Double> promedioPorEstudiante = notas.stream().collect(Collectors.groupingBy(
                CalificacionJpaEntity::getEstudianteId,
                Collectors.averagingDouble(CalificacionJpaEntity::getValor)));
        long enRiesgo = promedioPorEstudiante.values().stream()
                .filter(p -> p < Calificacion.NOTA_APROBATORIA).count();

        return new PanelIndicadoresDto(promedioGeneral, asistenciaInstitucional().asistenciaPromedio(),
                enRiesgo, promedioPorEstudiante.size());
    }

    /** RF-48: asistencia institucional consolidada (RB-04 solo cuenta injustificadas). */
    @Transactional(readOnly = true)
    public AsistenciaInstitucionalDto asistenciaInstitucional() {
        List<AsistenciaJpaEntity> registros = asistenciaRepository.findAll();
        long total = registros.size();
        long ausInjust = registros.stream()
                .filter(r -> r.getEstado() == EstadoAsistencia.AUSENTE && !r.isJustificada()).count();
        double promedio = total == 0 ? 100.0 : redondear((total - ausInjust) * 100.0 / total);
        return new AsistenciaInstitucionalDto(total, ausInjust, promedio);
    }

    /** RF-47 / HU-30: rendimiento academico por curso y periodo. */
    @Transactional(readOnly = true)
    public RendimientoCursoDto rendimientoCurso(Long cursoId, Long periodoAcademicoId) {
        List<Long> materias = planRepository.findByCursoId(cursoId).stream()
                .map(PlanEstudiosJpaEntity::getMateriaId).toList();

        List<RendimientoEstudianteDto> filas = matriculaRepository.findByCursoId(cursoId).stream()
                .filter(m -> m.getEstado() == EstadoMatriculaCurso.ACTIVA)
                .map(MatriculaJpaEntity::getEstudianteId)
                .distinct()
                .map(estId -> filaRendimiento(estId, materias, periodoAcademicoId))
                .toList();

        double promedioCurso = filas.isEmpty() ? 0.0
                : redondear(filas.stream().mapToDouble(RendimientoEstudianteDto::promedioGeneral).average().orElse(0.0));
        return new RendimientoCursoDto(cursoId, periodoAcademicoId, filas, promedioCurso);
    }

    private RendimientoEstudianteDto filaRendimiento(Long estudianteId, List<Long> materias, Long periodoId) {
        double promedio = materias.isEmpty() ? 0.0 : redondear(materias.stream()
                .mapToDouble(mat -> calificacionService.promedio(estudianteId, mat, periodoId).promedio())
                .average().orElse(0.0));
        String nombre = estudianteRepository.findById(estudianteId)
                .map(e -> e.getNombres() + " " + e.getApellidos()).orElse("Estudiante " + estudianteId);
        return new RendimientoEstudianteDto(estudianteId, nombre, promedio,
                promedio >= Calificacion.NOTA_APROBATORIA);
    }

    private static double redondear(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
