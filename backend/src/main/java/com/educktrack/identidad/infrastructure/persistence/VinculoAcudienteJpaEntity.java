package com.educktrack.identidad.infrastructure.persistence;

import com.educktrack.identidad.domain.Parentesco;
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

import java.time.LocalDateTime;

/**
 * Entidad JPA del vinculo formal entre la cuenta de un acudiente y un
 * estudiante (RF-11, RD-08). Es la fuente de verdad de RB-08: un padre de
 * familia solo puede visualizar la informacion de los estudiantes que tenga
 * vinculados aqui.
 */
@Entity
@Table(name = "vinculo_acudiente",
        uniqueConstraints = @UniqueConstraint(name = "uq_vinculo_acudiente",
                columnNames = {"usuario_id", "estudiante_id"}))
@Getter
@Setter
@NoArgsConstructor
public class VinculoAcudienteJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Cuenta del acudiente (rol PADRE_FAMILIA). */
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "estudiante_id", nullable = false)
    private Long estudianteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "parentesco", length = 25, nullable = false)
    private Parentesco parentesco;

    @Column(name = "fecha_vinculo", nullable = false)
    private LocalDateTime fechaVinculo;
}
