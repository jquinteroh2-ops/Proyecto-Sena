package com.educktrack.notas.infrastructure.rest;

import com.educktrack.notas.application.CalificacionService;
import com.educktrack.notas.infrastructure.rest.NotaDtos.BoletinDto;
import com.educktrack.notas.infrastructure.rest.NotaDtos.CalificacionDto;
import com.educktrack.notas.infrastructure.rest.NotaDtos.EditarCalificacionRequest;
import com.educktrack.notas.infrastructure.rest.NotaDtos.NovedadRequest;
import com.educktrack.notas.infrastructure.rest.NotaDtos.PromedioDto;
import com.educktrack.notas.infrastructure.rest.NotaDtos.RegistrarCalificacionRequest;
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
 * Gestion de calificaciones (RF-31..RF-37). El registro/edicion los realiza el
 * Docente; el cierre de corte, novedades y boletin, el Coordinador.
 */
@RestController
@RequestMapping("/api/calificaciones")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Calificaciones", description = "Notas, promedios, cierre de corte y boletin (RF-31..RF-37)")
public class CalificacionController {

    private static final String DOCENTE = "hasAnyRole('DOCENTE','COORDINADOR_ACADEMICO','ADMINISTRADOR')";
    private static final String COORDINADOR = "hasAnyRole('COORDINADOR_ACADEMICO','ADMINISTRADOR')";
    private static final String CONSULTA = "hasAnyRole('DOCENTE','COORDINADOR_ACADEMICO','RECTOR','ADMINISTRADOR')";

    private final CalificacionService service;

    public CalificacionController(CalificacionService service) {
        this.service = service;
    }

    /** RF-31 / RB-03: registrar calificacion. */
    @PostMapping
    @PreAuthorize(DOCENTE)
    @Operation(summary = "Registrar calificacion (RF-31, RB-03)")
    public ResponseEntity<CalificacionDto> registrar(@Valid @RequestBody RegistrarCalificacionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(req));
    }

    /** RF-32 / RB-15: editar calificacion (corte abierto). */
    @PutMapping("/{id}")
    @PreAuthorize(DOCENTE)
    @Operation(summary = "Editar calificacion (RF-32, RB-15)")
    public ResponseEntity<CalificacionDto> editar(@PathVariable Long id,
                                                  @Valid @RequestBody EditarCalificacionRequest req) {
        return ResponseEntity.ok(service.editar(id, req.valor()));
    }

    /** RF-33 / RB-07: promedio ponderado por materia. */
    @GetMapping("/promedio")
    @PreAuthorize(CONSULTA)
    @Operation(summary = "Calcular promedio por materia (RF-33, RB-07)")
    public ResponseEntity<PromedioDto> promedio(@RequestParam Long estudianteId,
                                                @RequestParam Long materiaId,
                                                @RequestParam Long periodoAcademicoId) {
        return ResponseEntity.ok(service.promedio(estudianteId, materiaId, periodoAcademicoId));
    }

    /** RF-34 / RB-19: cerrar corte academico de un curso/periodo. */
    @PostMapping("/cerrar-corte")
    @PreAuthorize(COORDINADOR)
    @Operation(summary = "Cerrar corte academico (RF-34, RB-19)")
    public ResponseEntity<Void> cerrarCorte(@RequestParam Long cursoId,
                                            @RequestParam Long periodoAcademicoId) {
        service.cerrarCorte(cursoId, periodoAcademicoId);
        return ResponseEntity.noContent().build();
    }

    /** RF-36 / RB-15: registrar novedad (correccion de corte cerrado). */
    @PostMapping("/{id}/novedad")
    @PreAuthorize(COORDINADOR)
    @Operation(summary = "Registrar novedad de nota (RF-36, RB-15)")
    public ResponseEntity<CalificacionDto> novedad(@PathVariable Long id,
                                                   @Valid @RequestBody NovedadRequest req) {
        return ResponseEntity.ok(service.registrarNovedad(id, req.nuevoValor(), req.motivo()));
    }

    /** RF-35 / RB-19 / RD-11: generar boletin (corte cerrado). */
    @GetMapping("/boletin")
    @PreAuthorize(CONSULTA)
    @Operation(summary = "Generar boletin de calificaciones (RF-35, RB-19)")
    public ResponseEntity<BoletinDto> boletin(@RequestParam Long estudianteId,
                                              @RequestParam Long cursoId,
                                              @RequestParam Long periodoAcademicoId) {
        return ResponseEntity.ok(service.boletin(estudianteId, cursoId, periodoAcademicoId));
    }

    /** RF-37: historico de calificaciones de un estudiante. */
    @GetMapping("/historico/{estudianteId}")
    @PreAuthorize(CONSULTA)
    @Operation(summary = "Consultar historico de calificaciones (RF-37)")
    public ResponseEntity<List<CalificacionDto>> historico(@PathVariable Long estudianteId) {
        return ResponseEntity.ok(service.historico(estudianteId));
    }
}
