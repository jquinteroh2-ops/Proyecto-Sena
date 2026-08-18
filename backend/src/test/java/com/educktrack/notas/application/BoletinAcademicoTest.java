package com.educktrack.notas.application;

import com.educktrack.asistencia.application.AsistenciaService;
import com.educktrack.auditoria.application.AuditoriaService;
import com.educktrack.cursos.infrastructure.persistence.PlanEstudiosJpaEntity;
import com.educktrack.cursos.infrastructure.persistence.PlanEstudiosRepository;
import com.educktrack.identidad.application.ContextoUsuario;
import com.educktrack.notas.domain.TipoEvaluacion;
import com.educktrack.notas.infrastructure.persistence.CalificacionJpaEntity;
import com.educktrack.notas.infrastructure.persistence.CalificacionRepository;
import com.educktrack.notas.infrastructure.persistence.CierreCorteRepository;
import com.educktrack.notas.infrastructure.persistence.NovedadNotaRepository;
import com.educktrack.notas.infrastructure.persistence.PonderacionRepository;
import com.educktrack.notas.infrastructure.rest.NotaDtos.BoletinDto;
import com.educktrack.notas.infrastructure.rest.NotaDtos.BoletinMateriaDto;
import com.educktrack.shared.domain.ReglaNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Pruebas de RB-12 (aprobacion del periodo) sobre el boletin (RF-35).
 *
 * <p>RB-12 dice "promedio igual o superior a 3.0 en <em>todas las materias del
 * plan</em>". El boletin se armaba agrupando las calificaciones existentes, de
 * modo que una materia del plan sin ninguna nota simplemente no aparecia y no
 * podia impedir la aprobacion: el estudiante salia aprobado por las materias que
 * alguien alcanzo a calificarle. Estas pruebas fijan que el plan de estudios
 * (RB-11) es lo que determina que materias entran.</p>
 */
@ExtendWith(MockitoExtension.class)
class BoletinAcademicoTest {

    private static final Long ESTUDIANTE = 10L;
    private static final Long CURSO = 3L;
    private static final Long PERIODO = 1L;
    private static final Long MATEMATICAS = 7L;
    private static final Long SOCIALES = 8L;

    @Mock private CalificacionRepository calificacionRepository;
    @Mock private PonderacionRepository ponderacionRepository;
    @Mock private CierreCorteRepository cierreRepository;
    @Mock private NovedadNotaRepository novedadRepository;
    @Mock private PlanEstudiosRepository planEstudiosRepository;
    @Mock private AsistenciaService asistencia;
    @Mock private ContextoUsuario contexto;
    @Mock private AuditoriaService auditoria;
    @Mock private ApplicationEventPublisher eventos;

    @InjectMocks private CalificacionService service;

    @BeforeEach
    void corteCerrado() {
        lenient().when(cierreRepository.existsByCursoIdAndPeriodoAcademicoId(CURSO, PERIODO)).thenReturn(true);
        // Sin ponderaciones configuradas el promedio de la materia es simple.
        lenient().when(ponderacionRepository.findByMateriaIdAndPeriodoAcademicoId(any(), any()))
                .thenReturn(List.of());
        // Salvo que la prueba diga lo contrario, la asistencia esta en regla.
        lenient().when(asistencia.conservaDerechoAEvaluacion(any(), any(), any())).thenReturn(true);
    }

    @Test
    void noApruebaSiUnaMateriaDelPlanNoTieneNingunaNota() {
        planDelCurso(MATEMATICAS, SOCIALES);
        notasDelPeriodo(nota(MATEMATICAS, 4.5));
        notasDeLaMateria(MATEMATICAS, nota(MATEMATICAS, 4.5));

        BoletinDto boletin = service.boletin(ESTUDIANTE, CURSO, PERIODO);

        // Las dos materias del plan estan en el boletin, no solo la calificada.
        assertEquals(2, boletin.materias().size());
        BoletinMateriaDto sociales = materia(boletin, SOCIALES);
        assertTrue(sociales.sinCalificar());
        assertFalse(sociales.aprobada());
        // RB-12: no se puede aprobar el periodo con una materia del plan sin evaluar.
        assertFalse(boletin.aprobado());
    }

    @Test
    void apruebaCuandoTodasLasMateriasDelPlanEstanAprobadas() {
        planDelCurso(MATEMATICAS, SOCIALES);
        notasDelPeriodo(nota(MATEMATICAS, 4.0), nota(SOCIALES, 3.5));
        notasDeLaMateria(MATEMATICAS, nota(MATEMATICAS, 4.0));
        notasDeLaMateria(SOCIALES, nota(SOCIALES, 3.5));

        BoletinDto boletin = service.boletin(ESTUDIANTE, CURSO, PERIODO);

        assertTrue(boletin.aprobado());
        assertEquals(3.75, boletin.promedioGeneral());
        assertTrue(boletin.materias().stream().noneMatch(BoletinMateriaDto::sinCalificar));
    }

    @Test
    void noApruebaConUnaMateriaDelPlanPorDebajoDeLaNotaMinima() {
        planDelCurso(MATEMATICAS, SOCIALES);
        notasDelPeriodo(nota(MATEMATICAS, 4.8), nota(SOCIALES, 2.9));
        notasDeLaMateria(MATEMATICAS, nota(MATEMATICAS, 4.8));
        notasDeLaMateria(SOCIALES, nota(SOCIALES, 2.9));

        BoletinDto boletin = service.boletin(ESTUDIANTE, CURSO, PERIODO);

        assertFalse(boletin.aprobado());
        assertFalse(materia(boletin, SOCIALES).aprobada());
    }

    @Test
    void conservaLasNotasDeMateriasQueYaNoFiguranEnElPlan() {
        // El plan puede cambiar a mitad de periodo. Ocultar esas notas haria
        // desaparecer del boletin calificaciones que el estudiante si tiene.
        planDelCurso(MATEMATICAS);
        notasDelPeriodo(nota(MATEMATICAS, 4.0), nota(SOCIALES, 3.2));
        notasDeLaMateria(MATEMATICAS, nota(MATEMATICAS, 4.0));
        notasDeLaMateria(SOCIALES, nota(SOCIALES, 3.2));

        BoletinDto boletin = service.boletin(ESTUDIANTE, CURSO, PERIODO);

        assertEquals(2, boletin.materias().size());
        assertTrue(materia(boletin, SOCIALES).aprobada());
    }

    @Test
    void senalaLaPerdidaDelDerechoAEvaluacionSinConvertirlaEnReprobacion() {
        // RB-04: la decision se toma en el modulo de asistencia; el boletin solo
        // la traslada. Se marca la materia, pero una nota aprobatoria sigue
        // siendo aprobatoria: RB-04 y RB-12 responden preguntas distintas y
        // mezclarlas impediria distinguir quien perdio la materia de quien
        // perdio la asistencia.
        planDelCurso(MATEMATICAS);
        notasDelPeriodo(nota(MATEMATICAS, 4.2));
        notasDeLaMateria(MATEMATICAS, nota(MATEMATICAS, 4.2));
        when(asistencia.conservaDerechoAEvaluacion(ESTUDIANTE, MATEMATICAS, PERIODO)).thenReturn(false);

        BoletinDto boletin = service.boletin(ESTUDIANTE, CURSO, PERIODO);

        BoletinMateriaDto matematicas = materia(boletin, MATEMATICAS);
        assertTrue(matematicas.pierdeDerechoAEvaluacion());
        assertTrue(matematicas.aprobada());
        assertTrue(boletin.aprobado());
    }

    @Test
    void exigeElCierreDelCorteAntesDeGenerarlo() {
        // RB-19 sigue siendo la primera condicion del boletin.
        when(cierreRepository.existsByCursoIdAndPeriodoAcademicoId(CURSO, PERIODO)).thenReturn(false);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.boletin(ESTUDIANTE, CURSO, PERIODO));

        assertEquals("RB-19", error.getCodigoRegla());
    }

    // ---------- utilidades ----------

    private void planDelCurso(Long... materiaIds) {
        List<PlanEstudiosJpaEntity> plan = java.util.Arrays.stream(materiaIds).map(materiaId -> {
            PlanEstudiosJpaEntity e = new PlanEstudiosJpaEntity();
            e.setCursoId(CURSO);
            e.setMateriaId(materiaId);
            return e;
        }).toList();
        when(planEstudiosRepository.findByCursoId(CURSO)).thenReturn(plan);
    }

    private void notasDelPeriodo(CalificacionJpaEntity... notas) {
        when(calificacionRepository.findByEstudianteIdAndPeriodoAcademicoId(ESTUDIANTE, PERIODO))
                .thenReturn(List.of(notas));
    }

    private void notasDeLaMateria(Long materiaId, CalificacionJpaEntity... notas) {
        lenient().when(calificacionRepository.findByEstudianteIdAndMateriaIdAndPeriodoAcademicoId(
                ESTUDIANTE, materiaId, PERIODO)).thenReturn(List.of(notas));
    }

    private static CalificacionJpaEntity nota(Long materiaId, double valor) {
        CalificacionJpaEntity e = new CalificacionJpaEntity();
        e.setEstudianteId(ESTUDIANTE);
        e.setMateriaId(materiaId);
        e.setCursoId(CURSO);
        e.setPeriodoAcademicoId(PERIODO);
        e.setTipo(TipoEvaluacion.EXAMEN);
        e.setValor(valor);
        return e;
    }

    private static BoletinMateriaDto materia(BoletinDto boletin, Long materiaId) {
        return boletin.materias().stream().filter(m -> m.materiaId().equals(materiaId)).findFirst()
                .orElseThrow(() -> new AssertionError("La materia " + materiaId + " no esta en el boletin."));
    }
}
