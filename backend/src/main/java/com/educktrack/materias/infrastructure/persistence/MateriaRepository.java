package com.educktrack.materias.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio Spring Data de materias (RF-17, RF-18).
 */
public interface MateriaRepository extends JpaRepository<MateriaJpaEntity, Long> {

    Optional<MateriaJpaEntity> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);
}
