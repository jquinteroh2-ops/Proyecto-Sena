package com.educktrack.cursos.infrastructure.rest;

import com.educktrack.cursos.application.PlanEstudiosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Plan de estudios de un curso (RF-19, RD-05). Competencia del Coordinador.
 */
@RestController
@RequestMapping("/api/cursos/{cursoId}/plan-estudios")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Plan de estudios", description = "Materias que componen un curso (RF-19)")
public class PlanEstudiosController {

    private static final String GESTION = "hasAnyRole('COORDINADOR_ACADEMICO','ADMINISTRADOR')";
    private static final String CONSULTA = "hasAnyRole('DOCENTE','COORDINADOR_ACADEMICO','RECTOR','ADMINISTRADOR')";

    private final PlanEstudiosService service;

    public PlanEstudiosController(PlanEstudiosService service) {
        this.service = service;
    }

    public record AsociarMateriaRequest(@NotNull(message = "La materia es obligatoria") Long materiaId) {
    }

    /** RF-19: asociar una materia al plan de estudios del curso. */
    @PostMapping
    @PreAuthorize(GESTION)
    @Operation(summary = "Asociar materia al plan de estudios (RF-19)")
    public ResponseEntity<Void> asociar(@PathVariable Long cursoId,
                                        @RequestBody AsociarMateriaRequest req) {
        service.asociarMateria(cursoId, req.materiaId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** Lista las materias (ids) del plan de estudios del curso. */
    @GetMapping
    @PreAuthorize(CONSULTA)
    @Operation(summary = "Listar materias del plan de estudios")
    public ResponseEntity<List<Long>> materias(@PathVariable Long cursoId) {
        return ResponseEntity.ok(service.materiasDelCurso(cursoId));
    }
}
