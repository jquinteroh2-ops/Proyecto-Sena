package com.educktrack.docentes.infrastructure.rest;

import com.educktrack.docentes.application.GestionDocenteService;
import com.educktrack.docentes.infrastructure.rest.DocenteDtos.ActualizarDocenteRequest;
import com.educktrack.docentes.infrastructure.rest.DocenteDtos.DocenteDto;
import com.educktrack.docentes.infrastructure.rest.DocenteDtos.RegistrarDocenteRequest;
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
 * Gestion de docentes (RF-12, RF-13). El registro y edicion son competencia del
 * Administrador (RS-03); la consulta la comparten coordinacion y rectoria.
 */
@RestController
@RequestMapping("/api/docentes")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Docentes", description = "Gestion integral de docentes (RF-12, RF-13)")
public class DocenteController {

    private static final String GESTION = "hasRole('ADMINISTRADOR')";
    private static final String CONSULTA = "hasAnyRole('COORDINADOR_ACADEMICO','RECTOR','ADMINISTRADOR')";

    private final GestionDocenteService service;

    public DocenteController(GestionDocenteService service) {
        this.service = service;
    }

    /** RF-12: registrar docente. */
    @PostMapping
    @PreAuthorize(GESTION)
    @Operation(summary = "Registrar docente (RF-12)")
    public ResponseEntity<DocenteDto> registrar(@Valid @RequestBody RegistrarDocenteRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(req));
    }

    /** RF-13: editar datos del docente. */
    @PutMapping("/{id}")
    @PreAuthorize(GESTION)
    @Operation(summary = "Editar datos del docente (RF-13)")
    public ResponseEntity<DocenteDto> actualizar(@PathVariable Long id,
                                                 @Valid @RequestBody ActualizarDocenteRequest req) {
        return ResponseEntity.ok(service.actualizar(id, req));
    }

    @GetMapping("/{id}")
    @PreAuthorize(CONSULTA)
    @Operation(summary = "Consultar docente")
    public ResponseEntity<DocenteDto> consultar(@PathVariable Long id) {
        return ResponseEntity.ok(service.consultar(id));
    }

    @GetMapping
    @PreAuthorize(CONSULTA)
    @Operation(summary = "Listar docentes")
    public ResponseEntity<List<DocenteDto>> listar() {
        return ResponseEntity.ok(service.listar());
    }
}
