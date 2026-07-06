package com.educktrack.reportes.infrastructure.rest;

import com.educktrack.reportes.application.ReporteService;
import com.educktrack.reportes.infrastructure.export.ExportadorReporte;
import com.educktrack.reportes.infrastructure.rest.ReporteDtos.AsistenciaInstitucionalDto;
import com.educktrack.reportes.infrastructure.rest.ReporteDtos.PanelIndicadoresDto;
import com.educktrack.reportes.infrastructure.rest.ReporteDtos.RendimientoCursoDto;
import com.educktrack.reportes.infrastructure.rest.ReporteDtos.RendimientoEstudianteDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Reportes e indicadores institucionales (RF-47..RF-51). Consulta por
 * Coordinacion y Rectoria; exportacion en PDF (RF-49) y Excel (RF-50).
 */
@RestController
@RequestMapping("/api/reportes")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reportes", description = "Indicadores, rendimiento y exportacion PDF/Excel (RF-47..RF-51)")
public class ReporteController {

    private static final String CONSULTA = "hasAnyRole('COORDINADOR_ACADEMICO','RECTOR','ADMINISTRADOR')";

    private final ReporteService service;

    public ReporteController(ReporteService service) {
        this.service = service;
    }

    /** RF-51 / HU-29: panel de indicadores institucionales. */
    @GetMapping("/indicadores")
    @PreAuthorize(CONSULTA)
    @Operation(summary = "Panel de indicadores institucionales (RF-51)")
    public ResponseEntity<PanelIndicadoresDto> indicadores() {
        return ResponseEntity.ok(service.panelIndicadores());
    }

    /** RF-48: asistencia institucional consolidada. */
    @GetMapping("/asistencia-institucional")
    @PreAuthorize(CONSULTA)
    @Operation(summary = "Reporte de asistencia institucional (RF-48)")
    public ResponseEntity<AsistenciaInstitucionalDto> asistencia() {
        return ResponseEntity.ok(service.asistenciaInstitucional());
    }

    /** RF-47 / HU-30: rendimiento academico por curso (JSON). */
    @GetMapping("/rendimiento")
    @PreAuthorize(CONSULTA)
    @Operation(summary = "Reporte de rendimiento por curso (RF-47)")
    public ResponseEntity<RendimientoCursoDto> rendimiento(@RequestParam Long cursoId,
                                                           @RequestParam Long periodoAcademicoId) {
        return ResponseEntity.ok(service.rendimientoCurso(cursoId, periodoAcademicoId));
    }

    /** RF-47 + RF-49: rendimiento por curso exportado en PDF. */
    @GetMapping("/rendimiento/pdf")
    @PreAuthorize(CONSULTA)
    @Operation(summary = "Exportar rendimiento en PDF (RF-49)")
    public ResponseEntity<byte[]> rendimientoPdf(@RequestParam Long cursoId,
                                                 @RequestParam Long periodoAcademicoId) {
        RendimientoCursoDto rep = service.rendimientoCurso(cursoId, periodoAcademicoId);
        byte[] pdf = ExportadorReporte.aPdf(tituloRendimiento(rep), encabezados(), filas(rep));
        return archivo(pdf, MediaType.APPLICATION_PDF, "rendimiento_curso_" + cursoId + ".pdf");
    }

    /** RF-47 + RF-50: rendimiento por curso exportado en Excel. */
    @GetMapping("/rendimiento/excel")
    @PreAuthorize(CONSULTA)
    @Operation(summary = "Exportar rendimiento en Excel (RF-50)")
    public ResponseEntity<byte[]> rendimientoExcel(@RequestParam Long cursoId,
                                                   @RequestParam Long periodoAcademicoId) {
        RendimientoCursoDto rep = service.rendimientoCurso(cursoId, periodoAcademicoId);
        byte[] xlsx = ExportadorReporte.aExcel(tituloRendimiento(rep), encabezados(), filas(rep));
        return archivo(xlsx, MediaType.valueOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                "rendimiento_curso_" + cursoId + ".xlsx");
    }

    private static String tituloRendimiento(RendimientoCursoDto rep) {
        return "Rendimiento curso " + rep.cursoId() + " - periodo " + rep.periodoAcademicoId();
    }

    private static List<String> encabezados() {
        return List.of("Estudiante", "Nombre", "Promedio", "Estado");
    }

    private static List<List<String>> filas(RendimientoCursoDto rep) {
        return rep.estudiantes().stream()
                .map((RendimientoEstudianteDto e) -> List.of(
                        String.valueOf(e.estudianteId()),
                        e.nombreCompleto(),
                        String.valueOf(e.promedioGeneral()),
                        e.aprobado() ? "APROBADO" : "REPRUEBA"))
                .toList();
    }

    private ResponseEntity<byte[]> archivo(byte[] contenido, MediaType tipo, String nombre) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(tipo)
                .body(contenido);
    }
}
