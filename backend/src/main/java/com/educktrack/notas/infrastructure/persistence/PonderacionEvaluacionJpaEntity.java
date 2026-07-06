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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad JPA de la ponderacion de un tipo de evaluacion por materia y periodo
 * (RF-20, RB-07). El conjunto por materia/periodo debe sumar 100%.
 */
@Entity
@Table(name = "ponderacion_evaluacion",
        uniqueConstraints = @UniqueConstraint(name = "uq_ponderacion",
                columnNames = {"materia_id", "periodo_academico_id", "tipo"}))
@Getter
@Setter
@NoArgsConstructor
public class PonderacionEvaluacionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "materia_id", nullable = false)
    private Long materiaId;

    @Column(name = "periodo_academico_id", nullable = false)
    private Long periodoAcademicoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 20, nullable = false)
    private TipoEvaluacion tipo;

    @Column(name = "porcentaje", nullable = false)
    private int porcentaje;
}
