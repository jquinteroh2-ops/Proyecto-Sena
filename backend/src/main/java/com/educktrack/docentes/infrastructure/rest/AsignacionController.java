package com.educktrack.docentes.infrastructure.rest;

import com.educktrack.docentes.application.AsignacionAcademicaService;
import com.educktrack.docentes.infrastructure.rest.AsignacionDtos.AsignacionDto;
import com.educktrack.docentes.infrastructure.rest.AsignacionDtos.AsignarMateriaRequest;
import com.educktrack.docentes.infrastructure.rest.AsignacionDtos.CargaAcademicaDto;
import com.educktrack.docentes.infrastructure.rest.AsignacionDtos.DesignarDirectorRequest;
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

/**
 * Asignacion academica de docentes (RF-14, RF-15, RF-16). Competencia del
 * Coordinador Academico.
 */
@RestController
@RequestMapping("/api/asignaciones")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Asignaciones", description = "Carga academica y direccion de grupo (RF-14, RF-15, RF-16)")
public class AsignacionController {

    private static final String GESTION = "hasAnyRole('COORDINADOR_ACADEMICO','ADMINISTRADOR')";
    private static final String CONSULTA = "hasAnyRole('DOCENTE','COORDINADOR_ACADEMICO','RECTOR','ADMINISTRADOR')";

    private final AsignacionAcademicaService service;

    public AsignacionController(AsignacionAcademicaService service) {
        this.service = service;
    }

    /** RF-14 / RB-16: asignar materia y curso a un docente. */
    @PostMapping
    @PreAuthorize(GESTION)
    @Operation(summary = "Asignar materia a docente (RF-14, RB-16)")
    public ResponseEntity<AsignacionDto> asignar(@Valid @RequestBody AsignarMateriaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.asignarMateria(req));
    }

    /** RF-15: consultar carga academica de un docente en un periodo. */
    @GetMapping("/carga")
    @PreAuthorize(CONSULTA)
    @Operation(summary = "Consultar carga academica del docente (RF-15)")
    public ResponseEntity<CargaAcademicaDto> carga(@RequestParam Long docenteId,
                                                   @RequestParam Long periodoAcademicoId) {
        return ResponseEntity.ok(service.consultarCarga(docenteId, periodoAcademicoId));
    }

    /** RF-16 / RB-02: designar director de grupo de un curso. */
    @PostMapping("/cursos/{cursoId}/director")
    @PreAuthorize(GESTION)
    @Operation(summary = "Designar director de grupo (RF-16, RB-02)")
    public ResponseEntity<Void> designarDirector(@PathVariable Long cursoId,
                                                 @Valid @RequestBody DesignarDirectorRequest req) {
        service.designarDirector(cursoId, req.docenteId());
        return ResponseEntity.noContent().build();
    }
}
