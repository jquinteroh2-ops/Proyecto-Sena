package com.educktrack.configuracion.infrastructure.rest;

import com.educktrack.configuracion.application.PeriodoAcademicoService;
import com.educktrack.configuracion.infrastructure.rest.PeriodoDtos.CrearPeriodoRequest;
import com.educktrack.configuracion.infrastructure.rest.PeriodoDtos.PeriodoDto;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Configuracion de periodos academicos (RF-57, RB-05). Version base de la Fase 3;
 * el panel de configuracion completo (jornadas, sedes, parametros) es la Fase 9.
 */
@RestController
@RequestMapping("/api/periodos")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Periodos academicos", description = "Configuracion de periodos (RF-57, RB-05)")
public class PeriodoAcademicoController {

    private static final String GESTION = "hasAnyRole('ADMINISTRADOR','COORDINADOR_ACADEMICO','RECTOR')";
    private static final String CONSULTA = "hasAnyRole('DOCENTE','COORDINADOR_ACADEMICO','RECTOR','ADMINISTRADOR')";

    private final PeriodoAcademicoService service;

    public PeriodoAcademicoController(PeriodoAcademicoService service) {
        this.service = service;
    }

    /** RF-57: crear periodo academico. */
    @PostMapping
    @PreAuthorize(GESTION)
    @Operation(summary = "Crear periodo academico (RF-57)")
    public ResponseEntity<PeriodoDto> crear(@Valid @RequestBody CrearPeriodoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    /** RF-57 / RB-05: activar periodo academico. */
    @PostMapping("/{id}/activar")
    @PreAuthorize(GESTION)
    @Operation(summary = "Activar periodo academico (RB-05)")
    public ResponseEntity<PeriodoDto> activar(@PathVariable Long id) {
        return ResponseEntity.ok(service.activar(id));
    }

    @GetMapping
    @PreAuthorize(CONSULTA)
    @Operation(summary = "Listar periodos academicos")
    public ResponseEntity<List<PeriodoDto>> listar() {
        return ResponseEntity.ok(service.listar());
    }
}
