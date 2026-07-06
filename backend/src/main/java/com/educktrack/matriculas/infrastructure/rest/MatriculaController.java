package com.educktrack.matriculas.infrastructure.rest;

import com.educktrack.matriculas.application.MatriculaService;
import com.educktrack.matriculas.infrastructure.rest.MatriculaDtos.MatriculaDto;
import com.educktrack.matriculas.infrastructure.rest.MatriculaDtos.MatricularRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Matricula de estudiantes (RF-09). Competencia del Coordinador Academico.
 */
@RestController
@RequestMapping("/api/matriculas")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Matriculas", description = "Matricula de estudiantes en cursos (RF-09)")
public class MatriculaController {

    private static final String GESTION = "hasAnyRole('COORDINADOR_ACADEMICO','ADMINISTRADOR')";

    private final MatriculaService service;

    public MatriculaController(MatriculaService service) {
        this.service = service;
    }

    /** RF-09 / HU-06: matricular estudiante (aplica RB-01, RB-17, RB-11). */
    @PostMapping
    @PreAuthorize(GESTION)
    @Operation(summary = "Matricular estudiante en curso (RF-09)")
    public ResponseEntity<MatriculaDto> matricular(@Valid @RequestBody MatricularRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.matricular(req));
    }
}
