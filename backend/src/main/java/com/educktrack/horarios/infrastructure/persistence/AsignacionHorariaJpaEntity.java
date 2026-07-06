package com.educktrack.horarios.infrastructure.persistence;

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
 * Entidad JPA de asignacion horaria (RF-22): materia + docente + curso en un
 * bloque y periodo. La ausencia de cruces (RB-18) se valida en la aplicacion.
 */
@Entity
@Table(name = "asignacion_horaria")
@Getter
@Setter
@NoArgsConstructor
public class AsignacionHorariaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bloque_id", nullable = false)
    private Long bloqueId;

    @Column(name = "materia_id", nullable = false)
    private Long materiaId;

    @Column(name = "docente_id", nullable = false)
    private Long docenteId;

    @Column(name = "curso_id", nullable = false)
    private Long cursoId;

    @Column(name = "periodo_academico_id", nullable = false)
    private Long periodoAcademicoId;
}
