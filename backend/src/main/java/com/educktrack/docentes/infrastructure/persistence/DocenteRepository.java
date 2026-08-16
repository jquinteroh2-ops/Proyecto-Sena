package com.educktrack.docentes.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio Spring Data de docentes. Soporta la unicidad de documento (HU-08)
 * y la consulta de carga academica (RF-15).
 */
public interface DocenteRepository extends JpaRepository<DocenteJpaEntity, Long> {

    Optional<DocenteJpaEntity> findByDocumento(String documento);

    boolean existsByDocumento(String documento);

    /** Resuelve el docente asociado a una cuenta de usuario (V9, RS-03 / RNF-07). */
    Optional<DocenteJpaEntity> findByUsuarioId(Long usuarioId);

    /** Indica si la cuenta ya esta vinculada a un docente (1:1, V9). */
    boolean existsByUsuarioId(Long usuarioId);
}
