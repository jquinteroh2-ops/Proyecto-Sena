package com.educktrack.matriculas.infrastructure.rest;

import com.educktrack.matriculas.domain.EstadoMatriculaCurso;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * DTOs del modulo de matriculas (RF-09).
 */
public final class MatriculaDtos {

    private MatriculaDtos() {
    }

    /** RF-09 / HU-06: matricular estudiante en curso y periodo. */
    public record MatricularRequest(
            @NotNull(message = "El estudiante es obligatorio") Long estudianteId,
            @NotNull(message = "El curso es obligatorio") Long cursoId,
            @NotNull(message = "El periodo academico es obligatorio") Long periodoAcademicoId) {
    }

    public record MatriculaDto(
            Long id,
            Long estudianteId,
            Long cursoId,
            Long periodoAcademicoId,
            LocalDate fechaMatricula,
            EstadoMatriculaCurso estado,
            int materiasInscritas) {
    }
}
