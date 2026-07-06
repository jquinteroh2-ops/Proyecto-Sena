package com.educktrack.notas.infrastructure.persistence;

import com.educktrack.notas.domain.TipoEvaluacion;
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
 * Entidad JPA de una calificacion (RF-31). El valor esta en la escala RB-03.
 */
@Entity
@Table(name = "calificacion")
@Getter
@Setter
@NoArgsConstructor
public class CalificacionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "estudiante_id", nullable = false)
    private Long estudianteId;

    @Column(name = "materia_id", nullable = false)
    private Long materiaId;

    @Column(name = "curso_id", nullable = false)
    private Long cursoId;

    @Column(name = "periodo_academico_id", nullable = false)
    private Long periodoAcademicoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 20, nullable = false)
    private TipoEvaluacion tipo;

    @Column(name = "valor", nullable = false)
    private double valor;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;
}
