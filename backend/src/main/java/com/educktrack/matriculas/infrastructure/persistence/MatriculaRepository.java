package com.educktrack.matriculas.infrastructure.persistence;

import com.educktrack.matriculas.domain.EstadoMatriculaCurso;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
