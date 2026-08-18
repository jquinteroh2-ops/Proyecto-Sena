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
import com.educktrack.identidad.application.ContextoUsuario;
import com.educktrack.notas.application.CalificacionService;
import com.educktrack.configuracion.application.ParametrosService;
import com.educktrack.notas.domain.Calificacion;
import com.educktrack.notas.infrastructure.persistence.CalificacionJpaEntity;
import com.educktrack.notas.infrastructure.persistence.CalificacionRepository;
import com.educktrack.reportes.infrastructure.rest.ReporteDtos.AsistenciaInstitucionalDto;
import com.educktrack.reportes.infrastructure.rest.ReporteDtos.PanelIndicadoresDto;
import com.educktrack.reportes.infrastructure.rest.ReporteDtos.RendimientoCursoDto;
import com.educktrack.reportes.infrastructure.rest.ReporteDtos.RendimientoEstudianteDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final ContextoUsuario contexto;
    private final ParametrosService parametros;

    public ReporteService(CalificacionRepository calificacionRepository,
                          AsistenciaRepository asistenciaRepository,
                          MatriculaRepository matriculaRepository,
                          PlanEstudiosRepository planRepository,
                          EstudianteRepository estudianteRepository,
                          CalificacionService calificacionService,
                          ContextoUsuario contexto,
                          ParametrosService parametros) {
        this.calificacionRepository = calificacionRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.matriculaRepository = matriculaRepository;
        this.planRepository = planRepository;
        this.estudianteRepository = estudianteRepository;
        this.calificacionService = calificacionService;
        this.contexto = contexto;
        this.parametros = parametros;
    }

    /** RF-51 / HU-29: panel de indicadores institucionales. */
    @Transactional(readOnly = true)
    public PanelIndicadoresDto panelIndicadores() {
        List<CalificacionJpaEntity> notas = calificacionRepository.findAll();
        BigDecimal promedioGeneral = redondear(mediaDe(
                notas.stream().map(CalificacionJpaEntity::getValor).toList()));

        Map<Long, List<BigDecimal>> notasPorEstudiante = notas.stream().collect(Collectors.groupingBy(
                CalificacionJpaEntity::getEstudianteId,
                Collectors.mapping(CalificacionJpaEntity::getValor, Collectors.toList())));
        long enRiesgo = notasPorEstudiante.values().stream()
                .filter(valores -> !parametros.escalaCalificacion().esAprobatoria(mediaDe(valores))).count();

        return new PanelIndicadoresDto(promedioGeneral, asistenciaInstitucional().asistenciaPromedio(),
                enRiesgo, notasPorEstudiante.size());
    }

    /** RF-48: asistencia institucional consolidada (RB-04 solo cuenta injustificadas). */
    @Transactional(readOnly = true)
    public AsistenciaInstitucionalDto asistenciaInstitucional() {
        List<AsistenciaJpaEntity> registros = asistenciaRepository.findAll();
        long total = registros.size();
        long ausInjust = registros.stream()
                .filter(r -> r.getEstado() == EstadoAsistencia.AUSENTE && !r.isJustificada()).count();
        double promedio = total == 0 ? 100.0 : redondearPorcentaje((total - ausInjust) * 100.0 / total);
        return new AsistenciaInstitucionalDto(total, ausInjust, promedio);
    }

    /**
     * RF-47 / HU-30: rendimiento academico por curso y periodo.
     *
     * <p>RNF-07: autoriza el curso una sola vez y a partir de ahi calcula los
     * promedios sin volver a comprobar el acceso estudiante por estudiante.</p>
     */
    @Transactional(readOnly = true)
    public RendimientoCursoDto rendimientoCurso(Long cursoId, Long periodoAcademicoId) {
        contexto.exigirAccesoCurso(cursoId);
        List<Long> materias = planRepository.findByCursoId(cursoId).stream()
                .map(PlanEstudiosJpaEntity::getMateriaId).toList();

        List<RendimientoEstudianteDto> filas = matriculaRepository.findByCursoId(cursoId).stream()
                .filter(m -> m.getEstado() == EstadoMatriculaCurso.ACTIVA)
                .map(MatriculaJpaEntity::getEstudianteId)
                .distinct()
                .map(estId -> filaRendimiento(estId, materias, periodoAcademicoId))
                .toList();

        BigDecimal promedioCurso = redondear(mediaDe(
                filas.stream().map(RendimientoEstudianteDto::promedioGeneral).toList()));
        return new RendimientoCursoDto(cursoId, periodoAcademicoId, filas, promedioCurso);
    }

    private RendimientoEstudianteDto filaRendimiento(Long estudianteId, List<Long> materias, Long periodoId) {
        BigDecimal promedio = redondear(mediaDe(materias.stream()
                .map(mat -> calificacionService.calcularPromedio(estudianteId, mat, periodoId).promedio())
                .toList()));
        String nombre = estudianteRepository.findById(estudianteId)
                .map(e -> e.getNombres() + " " + e.getApellidos()).orElse("Estudiante " + estudianteId);
        return new RendimientoEstudianteDto(estudianteId, nombre, promedio,
                parametros.escalaCalificacion().esAprobatoria(promedio));
    }

    /**
     * El porcentaje de asistencia (RB-04) se queda en {@code double}: es un
     * porcentaje calculado, no un valor de la escala de notas, y ahi el
     * redondeo binario no decide ninguna regla.
     */
    private static double redondearPorcentaje(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static BigDecimal redondear(BigDecimal v) {
        return v.setScale(Calificacion.ESCALA, RoundingMode.HALF_UP);
    }

    /** Media de un conjunto de notas; cero si no hay ninguna. */
    private static BigDecimal mediaDe(List<BigDecimal> valores) {
        if (valores.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(valores.size()), 6, RoundingMode.HALF_UP);
    }
}
