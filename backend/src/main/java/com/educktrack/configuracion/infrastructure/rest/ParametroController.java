package com.educktrack.configuracion.infrastructure.rest;

import com.educktrack.configuracion.application.ParametrosService;
import com.educktrack.configuracion.domain.ParametroInstitucional;
import com.educktrack.configuracion.infrastructure.rest.ParametroDtos.ActualizarParametroRequest;
import com.educktrack.configuracion.infrastructure.rest.ParametroDtos.ParametroDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Parametros institucionales (RF-59, RS-14): escala de calificacion (RB-03),
 * porcentaje minimo de asistencia (RB-04) y carga maxima del docente (RB-09).
 */
@RestController
@RequestMapping("/api/configuracion/parametros")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Parametros institucionales",
        description = "Escala de calificacion y minimos institucionales (RF-59)")
public class ParametroController {

    /**
     * RF-59 nombra al <strong>Rector</strong> como responsable. El Administrador
     * entra porque es quien sostiene el sistema, pero Coordinacion no: cambiar
     * la escala altera la aprobacion de todo el colegio y no es una decision de
     * gestion diaria.
     */
    private static final String GESTION = "hasAnyRole('RECTOR','ADMINISTRADOR')";

    /** Leerlos es inofensivo y el frontend los necesita para mostrar la escala. */
    private static final String CONSULTA =
            "hasAnyRole('RECTOR','ADMINISTRADOR','COORDINADOR_ACADEMICO','COORDINADOR_CONVIVENCIA','DOCENTE')";

    private final ParametrosService service;

    public ParametroController(ParametrosService service) {
        this.service = service;
    }

    /** RF-59: consultar los parametros vigentes. */
    @GetMapping
    @PreAuthorize(CONSULTA)
    @Operation(summary = "Consultar parametros institucionales (RF-59)")
    public ResponseEntity<List<ParametroDto>> listar() {
        Map<ParametroInstitucional, String> vigentes = service.listar();
        return ResponseEntity.ok(vigentes.entrySet().stream()
                .map(e -> new ParametroDto(e.getKey(), e.getValue(), e.getKey().getTipo()))
                .toList());
    }

    /** RF-59 / RS-07: fijar el valor de un parametro. */
    @PutMapping("/{clave}")
    @PreAuthorize(GESTION)
    @Operation(summary = "Actualizar un parametro institucional (RF-59)")
    public ResponseEntity<List<ParametroDto>> actualizar(@PathVariable ParametroInstitucional clave,
                                                         @Valid @RequestBody ActualizarParametroRequest req) {
        service.actualizar(clave, req.valor());
        return listar();
    }
}
