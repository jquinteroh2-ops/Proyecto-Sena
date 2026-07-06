package com.educktrack.notificaciones.infrastructure.rest;

import com.educktrack.notificaciones.application.NotificacionService;
import com.educktrack.notificaciones.domain.CanalNotificacion;
import com.educktrack.notificaciones.infrastructure.rest.NotificacionDtos.BandejaDto;
import com.educktrack.notificaciones.infrastructure.rest.NotificacionDtos.ConfigurarCanalRequest;
import com.educktrack.notificaciones.infrastructure.rest.NotificacionDtos.EnviarManualRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Gestion de notificaciones (RF-52..RF-54). Configuracion por Administrador;
 * envio manual por Docente/Coordinador; bandeja por el propio usuario.
 */
@RestController
@RequestMapping("/api/notificaciones")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notificaciones", description = "Canal, envio manual y bandeja (RF-52..RF-54)")
public class NotificacionController {

    private static final String ADMIN = "hasRole('ADMINISTRADOR')";
    private static final String EMISOR = "hasAnyRole('DOCENTE','COORDINADOR_ACADEMICO','ADMINISTRADOR','RECTOR')";

    private final NotificacionService service;

    public NotificacionController(NotificacionService service) {
        this.service = service;
    }

    /** RF-52 / HU-28: configurar canal institucional. */
    @PostMapping("/config")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Configurar canal de notificacion (RF-52)")
    public ResponseEntity<Map<String, CanalNotificacion>> configurar(
            @Valid @RequestBody ConfigurarCanalRequest req) {
        return ResponseEntity.ok(Map.of("canal", service.configurarCanal(req.canal())));
    }

    @GetMapping("/config")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Consultar canal configurado")
    public ResponseEntity<Map<String, CanalNotificacion>> canal() {
        return ResponseEntity.ok(Map.of("canal", service.canalActual()));
    }

    /** RF-53: enviar notificacion manual. */
    @PostMapping
    @PreAuthorize(EMISOR)
    @Operation(summary = "Enviar notificacion manual (RF-53)")
    public ResponseEntity<Map<String, Integer>> enviar(@Valid @RequestBody EnviarManualRequest req) {
        int enviadas = service.enviarManual(req.destinatariosUsuarioId(), req.titulo(), req.mensaje());
        return ResponseEntity.ok(Map.of("destinatarios", enviadas));
    }

    /** RF-54 / HU-27: bandeja de notificaciones del usuario. */
    @GetMapping("/bandeja")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consultar bandeja de notificaciones (RF-54)")
    public ResponseEntity<BandejaDto> bandeja(@RequestParam Long usuarioId) {
        return ResponseEntity.ok(service.bandeja(usuarioId));
    }

    /** HU-27: marcar notificacion como leida. */
    @PostMapping("/{id}/leida")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Marcar notificacion como leida")
    public ResponseEntity<Void> marcarLeida(@PathVariable Long id) {
        service.marcarLeida(id);
        return ResponseEntity.noContent().build();
    }
}
