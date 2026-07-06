package com.educktrack.seguridad.infrastructure.rest;

/**
 * Respuesta del login (RF-60): token JWT y datos del usuario autenticado.
 */
public record LoginResponse(
        String token,
        String tipo,
        long expiraEnSegundos,
        UsuarioAutenticadoDto usuario) {
}
