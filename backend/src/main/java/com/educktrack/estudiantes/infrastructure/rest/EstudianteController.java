package com.educktrack.estudiantes.infrastructure.rest;

import com.educktrack.estudiantes.application.GestionEstudianteService;
import com.educktrack.estudiantes.infrastructure.rest.EstudianteDtos.ActualizarEstudianteRequest;
import com.educktrack.estudiantes.infrastructure.rest.EstudianteDtos.EstudianteDto;
import com.educktrack.estudiantes.infrastructure.rest.EstudianteDtos.RegistrarEstudianteRequest;
import com.educktrack.estudiantes.infrastructure.rest.EstudianteDtos.RetirarEstudianteRequest;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Gestion de estudiantes (RF-06..RF-10). El registro, edicion y retiro son
 * competencia del Coordinador Academico (RS-03); la consulta la comparten
 * docentes y coordinacion.
 */
@RestController
@RequestMapping("/api/estudiantes")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Estudiantes", description = "Gestion integral de estudiantes (RF-06..RF-10)")
public class EstudianteController {

    private static final String GESTION = "hasAnyRole('COORDINADOR_ACADEMICO','RECTOR','ADMINISTRADOR')";
    private static final String CONSULTA = "hasAnyRole('DOCENTE','COORDINADOR_ACADEMICO','RECTOR','ADMINISTRADOR')";

    private final GestionEstudianteService service;

    public EstudianteController(GestionEstudianteService service) {
        this.service = service;
    }

    /** RF-06: registrar estudiante. */
    @PostMapping
    @PreAuthorize(GESTION)
    @Operation(summary = "Registrar estudiante (RF-06)")
    public ResponseEntity<EstudianteDto> registrar(@Valid @RequestBody RegistrarEstudianteRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(req));
    }

    /** RF-07: editar datos del estudiante. */
    @PutMapping("/{id}")
    @PreAuthorize(GESTION)
    @Operation(summary = "Editar datos del estudiante (RF-07)")
    public ResponseEntity<EstudianteDto> actualizar(@PathVariable Long id,
                                                    @Valid @RequestBody ActualizarEstudianteRequest req) {
        return ResponseEntity.ok(service.actualizar(id, req));
    }

    /** RF-08: consultar perfil del estudiante. */
    @GetMapping("/{id}")
    @PreAuthorize(CONSULTA)
    @Operation(summary = "Consultar perfil del estudiante (RF-08)")
    public ResponseEntity<EstudianteDto> consultar(@PathVariable Long id) {
        return ResponseEntity.ok(service.consultar(id));
    }

    @GetMapping
    @PreAuthorize(CONSULTA)
    @Operation(summary = "Listar estudiantes")
    public ResponseEntity<List<EstudianteDto>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    /** RF-10 / RB-20: retirar estudiante con motivo y autorizacion. */
    @PostMapping("/{id}/retirar")
    @PreAuthorize(GESTION)
    @Operation(summary = "Retirar estudiante (RF-10, RB-20)")
    public ResponseEntity<EstudianteDto> retirar(@PathVariable Long id,
                                                 @Valid @RequestBody RetirarEstudianteRequest req) {
        return ResponseEntity.ok(service.retirar(id, req));
    }
}
