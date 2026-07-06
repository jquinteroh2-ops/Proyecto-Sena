package com.educktrack.notas.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio Spring Data de novedades de nota (RF-36, RB-15).
 */
public interface NovedadNotaRepository extends JpaRepository<NovedadNotaJpaEntity, Long> {

    List<NovedadNotaJpaEntity> findByCalificacionId(Long calificacionId);
}
