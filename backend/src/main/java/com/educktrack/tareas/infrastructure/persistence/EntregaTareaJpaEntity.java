package com.educktrack.tareas.infrastructure.persistence;

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
 * Entidad JPA de la entrega de una tarea por un estudiante (RF-39).
 * Unica por tarea/estudiante. La evidencia se guarda como texto/URL
 * (DECISION DE DISENO: la subida de archivos binarios queda fuera del alcance).
 */
@Entity
@Table(name = "entrega_tarea",
        uniqueConstraints = @UniqueConstraint(name = "uq_entrega_tarea_estudiante",
                columnNames = {"tarea_id", "estudiante_id"}))
@Getter
@Setter
@NoArgsConstructor
public class EntregaTareaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tarea_id", nullable = false)
    private Long tareaId;

    @Column(name = "estudiante_id", nullable = false)
    private Long estudianteId;

    @Column(name = "evidencia", length = 1000)
    private String evidencia;

    @Column(name = "fecha_entrega", nullable = false)
    private LocalDateTime fechaEntrega;

    @Column(name = "calificacion")
    private Double calificacion;

    @Column(name = "retroalimentacion", length = 1000)
    private String retroalimentacion;
}
