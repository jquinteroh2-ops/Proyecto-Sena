package com.educktrack.usuarios.domain.evento;

/**
 * Hechos del modulo de usuarios que otros modulos pueden querer atender
 * (RS-08).
 */
public final class EventosDeUsuarios {

    private EventosDeUsuarios() {
    }

    /**
     * RF-03 / HU-02: se desactivo una cuenta. HU-02 exige notificar por correo
     * institucional a la persona afectada, que de otro modo solo se enteraria
     * al intentar entrar y no poder.
     */
    public record UsuarioDesactivado(
            Long usuarioId,
            String correoInstitucional,
            String nombre) {
    }
}
