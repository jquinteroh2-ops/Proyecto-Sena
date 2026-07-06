package com.educktrack.notas.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio Spring Data de ponderaciones de evaluacion (RF-20, RB-07).
 */
public interface PonderacionRepository extends JpaRepository<PonderacionEvaluacionJpaEntity, Long> {

    List<PonderacionEvaluacionJpaEntity> findByMateriaIdAndPeriodoAcademicoId(Long materiaId, Long periodoAcademicoId);

    void deleteByMateriaIdAndPeriodoAcademicoId(Long materiaId, Long periodoAcademicoId);
}
