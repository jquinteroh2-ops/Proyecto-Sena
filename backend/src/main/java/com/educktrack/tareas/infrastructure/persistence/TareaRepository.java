package com.educktrack.tareas.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio Spring Data de tareas (RF-38, RF-42).
 */
public interface TareaRepository extends JpaRepository<TareaJpaEntity, Long> {

    List<TareaJpaEntity> findByCursoId(Long cursoId);

    /** RF-42: tareas proximas a vencer entre hoy y una fecha limite. */
    List<TareaJpaEntity> findByFechaLimiteBetween(LocalDate desde, LocalDate hasta);
}
