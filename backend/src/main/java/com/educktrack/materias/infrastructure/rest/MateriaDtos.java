package com.educktrack.materias.infrastructure.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * DTOs del modulo de materias (RF-17, RF-18).
 */
public final class MateriaDtos {

    private MateriaDtos() {
    }

    public record GuardarMateriaRequest(
            @NotBlank(message = "El codigo es obligatorio") String codigo,
            @NotBlank(message = "El nombre es obligatorio") String nombre,
            @NotBlank(message = "El area es obligatoria") String area,
            @Positive(message = "La intensidad horaria debe ser mayor que cero") int intensidadHorariaSemanal) {
    }

    public record MateriaDto(
            Long id,
            String codigo,
            String nombre,
            String area,
            int intensidadHorariaSemanal) {
    }
}
