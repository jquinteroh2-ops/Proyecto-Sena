package com.educktrack.usuarios.infrastructure.rest;

import com.educktrack.usuarios.application.GestionUsuarioService;
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
 * Gestion de cuentas de usuario. Solo el Administrador puede operar sobre
 * usuarios (RS-03, HU-01). El control de acceso se aplica con @PreAuthorize.
 */
@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Usuarios", description = "Administracion de cuentas y roles (RF-01, RF-03)")
public class UsuarioController {

    private final GestionUsuarioService gestionUsuarioService;

    public UsuarioController(GestionUsuarioService gestionUsuarioService) {
        this.gestionUsuarioService = gestionUsuarioService;
    }

    /** RF-01: registra una nueva cuenta de usuario. */
    @PostMapping
    @Operation(summary = "Registrar usuario (RF-01)")
    public ResponseEntity<UsuarioDto> registrar(@Valid @RequestBody RegistrarUsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gestionUsuarioService.registrar(request));
    }

    /** RF-03: desactiva una cuenta sin eliminar su historial. */
    @PostMapping("/{id}/desactivar")
    @Operation(summary = "Desactivar usuario (RF-03)")
    public ResponseEntity<UsuarioDto> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(gestionUsuarioService.desactivar(id));
    }

    /** Lista las cuentas registradas (soporte administrativo). */
    @GetMapping
    @Operation(summary = "Listar usuarios")
    public ResponseEntity<List<UsuarioDto>> listar() {
        return ResponseEntity.ok(gestionUsuarioService.listar());
    }
}
