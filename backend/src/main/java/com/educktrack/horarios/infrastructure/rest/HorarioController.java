package com.educktrack.horarios.infrastructure.rest;

import com.educktrack.horarios.application.HorarioService;
import com.educktrack.horarios.infrastructure.rest.HorarioDtos.AsignarHorarioRequest;
import com.educktrack.horarios.infrastructure.rest.HorarioDtos.BloqueDto;
import com.educktrack.horarios.infrastructure.rest.HorarioDtos.CrearBloqueRequest;
import com.educktrack.horarios.infrastructure.rest.HorarioDtos.HorarioItemDto;
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
 * Gestion de horarios (RF-21..RF-25). El registro y la asignacion son
 * competencia del Coordinador Academico; las consultas las comparten docentes.
 */
@RestController
@RequestMapping("/api/horarios")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Horarios", description = "Bloques, asignaciones y validacion de cruces (RF-21..RF-25, RB-18)")
public class HorarioController {

    private static final String GESTION = "hasAnyRole('COORDINADOR_ACADEMICO','ADMINISTRADOR')";
    private static final String CONSULTA = "hasAnyRole('DOCENTE','COORDINADOR_ACADEMICO','RECTOR','ADMINISTRADOR')";

    private final HorarioService service;

    public HorarioController(HorarioService service) {
        this.service = service;
    }

    /** RF-21: crear bloque horario. */
    @PostMapping("/bloques")
    @PreAuthorize(GESTION)
    @Operation(summary = "Registrar bloque horario (RF-21)")
    public ResponseEntity<BloqueDto> crearBloque(@Valid @RequestBody CrearBloqueRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearBloque(req));
    }

    @GetMapping("/bloques")
    @PreAuthorize(CONSULTA)
    @Operation(summary = "Listar bloques horarios")
    public ResponseEntity<List<BloqueDto>> listarBloques() {
        return ResponseEntity.ok(service.listarBloques());
    }

    /** RF-22 / RF-23 / RB-18: asignar materia/docente/curso a un bloque. */
    @PostMapping("/asignaciones")
    @PreAuthorize(GESTION)
    @Operation(summary = "Asignar materia a bloque horario (RF-22, RB-18)")
    public ResponseEntity<HorarioItemDto> asignar(@Valid @RequestBody AsignarHorarioRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.asignar(req));
    }

    /** RF-24: consultar horario por curso. */
    @GetMapping("/cursos/{cursoId}")
    @PreAuthorize(CONSULTA)
    @Operation(summary = "Consultar horario por curso (RF-24)")
    public ResponseEntity<List<HorarioItemDto>> horarioCurso(@PathVariable Long cursoId,
                                                             @RequestParam Long periodoAcademicoId) {
        return ResponseEntity.ok(service.horarioCurso(cursoId, periodoAcademicoId));
    }

    /** RF-25: consultar horario por docente. */
    @GetMapping("/docentes/{docenteId}")
    @PreAuthorize(CONSULTA)
    @Operation(summary = "Consultar horario por docente (RF-25)")
    public ResponseEntity<List<HorarioItemDto>> horarioDocente(@PathVariable Long docenteId,
                                                               @RequestParam Long periodoAcademicoId) {
        return ResponseEntity.ok(service.horarioDocente(docenteId, periodoAcademicoId));
    }
}
