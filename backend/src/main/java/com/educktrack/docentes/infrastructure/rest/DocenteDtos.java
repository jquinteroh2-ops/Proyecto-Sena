package com.educktrack.docentes.infrastructure.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Set;

/**
 * DTOs del modulo de docentes (RF-12, RF-13).
 */
public final class DocenteDtos {

    private DocenteDtos() {
    }

    /** RF-12 / HU-08: registro de docente con al menos un area de formacion. */
    public record RegistrarDocenteRequest(
            @NotBlank(message = "El documento es obligatorio") String documento,
            @NotBlank(message = "Los nombres son obligatorios") String nombres,
            @NotBlank(message = "Los apellidos son obligatorios") String apellidos,
            String correo,
            String telefono,
            @NotEmpty(message = "Debe indicar al menos un area de formacion") Set<String> areasFormacion) {
    }

    /** RF-13: actualizacion de datos del docente. */
    public record ActualizarDocenteRequest(
            @NotBlank(message = "Los nombres son obligatorios") String nombres,
            @NotBlank(message = "Los apellidos son obligatorios") String apellidos,
            String correo,
            String telefono,
            @NotEmpty(message = "Debe indicar al menos un area de formacion") Set<String> areasFormacion) {
    }

    public record DocenteDto(
            Long id,
            String documento,
            String nombres,
            String apellidos,
            String correo,
            String telefono,
            List<String> areasFormacion) {
    }
}
