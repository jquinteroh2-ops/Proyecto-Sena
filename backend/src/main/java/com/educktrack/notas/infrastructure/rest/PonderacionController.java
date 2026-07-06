package com.educktrack.notas.infrastructure.rest;

import com.educktrack.notas.application.PonderacionService;
import com.educktrack.notas.infrastructure.rest.NotaDtos.ConfigurarPonderacionRequest;
import com.educktrack.notas.infrastructure.rest.NotaDtos.PonderacionDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Configuracion de ponderaciones de evaluacion (RF-20, RB-07). Competencia del
 * Docente de la materia.
 */
@RestController
@RequestMapping("/api/ponderaciones")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Ponderaciones", description = "Ponderacion de evaluaciones por materia (RF-20, RB-07)")
public class PonderacionController {

    private static final String GESTION = "hasAnyRole('DOCENTE','COORDINADOR_ACADEMICO','ADMINISTRADOR')";

    private final PonderacionService service;

    public PonderacionController(PonderacionService service) {
        this.service = service;
    }

    /** RF-20 / RB-07: configurar ponderaciones (suman 100%). */
    @PostMapping
    @PreAuthorize(GESTION)
    @Operation(summary = "Configurar ponderacion de evaluaciones (RF-20, RB-07)")
    public ResponseEntity<List<PonderacionDto>> configurar(@Valid @RequestBody ConfigurarPonderacionRequest req) {
        return ResponseEntity.ok(service.configurar(req));
    }

    @GetMapping
    @PreAuthorize(GESTION)
    @Operation(summary = "Listar ponderaciones de una materia/periodo")
    public ResponseEntity<List<PonderacionDto>> listar(@RequestParam Long materiaId,
                                                       @RequestParam Long periodoAcademicoId) {
        return ResponseEntity.ok(service.listar(materiaId, periodoAcademicoId));
    }
}
