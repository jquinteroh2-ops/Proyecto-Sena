package com.educktrack.cursos.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio Spring Data del plan de estudios (RF-19, RD-05).
 */
public interface PlanEstudiosRepository extends JpaRepository<PlanEstudiosJpaEntity, Long> {

    List<PlanEstudiosJpaEntity> findByCursoId(Long cursoId);

    boolean existsByCursoIdAndMateriaId(Long cursoId, Long materiaId);
}
