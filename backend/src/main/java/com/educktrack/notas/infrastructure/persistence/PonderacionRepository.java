package com.educktrack.notas.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/**
 * Repositorio Spring Data de ponderaciones de evaluacion (RF-20, RB-07).
 */
public interface PonderacionRepository extends JpaRepository<PonderacionEvaluacionJpaEntity, Long> {

    List<PonderacionEvaluacionJpaEntity> findByMateriaIdAndPeriodoAcademicoId(Long materiaId, Long periodoAcademicoId);

    /**
     * Ponderaciones de varias materias de un periodo en una sola consulta.
     *
     * <p>Existe para el boletin (RF-35), que recorre todas las materias del plan
     * y pedia las ponderaciones una materia por vez.</p>
     */
    List<PonderacionEvaluacionJpaEntity> findByPeriodoAcademicoIdAndMateriaIdIn(
            Long periodoAcademicoId, Collection<Long> materiaIds);

    void deleteByMateriaIdAndPeriodoAcademicoId(Long materiaId, Long periodoAcademicoId);
}
