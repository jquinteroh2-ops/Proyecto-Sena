package com.educktrack.notificaciones.infrastructure.persistence;

import com.educktrack.notificaciones.domain.CanalNotificacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Configuracion institucional del canal de notificaciones (RF-52). Fila unica
 * (id fijo = 1) que aplica a toda la institucion (HU-28).
 */
@Entity
@Table(name = "configuracion_notificacion")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionNotificacionJpaEntity {

    @Id
    @Column(name = "id")
    private Integer id = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal", length = 12, nullable = false)
    private CanalNotificacion canal = CanalNotificacion.INTERNO;
}
