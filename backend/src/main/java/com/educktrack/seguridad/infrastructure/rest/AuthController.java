package com.educktrack.seguridad.infrastructure.rest;

import com.educktrack.seguridad.application.AutenticacionService;
import com.educktrack.seguridad.application.RecuperacionPasswordService;
import com.educktrack.seguridad.infrastructure.rest.RecuperacionDtos.RecuperarPasswordRequest;
import com.educktrack.seguridad.infrastructure.rest.RecuperacionDtos.RestablecerPasswordRequest;
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
    private final RecuperacionPasswordService recuperacionService;

    public AuthController(AutenticacionService autenticacionService,
                          RecuperacionPasswordService recuperacionService) {
        this.autenticacionService = autenticacionService;
        this.recuperacionService = recuperacionService;
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
     * RF-64 / HU-04: solicita un enlace de recuperacion de contrasena.
     *
     * <p>Responde siempre lo mismo, exista o no la cuenta y salga o no el
     * correo. Distinguir los casos convertiria este endpoint publico en un
     * oraculo para averiguar que correos estan dados de alta.</p>
     */
    @PostMapping("/recuperar-password")
    @Operation(summary = "Solicitar recuperacion de contrasena (RF-64, HU-04)")
    public ResponseEntity<Map<String, String>> recuperarPassword(
            @Valid @RequestBody RecuperarPasswordRequest request) {
        recuperacionService.solicitar(request.correo());
        return ResponseEntity.ok(Map.of("mensaje",
                "Si el correo esta registrado, se enviaran instrucciones de recuperacion."));
    }

    /**
     * RF-64 / HU-04: restablece la contrasena consumiendo el enlace recibido.
     *
     * <p>El enlace caduca a los 30 minutos y ademas se consume al primer uso.
     * Un enlace invalido, caducado o ya gastado responde con el mismo error,
     * porque diferenciarlos solo ayudaria a quien esta probando enlaces.</p>
     */
    @PostMapping("/restablecer-password")
    @Operation(summary = "Restablecer contrasena con el enlace recibido (RF-64, HU-04)")
    public ResponseEntity<Map<String, String>> restablecerPassword(
            @Valid @RequestBody RestablecerPasswordRequest request) {
        recuperacionService.restablecer(request.token(), request.nuevaPassword());
        return ResponseEntity.ok(Map.of("mensaje",
                "Contrasena actualizada correctamente. Ya puede iniciar sesion."));
    }
}
