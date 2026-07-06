package com.educktrack.configuracion.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Entidad JPA de periodo academico (RF-57, RD-02).
 */
@Entity
@Table(name = "periodo_academico")
@Getter
@Setter
@NoArgsConstructor
public class PeriodoAcademicoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", length = 80, nullable = false)
    private String nombre;

    @Column(name = "anio_lectivo", nullable = false)
    private int anioLectivo;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "activo", nullable = false)
    private boolean activo = false;

    @Column(name = "cerrado", nullable = false)
    private boolean cerrado = false;
}
