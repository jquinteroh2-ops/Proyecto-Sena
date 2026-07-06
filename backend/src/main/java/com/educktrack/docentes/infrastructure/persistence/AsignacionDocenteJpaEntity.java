package com.educktrack.docentes.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad JPA de asignacion academica: materia + curso a un docente en un
 * periodo (RF-14). Base para consultar la carga academica (RF-15) y validar el
 * area de formacion (RB-16) y los cruces de horario (RB-18) en fases siguientes.
 */
@Entity
@Table(name = "asignacion_docente")
@Getter
@Setter
@NoArgsConstructor
public class AsignacionDocenteJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "docente_id", nullable = false)
    private Long docenteId;

    @Column(name = "materia_id", nullable = false)
    private Long materiaId;

    @Column(name = "curso_id", nullable = false)
    private Long cursoId;

    @Column(name = "periodo_academico_id", nullable = false)
    private Long periodoAcademicoId;
}
