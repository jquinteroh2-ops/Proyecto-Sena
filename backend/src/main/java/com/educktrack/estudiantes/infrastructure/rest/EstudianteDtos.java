package com.educktrack.estudiantes.infrastructure.rest;

import com.educktrack.estudiantes.domain.EstadoMatricula;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/**
 * DTOs de entrada y salida del modulo de estudiantes (RF-06..RF-10).
 */
public final class EstudianteDtos {

    private EstudianteDtos() {
    }

    /** RF-06 / HU-05: registro de estudiante. */
    public record RegistrarEstudianteRequest(
            @NotBlank(message = "El documento es obligatorio") String documento,
            @NotBlank(message = "Los nombres son obligatorios") String nombres,
            @NotBlank(message = "Los apellidos son obligatorios") String apellidos,
            LocalDate fechaNacimiento,
            String direccion,
            String acudienteNombre,
            String acudienteTelefono,
            String acudienteParentesco) {
    }

    /** RF-07: actualizacion de datos personales. */
    public record ActualizarEstudianteRequest(
            @NotBlank(message = "Los nombres son obligatorios") String nombres,
            @NotBlank(message = "Los apellidos son obligatorios") String apellidos,
            LocalDate fechaNacimiento,
            String direccion,
            String acudienteNombre,
            String acudienteTelefono,
            String acudienteParentesco) {
    }

    /** RF-10 / RB-20: retiro con motivo y autorizacion. */
    public record RetirarEstudianteRequest(
            @NotBlank(message = "El motivo del retiro es obligatorio") String motivo,
            @NotBlank(message = "Debe indicar quien autoriza el retiro") String autorizadoPor) {
    }

    public record EstudianteDto(
            Long id,
            String documento,
            String nombres,
            String apellidos,
            LocalDate fechaNacimiento,
            String direccion,
            EstadoMatricula estado,
            String acudienteNombre,
            String acudienteTelefono,
            String acudienteParentesco) {
    }
}
