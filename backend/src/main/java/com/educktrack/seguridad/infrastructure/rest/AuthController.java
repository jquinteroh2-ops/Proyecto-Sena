package com.educktrack.seguridad.infrastructure.rest;

import com.educktrack.seguridad.application.AutenticacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints de autenticacion (modulo Seguridad).
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticacion", description = "Inicio y cierre de sesion (RF-60, RF-61, RF-64)")
public class AuthController {

    private final AutenticacionService autenticacionService;

    public AuthController(AutenticacionService autenticacionService) {
        this.autenticacionService = autenticacionService;
    }

    /** RF-60: autentica al usuario y emite un token JWT. */
    @PostMapping("/login")
    @Operation(summary = "Autenticar usuario y emitir JWT (RF-60)")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(autenticacionService.login(request.correo(), request.password()));
    }

    /**
     * RF-61: cierra la sesion. DECISION DE DISENO: al ser JWT sin estado (RS-04),
     * el cierre de sesion se resuelve descartando el token en el cliente; este
     * endpoint limpia el contexto del servidor y confirma la operacion. Una lista
     * de revocacion server-side queda como mejora futura si se requiere.
     */
    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesion (RF-61)")
    public ResponseEntity<Map<String, String>> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("mensaje", "Sesion cerrada correctamente."));
    }

    /**
     * RF-64: solicitud de recuperacion de contrasena. DECISION DE DISENO: el envio
     * real del correo con enlace temporal (HU-04) se implementa en la Fase 8
     * (modulo de notificaciones); aqui se expone el endpoint publico que siempre
     * responde 200 para no revelar si el correo existe.
     */
    @PostMapping("/recuperar-password")
    @Operation(summary = "Solicitar recuperacion de contrasena (RF-64)")
    public ResponseEntity<Map<String, String>> recuperarPassword(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(Map.of("mensaje",
                "Si el correo esta registrado, se enviaran instrucciones de recuperacion."));
    }
}
