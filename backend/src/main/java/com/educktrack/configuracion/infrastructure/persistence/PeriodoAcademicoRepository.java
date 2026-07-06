package com.educktrack.configuracion.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio Spring Data de periodos academicos. Soporta la validacion de
 * unicidad de periodo activo por ano lectivo (RB-05).
 */
public interface PeriodoAcademicoRepository extends JpaRepository<PeriodoAcademicoJpaEntity, Long> {

    Optional<PeriodoAcademicoJpaEntity> findByActivoTrue();

    boolean existsByAnioLectivoAndActivoTrue(int anioLectivo);
}
