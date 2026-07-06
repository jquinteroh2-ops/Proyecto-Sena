package com.educktrack.notificaciones.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data de la configuracion de notificaciones (RF-52).
 */
public interface ConfiguracionNotificacionRepository
        extends JpaRepository<ConfiguracionNotificacionJpaEntity, Integer> {
}
