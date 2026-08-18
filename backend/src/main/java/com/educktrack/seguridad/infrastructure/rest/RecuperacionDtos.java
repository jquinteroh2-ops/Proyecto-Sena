package com.educktrack.seguridad.infrastructure.rest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTOs de recuperacion de contrasena (RF-64, HU-04).
 */
public final class RecuperacionDtos {

    private RecuperacionDtos() {
    }

    /** RF-64: solicitud del enlace de recuperacion. */
    public record RecuperarPasswordRequest(
            @NotBlank(message = "El correo institucional es obligatorio")
            @Email(message = "El correo no tiene un formato valido")
            String correo) {
    }

    /**
     * RF-64: restablecimiento con el enlace recibido.
     *
     * <p>La longitud minima no se declara aqui con {@code @Size}: la politica
     * vive en {@code PoliticaPassword} para que registro y recuperacion no
     * puedan divergir. Una anotacion por DTO es como se llego a tener la regla
     * en un solo punto de entrada.</p>
     */
    public record RestablecerPasswordRequest(
            @NotBlank(message = "El enlace de recuperacion es obligatorio")
            String token,

            @NotBlank(message = "La nueva contrasena es obligatoria")
            String nuevaPassword) {
    }
}
