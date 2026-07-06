package com.educktrack.shared.domain;

/**
 * Excepcion de dominio que se lanza cuando se viola una regla de negocio (RB-xx).
 *
 * <p>Se lanza desde la capa de dominio/aplicacion (nunca solo en el frontend) y
 * la capa REST la traduce a una respuesta HTTP con mensaje claro en espanol
 * (RNF-10). Cada punto de lanzamiento debe citar el identificador de la RB.</p>
 */
public class ReglaNegocioException extends RuntimeException {

    private final String codigoRegla;

    public ReglaNegocioException(String codigoRegla, String mensaje) {
        super(mensaje);
        this.codigoRegla = codigoRegla;
    }

    /** Identificador de la regla de negocio violada, p. ej. "RB-14". */
    public String getCodigoRegla() {
        return codigoRegla;
    }
}
