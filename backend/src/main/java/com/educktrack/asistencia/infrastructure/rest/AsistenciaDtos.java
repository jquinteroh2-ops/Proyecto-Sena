package com.educktrack.asistencia.infrastructure.rest;

import com.educktrack.asistencia.domain.EstadoAsistencia;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * DTOs del modulo de asistencia (RF-26..RF-30).
 */
public final class AsistenciaDtos {

    private AsistenciaDtos() {
    }

    /** RF-26 / HU-14: registro diario de asistencia de un curso en un bloque. */
    public record RegistrarAsistenciaRequest(
            @NotNull(message = "El curso es obligatorio") Long cursoId,
            @NotNull(message = "La materia es obligatoria") Long materiaId,
            @NotNull(message = "El bloque es obligatorio") Long bloqueId,
            @NotNull(message = "El periodo academico es obligatorio") Long periodoAcademicoId,
            @NotNull(message = "La fecha es obligatoria") LocalDate fecha,
            @NotEmpty(message = "Debe registrar al menos un estudiante") @Valid List<ItemAsistencia> registros) {
    }

    public record ItemAsistencia(
            @NotNull(message = "El estudiante es obligatorio") Long estudianteId,
            @NotNull(message = "El estado es obligatorio") EstadoAsistencia estado) {
    }

    /** RF-27 / HU-15: justificar una inasistencia. */
    public record JustificarRequest(
            @jakarta.validation.constraints.NotBlank(message = "El motivo es obligatorio") String motivo) {
    }

    /** RF-29: editar el estado de un registro (dentro de 48h, RB-06). */
    public record EditarAsistenciaRequest(
            @NotNull(message = "El estado es obligatorio") EstadoAsistencia estado) {
    }

    public record AsistenciaDto(
            Long id, Long estudianteId, Long cursoId, Long materiaId, Long bloqueId,
            LocalDate fecha, EstadoAsistencia estado, boolean justificada, String motivoJustificacion) {
    }

    /** RF-28 / RB-04: reporte de asistencia de un estudiante en una materia. */
    public record ReporteAsistenciaDto(
            Long estudianteId, Long materiaId, Long periodoAcademicoId,
            long totalRegistros, long presentes, long tardes,
            long ausencias, long ausenciasInjustificadas,
            double porcentajeAsistencia, boolean enRiesgo) {
    }

    /** RF-30: estudiante por debajo del minimo de asistencia (RB-04). */
    public record EstudianteRiesgoDto(Long estudianteId, double porcentajeAsistencia) {
    }
}
