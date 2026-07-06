package com.educktrack.usuarios.infrastructure.persistence;

import com.educktrack.usuarios.domain.NombreRol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad JPA del catalogo de roles (RD-07, RS-03). Se precarga en Flyway.
 */
@Entity
@Table(name = "rol")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RolJpaEntity {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "nombre", length = 40, nullable = false)
    private NombreRol nombre;

    @Column(name = "descripcion", length = 150)
    private String descripcion;
}
