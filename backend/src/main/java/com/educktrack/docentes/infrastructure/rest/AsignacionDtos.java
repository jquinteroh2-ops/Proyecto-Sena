package com.educktrack.docentes.infrastructure.rest;

import jakarta.validation.constraints.NotNull;

/**
 * DTOs de asignacion academica de docentes (RF-14, RF-15, RF-16).
 */
public final class AsignacionDtos {

    private AsignacionDtos() {
    }

    /** RF-14: asignar materia y curso a un docente en un periodo. */
    public record AsignarMateriaRequest(
            @NotNull(message = "El docente es obligatorio") Long docenteId,
            @NotNull(message = "La materia es obligatoria") Long materiaId,
            @NotNull(message = "El curso es obligatorio") Long cursoId,
            @NotNull(message = "El periodo academico es obligatorio") Long periodoAcademicoId) {
    }

    public record AsignacionDto(
            Long id,
            Long docenteId,
            Long materiaId,
            Long cursoId,
            Long periodoAcademicoId) {
    }

    /** RF-15: carga academica de un docente en un periodo. */
    public record CargaAcademicaDto(
            Long docenteId,
            Long periodoAcademicoId,
            int totalMaterias,
            int totalHorasSemanales) {
    }

    /** RF-16: designacion de director de grupo. */
    public record DesignarDirectorRequest(
            @NotNull(message = "El docente es obligatorio") Long docenteId) {
    }
}
