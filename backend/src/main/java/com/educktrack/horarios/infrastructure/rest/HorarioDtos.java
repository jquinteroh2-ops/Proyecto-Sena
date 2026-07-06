package com.educktrack.horarios.infrastructure.rest;

import com.educktrack.cursos.domain.Jornada;
import com.educktrack.horarios.domain.DiaSemana;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/**
 * DTOs del modulo de horarios (RF-21..RF-25).
 */
public final class HorarioDtos {

    private HorarioDtos() {
    }

    /** RF-21: crear bloque horario. */
    public record CrearBloqueRequest(
            @NotNull(message = "El dia es obligatorio") DiaSemana dia,
            @NotNull(message = "La hora de inicio es obligatoria") LocalTime horaInicio,
            @NotNull(message = "La hora de fin es obligatoria") LocalTime horaFin,
            @NotNull(message = "La jornada es obligatoria") Jornada jornada) {
    }

    public record BloqueDto(Long id, DiaSemana dia, LocalTime horaInicio, LocalTime horaFin, Jornada jornada) {
    }

    /** RF-22: asignar materia/docente/curso a un bloque. */
    public record AsignarHorarioRequest(
            @NotNull(message = "El bloque es obligatorio") Long bloqueId,
            @NotNull(message = "La materia es obligatoria") Long materiaId,
            @NotNull(message = "El docente es obligatorio") Long docenteId,
            @NotNull(message = "El curso es obligatorio") Long cursoId,
            @NotNull(message = "El periodo academico es obligatorio") Long periodoAcademicoId) {
    }

    /** Item de horario (RF-24, RF-25) con detalle del bloque. */
    public record HorarioItemDto(
            Long asignacionId,
            Long bloqueId,
            DiaSemana dia,
            LocalTime horaInicio,
            LocalTime horaFin,
            Jornada jornada,
            Long materiaId,
            Long docenteId,
            Long cursoId) {
    }
}
