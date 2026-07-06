package com.educktrack.seguridad.infrastructure.rest;

import java.util.List;

/**
 * Datos del usuario autenticado que se devuelven al cliente tras el login.
 * Nunca incluye el hash de la contrasena.
 */
public record UsuarioAutenticadoDto(
        Long id,
        String nombre,
        String correo,
        List<String> roles,
        boolean debeCambiarPassword) {
}
