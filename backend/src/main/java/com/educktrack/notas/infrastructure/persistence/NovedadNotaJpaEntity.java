package com.educktrack.notas.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entidad JPA de novedad de nota (RF-36, RB-15): correccion auditada de una
 * calificacion de un corte cerrado. Conserva el valor original y el nuevo.
 */
@Entity
@Table(name = "novedad_nota")
@Getter
@Setter
@NoArgsConstructor
public class NovedadNotaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "calificacion_id", nullable = false)
    private Long calificacionId;

    @Column(name = "valor_anterior", nullable = false)
    private double valorAnterior;

    @Column(name = "valor_nuevo", nullable = false)
    private double valorNuevo;

    @Column(name = "motivo", length = 300, nullable = false)
    private String motivo;

    @Column(name = "usuario", length = 150, nullable = false)
    private String usuario;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;
}
