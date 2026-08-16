package com.educktrack.identidad.infrastructure.persistence;

import com.educktrack.cursos.domain.Jornada;
import com.educktrack.cursos.domain.NivelEducativo;
import com.educktrack.cursos.infrastructure.persistence.CursoJpaEntity;
import com.educktrack.cursos.infrastructure.persistence.CursoRepository;
import com.educktrack.docentes.infrastructure.persistence.AsignacionDocenteJpaEntity;
import com.educktrack.docentes.infrastructure.persistence.AsignacionDocenteRepository;
import com.educktrack.matriculas.domain.EstadoMatriculaCurso;
import com.educktrack.matriculas.infrastructure.persistence.MatriculaJpaEntity;
import com.educktrack.matriculas.infrastructure.persistence.MatriculaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de las consultas que sostienen el alcance de datos de la Fase 2
 * (RNF-07, RB-02, RB-08).
 *
 * <p>{@link com.educktrack.identidad.application.ContextoUsuarioTest} prueba la
 * <em>logica</em> de la decision con dobles de prueba; esta clase prueba que las
 * consultas en las que se apoya existen de verdad y devuelven lo que la logica
 * asume. Sin ella un nombre de metodo derivado mal escrito solo se detectaria al
 * arrancar la aplicacion.</p>
 */
@DataJpaTest
@ActiveProfiles("test")
class ConsultasDeAlcanceTest {

    private static final Long DOCENTE = 55L;
    private static final Long OTRO_DOCENTE = 66L;
    private static final Long PERIODO = 1L;
    private static final Long MATEMATICAS = 7L;
    private static final Long SOCIALES = 8L;

    @Autowired private AsignacionDocenteRepository asignacionRepository;
    @Autowired private CursoRepository cursoRepository;
    @Autowired private MatriculaRepository matriculaRepository;

    private Long cursoPropio;
    private Long cursoDirigido;
    private Long cursoAjeno;

    @BeforeEach
    void prepararEscenario() {
        // El docente dicta matematicas en 7A, dirige 8B sin dictar alli, y no
        // tiene ningun vinculo con 11C.
        cursoPropio = guardarCurso("7A", 7, null);
        cursoDirigido = guardarCurso("8B", 8, DOCENTE);
        cursoAjeno = guardarCurso("11C", 11, OTRO_DOCENTE);

        guardarAsignacion(DOCENTE, MATEMATICAS, cursoPropio);
        guardarAsignacion(OTRO_DOCENTE, SOCIALES, cursoAjeno);

        matricular(10L, cursoPropio, EstadoMatriculaCurso.ACTIVA);
        matricular(11L, cursoDirigido, EstadoMatriculaCurso.ACTIVA);
        matricular(12L, cursoAjeno, EstadoMatriculaCurso.ACTIVA);
        matricular(13L, cursoPropio, EstadoMatriculaCurso.RETIRADA);
    }

    @Test
    void laCargaAcademicaDevuelveSoloLosCursosAsignados() {
        assertEquals(List.of(cursoPropio), asignacionRepository.findCursoIdsByDocenteId(DOCENTE));
    }

    @Test
    void laDireccionDeGrupoDevuelveLosCursosDirigidos() {
        // RB-02: 8B entra por direccion de grupo aunque no haya asignacion.
        assertEquals(List.of(cursoDirigido), cursoRepository.findIdsByDirectorGrupoId(DOCENTE));
    }

    @Test
    void laPotestadSobreUnaMateriaExigeLaAsignacionExacta() {
        assertTrue(asignacionRepository
                .existsByDocenteIdAndCursoIdAndMateriaId(DOCENTE, cursoPropio, MATEMATICAS));
        // Dicta en 7A, pero no sociales.
        assertFalse(asignacionRepository
                .existsByDocenteIdAndCursoIdAndMateriaId(DOCENTE, cursoPropio, SOCIALES));
        // Dirige 8B, pero no dicta ninguna materia alli.
        assertFalse(asignacionRepository
                .existsByDocenteIdAndCursoIdAndMateriaId(DOCENTE, cursoDirigido, MATEMATICAS));
    }

    @Test
    void elEstudianteEsAlcanzableSoloDesdeUnCursoDelDocente() {
        Set<Long> cursosDelDocente = Set.of(cursoPropio, cursoDirigido);

        assertTrue(matriculaRepository.existsByEstudianteIdAndCursoIdInAndEstado(
                10L, cursosDelDocente, EstadoMatriculaCurso.ACTIVA));
        assertFalse(matriculaRepository.existsByEstudianteIdAndCursoIdInAndEstado(
                12L, cursosDelDocente, EstadoMatriculaCurso.ACTIVA));
    }

    @Test
    void unaMatriculaRetiradaNoConcedeAlcance() {
        // El alcance se apoya en la matricula ACTIVA, no en el historico.
        assertFalse(matriculaRepository.existsByEstudianteIdAndCursoIdInAndEstado(
                13L, Set.of(cursoPropio), EstadoMatriculaCurso.ACTIVA));
    }

    @Test
    void enumeraLosEstudiantesActivosDeLosCursosDelDocente() {
        List<Long> visibles = matriculaRepository.findEstudianteIdsByCursoIdInAndEstado(
                Set.of(cursoPropio, cursoDirigido), EstadoMatriculaCurso.ACTIVA);

        assertEquals(Set.of(10L, 11L), Set.copyOf(visibles));
    }

    @Test
    void elEstudianteYSuAcudienteAlcanzanElCursoQueCursa() {
        assertTrue(matriculaRepository.existsByEstudianteIdInAndCursoIdAndEstado(
                List.of(10L), cursoPropio, EstadoMatriculaCurso.ACTIVA));
        assertFalse(matriculaRepository.existsByEstudianteIdInAndCursoIdAndEstado(
                List.of(10L), cursoAjeno, EstadoMatriculaCurso.ACTIVA));
    }

    @Test
    void devuelveLosCursosQueCursanElEstudianteYSusTutelados() {
        List<Long> cursos = matriculaRepository.findCursoIdsByEstudianteIdInAndEstado(
                List.of(10L, 12L), EstadoMatriculaCurso.ACTIVA);

        assertEquals(Set.of(cursoPropio, cursoAjeno), Set.copyOf(cursos));
    }

    // ---------------------------------------------------------------------

    private Long guardarCurso(String nombre, int grado, Long directorGrupoId) {
        CursoJpaEntity curso = new CursoJpaEntity();
        curso.setNombre(nombre);
        curso.setGrado(grado);
        curso.setNivel(NivelEducativo.BASICA_SECUNDARIA);
        curso.setJornada(Jornada.MANANA);
        curso.setCupoMaximo(30);
        curso.setPeriodoAcademicoId(PERIODO);
        curso.setDirectorGrupoId(directorGrupoId);
        return cursoRepository.save(curso).getId();
    }

    private void guardarAsignacion(Long docenteId, Long materiaId, Long cursoId) {
        AsignacionDocenteJpaEntity a = new AsignacionDocenteJpaEntity();
        a.setDocenteId(docenteId);
        a.setMateriaId(materiaId);
        a.setCursoId(cursoId);
        a.setPeriodoAcademicoId(PERIODO);
        asignacionRepository.save(a);
    }

    private void matricular(Long estudianteId, Long cursoId, EstadoMatriculaCurso estado) {
        MatriculaJpaEntity m = new MatriculaJpaEntity();
        m.setEstudianteId(estudianteId);
        m.setCursoId(cursoId);
        m.setPeriodoAcademicoId(PERIODO);
        m.setFechaMatricula(LocalDate.now());
        m.setEstado(estado);
        matriculaRepository.save(m);
    }
}
