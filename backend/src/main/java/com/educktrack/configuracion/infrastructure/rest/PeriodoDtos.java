package com.educktrack.configuracion.infrastructure.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/**
 * DTOs de periodo academico (RF-57, RD-02). Version base de la Fase 3; el panel
 * completo de configuracion se implementa en la Fase 9.
 */
public final class PeriodoDtos {

    private PeriodoDtos() {
    }

    public record CrearPeriodoRequest(
            @NotBlank(message = "El nombre es obligatorio") String nombre,
            @Positive(message = "El ano lectivo debe ser positivo") int anioLectivo,
            @NotNull(message = "La fecha de inicio es obligatoria") LocalDate fechaInicio,
            @NotNull(message = "La fecha de fin es obligatoria") LocalDate fechaFin) {
    }

    public record PeriodoDto(
            Long id,
            String nombre,
            int anioLectivo,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            boolean activo,
            boolean cerrado) {
    }
}
