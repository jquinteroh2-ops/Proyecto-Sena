package com.educktrack.configuracion.infrastructure.rest;

import com.educktrack.configuracion.domain.ParametroInstitucional;
import jakarta.validation.constraints.NotBlank;

/**
 * DTOs de parametros institucionales (RF-59).
 */
public final class ParametroDtos {

    private ParametroDtos() {
    }

    /**
     * El tipo viaja en la respuesta para que el frontend sepa que control
     * mostrar y que validar antes de enviar, sin duplicar aqui las reglas.
     */
    public record ParametroDto(ParametroInstitucional clave, String valor,
                               ParametroInstitucional.Tipo tipo) {
    }

    /**
     * El valor llega como texto: los parametros tienen tipos distintos y quien
     * los interpreta es {@code ParametrosService}, que es tambien quien conoce
     * el rango admisible de cada uno.
     */
    public record ActualizarParametroRequest(
            @NotBlank(message = "El valor es obligatorio") String valor) {
    }
}
