package com.educktrack.notificaciones.infrastructure.persistence;

import com.educktrack.notificaciones.domain.TipoNotificacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entidad JPA de una notificacion interna dirigida a un usuario (RS-08, RF-54).
 */
@Entity
@Table(name = "notificacion")
@Getter
@Setter
@NoArgsConstructor
public class NotificacionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "destinatario_usuario_id", nullable = false)
    private Long destinatarioUsuarioId;

    @Column(name = "titulo", length = 150, nullable = false)
    private String titulo;

    @Column(name = "mensaje", length = 1000, nullable = false)
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 30, nullable = false)
    private TipoNotificacion tipo;

    @Column(name = "leida", nullable = false)
    private boolean leida = false;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;
}
