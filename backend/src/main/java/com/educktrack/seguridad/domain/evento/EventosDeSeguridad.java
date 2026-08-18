package com.educktrack.seguridad.domain.evento;

/**
 * Hechos del modulo de seguridad que otros modulos pueden querer atender
 * (RS-08).
 */
public final class EventosDeSeguridad {

    private EventosDeSeguridad() {
    }

    /**
     * RF-64 / HU-04: la contrasena de una cuenta se restablecio mediante un
     * enlace de recuperacion.
     *
     * <p>HU-04 exige avisar de que el cambio ocurrio. El aviso no es una
     * cortesia: si quien recibe el mensaje no pidio el cambio, es la unica
     * senal de que alguien mas entro a su correo, y llega a tiempo de
     * reaccionar.</p>
     *
     * <p><strong>El evento no lleva el token ni la contrasena.</strong> El
     * enlace de recuperacion se envia por correo directo y nunca pasa por el
     * modulo de notificaciones, que ademas de enviar deja copia en la bandeja
     * interna: una credencial viva no debe quedar guardada en una bandeja que
     * solo se puede leer entrando a la cuenta que precisamente no se puede
     * abrir.</p>
     */
    public record PasswordRestablecida(Long usuarioId, String correoInstitucional) {
    }
}
