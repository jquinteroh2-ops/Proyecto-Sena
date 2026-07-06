package com.educktrack.matriculas.infrastructure.persistence;

import com.educktrack.matriculas.domain.EstadoMatriculaCurso;
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

import java.time.LocalDate;

/**
 * Entidad JPA de matricula (RF-09). Relaciona estudiante, curso y periodo por id
 * (bajo acoplamiento). La unicidad de matricula activa por periodo (RB-01) se
 * valida en la capa de aplicacion y se refuerza en la migracion.
 */
@Entity
@Table(name = "matricula")
@Getter
@Setter
@NoArgsConstructor
public class MatriculaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "estudiante_id", nullable = false)
    private Long estudianteId;

    @Column(name = "curso_id", nullable = false)
    private Long cursoId;

    @Column(name = "periodo_academico_id", nullable = false)
    private Long periodoAcademicoId;

    @Column(name = "fecha_matricula", nullable = false)
    private LocalDate fechaMatricula;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private EstadoMatriculaCurso estado;
}
