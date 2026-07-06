package com.educktrack.notas.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data de cierres de corte (RF-34, RB-19).
 */
public interface CierreCorteRepository extends JpaRepository<CierreCorteJpaEntity, Long> {

    /** RB-19: indica si el corte de un curso/periodo esta cerrado. */
    boolean existsByCursoIdAndPeriodoAcademicoId(Long cursoId, Long periodoAcademicoId);
}
