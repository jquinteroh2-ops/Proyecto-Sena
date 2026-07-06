package com.educktrack.usuarios.infrastructure.rest;

import com.educktrack.usuarios.domain.NombreRol;

import java.util.List;

/**
 * Representacion de salida de un usuario (sin datos sensibles como el hash).
 */
public record UsuarioDto(
        Long id,
        String nombre,
        String correo,
        boolean activo,
        boolean debeCambiarPassword,
        List<NombreRol> roles) {
}
