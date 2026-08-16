package com.educktrack.matriculas.infrastructure.persistence;

import com.educktrack.matriculas.domain.EstadoMatriculaCurso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

/**
 * Repositorio Spring Data de matriculas (RF-09, RF-45, RF-46).
 */
public interface MatriculaRepository extends JpaRepository<MatriculaJpaEntity, Long> {

    /** RB-01: verifica si el estudiante ya tiene una matricula activa en el periodo. */
    boolean existsByEstudianteIdAndPeriodoAcademicoIdAndEstado(
            Long estudianteId, Long periodoAcademicoId, EstadoMatriculaCurso estado);

    /** RB-17 / RF-46: numero de matriculas activas de un curso (para el cupo). */
    long countByCursoIdAndEstado(Long cursoId, EstadoMatriculaCurso estado);

    /** RF-45: matriculas (activas o no) de un curso. */
    List<MatriculaJpaEntity> findByCursoId(Long cursoId);

    /**
     * RNF-07: comprueba si el estudiante esta matriculado y activo en alguno de
     * los cursos indicados. Es la condicion que conecta el alcance del docente
     * (sus cursos) con el estudiante concreto que intenta consultar.
     */
    boolean existsByEstudianteIdAndCursoIdInAndEstado(
            Long estudianteId, Collection<Long> cursoIds, EstadoMatriculaCurso estado);

    /**
     * RNF-07 / RB-08: comprueba si alguno de los estudiantes indicados (el
     * propio, o los tutelados por un acudiente) cursa activamente el curso.
     */
    boolean existsByEstudianteIdInAndCursoIdAndEstado(
            Collection<Long> estudianteIds, Long cursoId, EstadoMatriculaCurso estado);

    /** RNF-07: cursos que cursan activamente el estudiante o sus tutelados (RB-08). */
    @Query("""
            select distinct m.cursoId from MatriculaJpaEntity m
            where m.estudianteId in :estudianteIds and m.estado = :estado
            """)
    List<Long> findCursoIdsByEstudianteIdInAndEstado(
            Collection<Long> estudianteIds, EstadoMatriculaCurso estado);

    /** RNF-07 / RF-45: estudiantes activos de un conjunto de cursos. */
    @Query("""
            select distinct m.estudianteId from MatriculaJpaEntity m
            where m.cursoId in :cursoIds and m.estado = :estado
            """)
    List<Long> findEstudianteIdsByCursoIdInAndEstado(
            Collection<Long> cursoIds, EstadoMatriculaCurso estado);
}
