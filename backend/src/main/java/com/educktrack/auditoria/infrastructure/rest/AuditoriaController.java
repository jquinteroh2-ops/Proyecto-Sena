package com.educktrack.auditoria.infrastructure.rest;

import com.educktrack.auditoria.application.ConsultaAuditoriaService;
import com.educktrack.auditoria.domain.TipoOperacion;
import com.educktrack.auditoria.infrastructure.rest.AuditoriaDtos.PaginaAuditoriaDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta del log de auditoria (RS-07, RF-05, RF-63).
 *
 * <p>Todo el controlador es competencia exclusiva del Administrador: el log
 * revela que hizo cada persona y a que hora, de modo que abrirlo a mas roles
 * convertiria una herramienta de control en una de vigilancia entre
 * companeros. El registro de entradas lo hace el sistema (RF-63) desde los
 * propios casos de uso, por eso aqui no hay ningun endpoint de escritura.</p>
 */
@RestController
@RequestMapping("/api/auditoria")
@PreAuthorize("hasRole('ADMINISTRADOR')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Auditoria", description = "Log de operaciones criticas e historial de accesos (RS-07, RF-05)")
public class AuditoriaController {

    private final ConsultaAuditoriaService service;

    public AuditoriaController(ConsultaAuditoriaService service) {
        this.service = service;
    }

    /** RF-63 / RS-07: log de operaciones criticas, filtrable y paginado. */
    @GetMapping
    @Operation(summary = "Consultar el log de auditoria (RS-07, RF-63)")
    public ResponseEntity<PaginaAuditoriaDto> consultar(
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) TipoOperacion operacion,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamano) {
        return ResponseEntity.ok(service.consultar(usuario, operacion, pagina, tamano));
    }

    /** RF-05: historial de inicios de sesion de una cuenta. */
    @GetMapping("/accesos")
    @Operation(summary = "Consultar el historial de accesos de un usuario (RF-05)")
    public ResponseEntity<PaginaAuditoriaDto> accesos(
            @RequestParam String usuario,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamano) {
        return ResponseEntity.ok(service.historialDeAccesos(usuario, pagina, tamano));
    }
}
