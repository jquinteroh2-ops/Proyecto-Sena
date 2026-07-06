package com.educktrack.estudiantes.infrastructure.persistence;

import com.educktrack.estudiantes.domain.EstadoMatricula;
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
 * Entidad JPA de estudiante (RF-06). El documento de identidad es unico (HU-05).
 */
@Entity
@Table(name = "estudiante")
@Getter
@Setter
@NoArgsConstructor
public class EstudianteJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "documento", length = 30, nullable = false, unique = true)
    private String documento;

    @Column(name = "nombres", length = 100, nullable = false)
    private String nombres;

    @Column(name = "apellidos", length = 100, nullable = false)
    private String apellidos;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "direccion", length = 200)
    private String direccion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private EstadoMatricula estado;

    @Column(name = "acudiente_nombre", length = 120)
    private String acudienteNombre;

    @Column(name = "acudiente_telefono", length = 30)
    private String acudienteTelefono;

    @Column(name = "acudiente_parentesco", length = 40)
    private String acudienteParentesco;
}
