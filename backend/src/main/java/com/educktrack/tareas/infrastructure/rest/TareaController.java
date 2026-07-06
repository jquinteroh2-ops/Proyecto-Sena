package com.educktrack.tareas.infrastructure.rest;

import com.educktrack.tareas.application.TareaService;
import com.educktrack.tareas.infrastructure.rest.TareaDtos.AsignarTareaRequest;
import com.educktrack.tareas.infrastructure.rest.TareaDtos.CalificarTareaRequest;
import com.educktrack.tareas.infrastructure.rest.TareaDtos.EntregaDto;
import com.educktrack.tareas.infrastructure.rest.TareaDtos.EntregarTareaRequest;
import com.educktrack.tareas.infrastructure.rest.TareaDtos.EstadoTareaDto;
import com.educktrack.tareas.infrastructure.rest.TareaDtos.TareaDto;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Gestion de tareas (RF-38..RF-42). El docente asigna y califica; el estudiante
 * entrega; padres y estudiantes consultan el estado.
 */
@RestController
@RequestMapping("/api/tareas")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Tareas", description = "Asignacion, entrega y calificacion de tareas (RF-38..RF-42)")
public class TareaController {

    private static final String DOCENTE = "hasAnyRole('DOCENTE','COORDINADOR_ACADEMICO','ADMINISTRADOR')";
    private static final String ENTREGA = "hasAnyRole('ESTUDIANTE','DOCENTE','COORDINADOR_ACADEMICO','ADMINISTRADOR')";
    private static final String CONSULTA = "hasAnyRole('ESTUDIANTE','PADRE_FAMILIA','DOCENTE','COORDINADOR_ACADEMICO','RECTOR','ADMINISTRADOR')";

    private final TareaService service;

    public TareaController(TareaService service) {
        this.service = service;
    }

    /** RF-38 / HU-23: asignar tarea. */
    @PostMapping
    @PreAuthorize(DOCENTE)
    @Operation(summary = "Asignar tarea (RF-38, HU-23)")
    public ResponseEntity<TareaDto> asignar(@Valid @RequestBody AsignarTareaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.asignar(req));
    }

    @GetMapping("/curso/{cursoId}")
    @PreAuthorize(CONSULTA)
    @Operation(summary = "Listar tareas de un curso")
    public ResponseEntity<List<TareaDto>> listarPorCurso(@PathVariable Long cursoId) {
        return ResponseEntity.ok(service.listarPorCurso(cursoId));
    }

    /** RF-39 / RB-10: entregar tarea. */
    @PostMapping("/{tareaId}/entregar")
    @PreAuthorize(ENTREGA)
    @Operation(summary = "Entregar tarea (RF-39, RB-10)")
    public ResponseEntity<EntregaDto> entregar(@PathVariable Long tareaId,
                                               @Valid @RequestBody EntregarTareaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.entregar(tareaId, req));
    }

    /** RF-40: calificar entrega. */
    @PostMapping("/entregas/{entregaId}/calificar")
    @PreAuthorize(DOCENTE)
    @Operation(summary = "Calificar tarea (RF-40)")
    public ResponseEntity<EntregaDto> calificar(@PathVariable Long entregaId,
                                                @Valid @RequestBody CalificarTareaRequest req) {
        return ResponseEntity.ok(service.calificar(entregaId, req));
    }

    /** RF-41: estado de tareas de un estudiante en un curso. */
    @GetMapping("/estado")
    @PreAuthorize(CONSULTA)
    @Operation(summary = "Consultar estado de tareas (RF-41)")
    public ResponseEntity<List<EstadoTareaDto>> estado(@RequestParam Long estudianteId,
                                                       @RequestParam Long cursoId) {
        return ResponseEntity.ok(service.estadoTareas(estudianteId, cursoId));
    }

    /** RF-42: tareas proximas a vencer. */
    @GetMapping("/proximas")
    @PreAuthorize(CONSULTA)
    @Operation(summary = "Tareas proximas a vencer (RF-42)")
    public ResponseEntity<List<TareaDto>> proximas(@RequestParam(defaultValue = "3") int dias) {
        return ResponseEntity.ok(service.proximasAVencer(dias));
    }
}
