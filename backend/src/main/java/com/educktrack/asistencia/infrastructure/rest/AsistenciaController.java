package com.educktrack.asistencia.infrastructure.rest;

import com.educktrack.asistencia.application.AsistenciaService;
import com.educktrack.asistencia.infrastructure.rest.AsistenciaDtos.AsistenciaDto;
import com.educktrack.asistencia.infrastructure.rest.AsistenciaDtos.EditarAsistenciaRequest;
import com.educktrack.asistencia.infrastructure.rest.AsistenciaDtos.EstudianteRiesgoDto;
import com.educktrack.asistencia.infrastructure.rest.AsistenciaDtos.JustificarRequest;
import com.educktrack.asistencia.infrastructure.rest.AsistenciaDtos.RegistrarAsistenciaRequest;
import com.educktrack.asistencia.infrastructure.rest.AsistenciaDtos.ReporteAsistenciaDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Gestion de asistencia (RF-26..RF-30). El registro y edicion los realiza el
 * Docente; la justificacion es competencia del Coordinador (HU-15).
 */
@RestController
@RequestMapping("/api/asistencia")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Asistencia", description = "Registro, justificacion y reportes de asistencia (RF-26..RF-30)")
public class AsistenciaController {

    private static final String DOCENTE = "hasAnyRole('DOCENTE','COORDINADOR_ACADEMICO','ADMINISTRADOR')";
    private static final String COORDINADOR = "hasAnyRole('COORDINADOR_ACADEMICO','ADMINISTRADOR')";
    private static final String CONSULTA = "hasAnyRole('DOCENTE','COORDINADOR_ACADEMICO','RECTOR','ADMINISTRADOR')";

    private final AsistenciaService service;

    public AsistenciaController(AsistenciaService service) {
        this.service = service;
    }

    /** RF-26 / HU-14: registrar asistencia diaria del curso en un bloque. */
    @PostMapping
    @PreAuthorize(DOCENTE)
    @Operation(summary = "Registrar asistencia diaria (RF-26, RB-06)")
    public ResponseEntity<List<AsistenciaDto>> registrar(@Valid @RequestBody RegistrarAsistenciaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(req));
    }

    /** RF-27 / HU-15: justificar inasistencia. */
    @PostMapping("/{id}/justificar")
    @PreAuthorize(COORDINADOR)
    @Operation(summary = "Justificar inasistencia (RF-27)")
    public ResponseEntity<AsistenciaDto> justificar(@PathVariable Long id,
                                                    @Valid @RequestBody JustificarRequest req) {
        return ResponseEntity.ok(service.justificar(id, req.motivo()));
    }

    /** RF-29 / RB-06: editar registro dentro de las 48h. */
    @PutMapping("/{id}")
    @PreAuthorize(DOCENTE)
    @Operation(summary = "Editar registro de asistencia (RF-29, RB-06)")
    public ResponseEntity<AsistenciaDto> editar(@PathVariable Long id,
                                                @Valid @RequestBody EditarAsistenciaRequest req) {
        return ResponseEntity.ok(service.editar(id, req));
    }

    /** RF-28 / RB-04: reporte de asistencia de un estudiante en una materia. */
    @GetMapping("/reporte")
    @PreAuthorize(CONSULTA)
    @Operation(summary = "Reporte de asistencia por estudiante y materia (RF-28, RB-04)")
    public ResponseEntity<ReporteAsistenciaDto> reporte(@RequestParam Long estudianteId,
                                                        @RequestParam Long materiaId,
                                                        @RequestParam Long periodoAcademicoId) {
        return ResponseEntity.ok(service.reporteEstudiante(estudianteId, materiaId, periodoAcademicoId));
    }

    /** RF-30 / RB-04: estudiantes en riesgo por baja asistencia. */
    @GetMapping("/riesgo")
    @PreAuthorize(CONSULTA)
    @Operation(summary = "Estudiantes por debajo del minimo de asistencia (RF-30, RB-04)")
    public ResponseEntity<List<EstudianteRiesgoDto>> riesgo(@RequestParam Long cursoId,
                                                            @RequestParam Long materiaId,
                                                            @RequestParam Long periodoAcademicoId) {
        return ResponseEntity.ok(service.estudiantesEnRiesgo(cursoId, materiaId, periodoAcademicoId));
    }
}
