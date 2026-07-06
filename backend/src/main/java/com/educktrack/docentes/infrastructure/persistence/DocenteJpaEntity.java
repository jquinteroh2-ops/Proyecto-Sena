package com.educktrack.docentes.infrastructure.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Entidad JPA de docente (RF-12). El documento es unico (HU-08). Las areas de
 * formacion (RD-09) se guardan en una tabla de coleccion.
 */
@Entity
@Table(name = "docente")
@Getter
@Setter
@NoArgsConstructor
public class DocenteJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "documento", length = 30, nullable = false, unique = true)
    private String documento;

    @Column(name = "nombres", length = 100, nullable = false)
    private String nombres;

    @Column(name = "apellidos", length = 100, nullable = false)
    private String apellidos;

    @Column(name = "correo", length = 150)
    private String correo;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "docente_area", joinColumns = @JoinColumn(name = "docente_id"))
    @Column(name = "area", length = 80, nullable = false)
    private Set<String> areasFormacion = new HashSet<>();
}
