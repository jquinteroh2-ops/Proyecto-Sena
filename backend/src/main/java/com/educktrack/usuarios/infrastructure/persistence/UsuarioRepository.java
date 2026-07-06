package com.educktrack.usuarios.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio Spring Data de usuarios. Soporta la autenticacion (RF-60) y la
 * validacion de unicidad de correo institucional (HU-01).
 */
public interface UsuarioRepository extends JpaRepository<UsuarioJpaEntity, Long> {

    Optional<UsuarioJpaEntity> findByCorreoInstitucional(String correoInstitucional);

    boolean existsByCorreoInstitucional(String correoInstitucional);
}
