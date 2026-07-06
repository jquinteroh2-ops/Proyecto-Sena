package com.educktrack.cursos.infrastructure.persistence;

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

/**
 * Entidad JPA del plan de estudios: materias que componen un curso (RD-05).
 * Sustenta RB-11 (un estudiante matriculado cursa todas las materias del plan).
 */
@Entity
@Table(name = "plan_estudios",
        uniqueConstraints = @UniqueConstraint(name = "uq_plan_curso_materia",
                columnNames = {"curso_id", "materia_id"}))
@Getter
@Setter
@NoArgsConstructor
public class PlanEstudiosJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "curso_id", nullable = false)
    private Long cursoId;

    @Column(name = "materia_id", nullable = false)
    private Long materiaId;
}
