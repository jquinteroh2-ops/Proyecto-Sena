package com.educktrack.asistencia.infrastructure.persistence;

import com.educktrack.asistencia.domain.EstadoAsistencia;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad JPA de asistencia (RF-26). Unica por estudiante/bloque/fecha (RB-06).
 */
@Entity
@Table(name = "asistencia",
        uniqueConstraints = @UniqueConstraint(name = "uq_asistencia",
                columnNames = {"estudiante_id", "bloque_id", "fecha"}))
@Getter
@Setter
@NoArgsConstructor
public class AsistenciaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "estudiante_id", nullable = false)
    private Long estudianteId;

    @Column(name = "curso_id", nullable = false)
    private Long cursoId;

    @Column(name = "materia_id", nullable = false)
    private Long materiaId;

    @Column(name = "bloque_id", nullable = false)
    private Long bloqueId;

    @Column(name = "periodo_academico_id", nullable = false)
    private Long periodoAcademicoId;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 12, nullable = false)
    private EstadoAsistencia estado;

    @Column(name = "justificada", nullable = false)
    private boolean justificada = false;

    @Column(name = "motivo_justificacion", length = 300)
    private String motivoJustificacion;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;
}
