package com.educktrack.notas.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entidad JPA del cierre de corte academico de un curso en un periodo (RF-34,
 * RB-19). Una vez existe, las notas de ese curso/periodo quedan bloqueadas.
 */
@Entity
@Table(name = "cierre_corte",
        uniqueConstraints = @UniqueConstraint(name = "uq_cierre_curso_periodo",
                columnNames = {"curso_id", "periodo_academico_id"}))
@Getter
@Setter
@NoArgsConstructor
public class CierreCorteJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "curso_id", nullable = false)
    private Long cursoId;

    @Column(name = "periodo_academico_id", nullable = false)
    private Long periodoAcademicoId;

    @Column(name = "fecha_cierre", nullable = false)
    private LocalDateTime fechaCierre;

    @Column(name = "cerrado_por", length = 150, nullable = false)
    private String cerradoPor;
}
