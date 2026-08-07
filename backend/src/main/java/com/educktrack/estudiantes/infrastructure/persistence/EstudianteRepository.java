package com.educktrack.estudiantes.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio Spring Data de estudiantes. Soporta la validacion de unicidad de
 * documento (HU-05) y las consultas de perfil (RF-08).
 */
public interface EstudianteRepository extends JpaRepository<EstudianteJpaEntity, Long> {

    Optional<EstudianteJpaEntity> findByDocumento(String documento);

    boolean existsByDocumento(String documento);

    /** Resuelve el estudiante asociado a una cuenta de usuario (V9, RS-03 / RNF-07). */
    Optional<EstudianteJpaEntity> findByUsuarioId(Long usuarioId);

    /** Indica si la cuenta ya esta vinculada a un estudiante (1:1, V9). */
    boolean existsByUsuarioId(Long usuarioId);
}
