package com.educktrack.reportes.infrastructure.rest;

import java.util.List;

/**
 * DTOs del modulo de reportes (RF-47, RF-48, RF-51).
 */
public final class ReporteDtos {

    private ReporteDtos() {
    }

    /** RF-51: panel de indicadores institucionales. */
    public record PanelIndicadoresDto(
            double promedioGeneralInstitucional,
            double asistenciaPromedioInstitucional,
            long estudiantesEnRiesgo,
            long totalEstudiantesConNotas) {
    }

    /** RF-47: fila de rendimiento de un estudiante en un curso. */
    public record RendimientoEstudianteDto(
            Long estudianteId, String nombreCompleto, double promedioGeneral, boolean aprobado) {
    }

    public record RendimientoCursoDto(
            Long cursoId, Long periodoAcademicoId,
            List<RendimientoEstudianteDto> estudiantes, double promedioCurso) {
    }

    /** RF-48: asistencia institucional consolidada. */
    public record AsistenciaInstitucionalDto(
            long totalRegistros, long ausenciasInjustificadas, double asistenciaPromedio) {
    }
}
