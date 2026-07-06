package com.educktrack.cursos.infrastructure.rest;

import com.educktrack.cursos.application.GestionCursoService;
import com.educktrack.cursos.infrastructure.rest.CursoDtos.CupoDto;
import com.educktrack.cursos.infrastructure.rest.CursoDtos.CursoDto;
import com.educktrack.cursos.infrastructure.rest.CursoDtos.EstudianteEnCursoDto;
import com.educktrack.cursos.infrastructure.rest.CursoDtos.GuardarCursoRequest;
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
 * Gestion de cursos (RF-43..RF-46). Competencia del Coordinador Academico.
 */
@RestController
@RequestMapping("/api/cursos")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cursos", description = "Gestion de cursos y cupos (RF-43..RF-46)")
public class CursoController {

    private static final String GESTION = "hasAnyRole('COORDINADOR_ACADEMICO','ADMINISTRADOR')";
    private static final String CONSULTA = "hasAnyRole('DOCENTE','COORDINADOR_ACADEMICO','RECTOR','ADMINISTRADOR')";

    private final GestionCursoService service;

    public CursoController(GestionCursoService service) {
        this.service = service;
    }

    /** RF-43: registrar curso. */
    @PostMapping
    @PreAuthorize(GESTION)
    @Operation(summary = "Registrar curso (RF-43)")
    public ResponseEntity<CursoDto> registrar(@Valid @RequestBody GuardarCursoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(req));
    }

    /** RF-44: editar curso. */
    @PutMapping("/{id}")
    @PreAuthorize(GESTION)
    @Operation(summary = "Editar curso (RF-44)")
    public ResponseEntity<CursoDto> actualizar(@PathVariable Long id,
                                               @Valid @RequestBody GuardarCursoRequest req) {
        return ResponseEntity.ok(service.actualizar(id, req));
    }

    @GetMapping("/{id}")
    @PreAuthorize(CONSULTA)
    @Operation(summary = "Consultar curso")
    public ResponseEntity<CursoDto> consultar(@PathVariable Long id) {
        return ResponseEntity.ok(service.consultar(id));
    }

    @GetMapping
    @PreAuthorize(CONSULTA)
    @Operation(summary = "Listar cursos")
    public ResponseEntity<List<CursoDto>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    /** RF-45: listar estudiantes matriculados en el curso. */
    @GetMapping("/{id}/estudiantes")
    @PreAuthorize(CONSULTA)
    @Operation(summary = "Listar estudiantes del curso (RF-45)")
    public ResponseEntity<List<EstudianteEnCursoDto>> listarEstudiantes(@PathVariable Long id) {
        return ResponseEntity.ok(service.listarEstudiantes(id));
    }

    /** RF-46: consultar cupo disponible (RB-17). */
    @GetMapping("/{id}/cupo")
    @PreAuthorize(CONSULTA)
    @Operation(summary = "Consultar cupo disponible (RF-46, RB-17)")
    public ResponseEntity<CupoDto> consultarCupo(@PathVariable Long id) {
        return ResponseEntity.ok(service.consultarCupo(id));
    }
}
