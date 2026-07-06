package com.educktrack.usuarios.infrastructure.persistence;

import com.educktrack.usuarios.domain.NombreRol;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data del catalogo de roles (RS-03).
 */
public interface RolRepository extends JpaRepository<RolJpaEntity, NombreRol> {
}
