package com.educktrack.horarios.infrastructure.persistence;

import com.educktrack.cursos.domain.Jornada;
import com.educktrack.horarios.domain.DiaSemana;
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

import java.time.LocalTime;

/**
 * Entidad JPA de bloque horario (RF-21). Unico por dia/hora/jornada (HU-11).
 */
@Entity
@Table(name = "bloque_horario",
        uniqueConstraints = @UniqueConstraint(name = "uq_bloque",
                columnNames = {"dia", "hora_inicio", "hora_fin", "jornada"}))
@Getter
@Setter
@NoArgsConstructor
public class BloqueHorarioJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia", length = 12, nullable = false)
    private DiaSemana dia;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "jornada", length = 20, nullable = false)
    private Jornada jornada;
}
