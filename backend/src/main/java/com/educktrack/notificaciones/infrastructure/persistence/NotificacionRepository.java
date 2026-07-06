package com.educktrack.notificaciones.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio Spring Data de notificaciones (RF-54).
 */
public interface NotificacionRepository extends JpaRepository<NotificacionJpaEntity, Long> {

    /** RF-54 / HU-27: bandeja del usuario, de la mas reciente a la mas antigua. */
    List<NotificacionJpaEntity> findByDestinatarioUsuarioIdOrderByFechaCreacionDesc(Long destinatarioUsuarioId);

    long countByDestinatarioUsuarioIdAndLeidaFalse(Long destinatarioUsuarioId);
}
