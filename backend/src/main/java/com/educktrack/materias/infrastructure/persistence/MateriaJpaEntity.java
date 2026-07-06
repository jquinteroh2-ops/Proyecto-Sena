package com.educktrack.materias.infrastructure.persistence;

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
 * Entidad JPA de materia (RF-17). El codigo es unico.
 */
@Entity
@Table(name = "materia")
@Getter
@Setter
@NoArgsConstructor
public class MateriaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", length = 30, nullable = false, unique = true)
    private String codigo;

    @Column(name = "nombre", length = 120, nullable = false)
    private String nombre;

    @Column(name = "area", length = 80, nullable = false)
    private String area;

    @Column(name = "intensidad_horaria_semanal", nullable = false)
    private int intensidadHorariaSemanal;
}
