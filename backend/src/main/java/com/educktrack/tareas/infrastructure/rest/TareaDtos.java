package com.educktrack.tareas.infrastructure.rest;

import com.educktrack.tareas.domain.EstadoEntrega;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTOs del modulo de tareas (RF-38..RF-42).
 */
public final class TareaDtos {

    private TareaDtos() {
    }

    /** RF-38 / HU-23: asignar tarea. */
    public record AsignarTareaRequest(
            @NotBlank(message = "El titulo es obligatorio") String titulo,
            String descripcion,
            @NotNull(message = "La materia es obligatoria") Long materiaId,
            @NotNull(message = "El curso es obligatorio") Long cursoId,
            @NotNull(message = "El periodo academico es obligatorio") Long periodoAcademicoId,
            Long docenteId,
            @NotNull(message = "La fecha limite es obligatoria") LocalDate fechaLimite,
            boolean permiteEntregaTardia) {
    }

    public record TareaDto(
            Long id, String titulo, String descripcion, Long materiaId, Long cursoId,
            Long periodoAcademicoId, Long docenteId, LocalDate fechaLimite, boolean permiteEntregaTardia) {
    }

    /** RF-39 / RB-10: entregar tarea. */
    public record EntregarTareaRequest(
            @NotNull(message = "El estudiante es obligatorio") Long estudianteId,
            @NotBlank(message = "La evidencia es obligatoria") String evidencia) {
    }

    /** RF-40: calificar tarea. */
    public record CalificarTareaRequest(
            @NotNull(message = "La calificacion es obligatoria") Double calificacion,
            String retroalimentacion) {
    }

    public record EntregaDto(
            Long id, Long tareaId, Long estudianteId, String evidencia,
            LocalDateTime fechaEntrega, Double calificacion, String retroalimentacion) {
    }

    /** RF-41: estado de una tarea para un estudiante. */
    public record EstadoTareaDto(Long tareaId, String titulo, LocalDate fechaLimite, EstadoEntrega estado) {
    }
}
