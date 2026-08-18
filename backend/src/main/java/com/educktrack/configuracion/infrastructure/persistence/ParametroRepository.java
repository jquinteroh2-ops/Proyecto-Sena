package com.educktrack.configuracion.infrastructure.persistence;

import com.educktrack.configuracion.domain.ParametroInstitucional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data de parametros institucionales (RF-59).
 */
public interface ParametroRepository extends JpaRepository<ParametroJpaEntity, ParametroInstitucional> {
}
