package com.educktrack.materias.infrastructure.rest;

import com.educktrack.materias.application.GestionMateriaService;
import com.educktrack.materias.infrastructure.rest.MateriaDtos.GuardarMateriaRequest;
import com.educktrack.materias.infrastructure.rest.MateriaDtos.MateriaDto;
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
 * Gestion de materias (RF-17, RF-18). Competencia del Coordinador Academico.
 */
@RestController
@RequestMapping("/api/materias")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Materias", description = "Gestion de materias (RF-17, RF-18)")
public class MateriaController {

    private static final String GESTION = "hasAnyRole('COORDINADOR_ACADEMICO','ADMINISTRADOR')";
    private static final String CONSULTA = "hasAnyRole('DOCENTE','COORDINADOR_ACADEMICO','RECTOR','ADMINISTRADOR')";

    private final GestionMateriaService service;

    public MateriaController(GestionMateriaService service) {
        this.service = service;
    }

    /** RF-17: registrar materia. */
    @PostMapping
    @PreAuthorize(GESTION)
    @Operation(summary = "Registrar materia (RF-17)")
    public ResponseEntity<MateriaDto> registrar(@Valid @RequestBody GuardarMateriaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(req));
    }

    /** RF-18: editar materia. */
    @PutMapping("/{id}")
    @PreAuthorize(GESTION)
    @Operation(summary = "Editar materia (RF-18)")
    public ResponseEntity<MateriaDto> actualizar(@PathVariable Long id,
                                                 @Valid @RequestBody GuardarMateriaRequest req) {
        return ResponseEntity.ok(service.actualizar(id, req));
    }

    @GetMapping("/{id}")
    @PreAuthorize(CONSULTA)
    @Operation(summary = "Consultar materia")
    public ResponseEntity<MateriaDto> consultar(@PathVariable Long id) {
        return ResponseEntity.ok(service.consultar(id));
    }

    @GetMapping
    @PreAuthorize(CONSULTA)
    @Operation(summary = "Listar materias")
    public ResponseEntity<List<MateriaDto>> listar() {
        return ResponseEntity.ok(service.listar());
    }
}
