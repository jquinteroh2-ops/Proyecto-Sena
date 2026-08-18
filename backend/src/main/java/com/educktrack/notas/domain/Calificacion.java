package com.educktrack.notas.domain;

import com.educktrack.shared.domain.ReglaNegocioException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Modelo de dominio de una calificacion (RF-31). Aplica la escala institucional
 * (RB-03), que desde la Fase 9 <strong>se recibe</strong> en vez de estar
 * cableada: RF-59 pide que la defina el Rector.
 *
 * <p><strong>El valor es {@link BigDecimal} y no {@code double} (Fase 8).</strong>
 * {@code double} es binario y no representa exactamente valores como 2.9 o
 * 3.05. Con notas eso no es teorico: RB-12 decide la aprobacion comparando
 * contra la nota aprobatoria, y una nota que vale 2.9999999999999996 cae del
 * lado equivocado de una comparacion que decide si alguien pierde el ano.</p>
 */
public class Calificacion {

    /**
     * Decimales con los que se registran y comparan las notas.
     *
     * <p>No es configurable: lo fija el esquema, {@code DECIMAL(3,2)} (V14).
     * Cambiarlo sin migrar la columna haria que lo guardado y lo mostrado
     * discrepasen.</p>
     */
    public static final int ESCALA = 2;

    private final BigDecimal valor;
    private final EscalaCalificacion escala;

    public Calificacion(BigDecimal valor, EscalaCalificacion escala) {
        if (escala == null) {
            throw new ReglaNegocioException("RB-03", "No hay escala de calificacion definida.");
        }
        this.escala = escala;
        this.valor = validar(valor, escala);
    }

    /** RB-03: la nota debe estar dentro de la escala institucional. */
    private static BigDecimal validar(BigDecimal valor, EscalaCalificacion escala) {
        if (valor == null) {
            throw new ReglaNegocioException("RB-03", "La calificacion es obligatoria.");
        }
        // Se normaliza antes de comparar: 3.0 y 3.00 son el mismo numero, y
        // comparar con equals los consideraria distintos.
        BigDecimal normalizado = normalizar(valor);
        if (!escala.contiene(normalizado)) {
            throw new ReglaNegocioException("RB-03",
                    "La calificacion debe estar entre " + escala.minima().toPlainString()
                            + " y " + escala.maxima().toPlainString() + ".");
        }
        return normalizado;
    }

    /** Lleva un valor a la escala de dos decimales con que se guardan las notas. */
    public static BigDecimal normalizar(BigDecimal valor) {
        return valor.setScale(ESCALA, RoundingMode.HALF_UP);
    }

    /** RB-12 / RD-01: indica si la nota es aprobatoria. */
    public boolean esAprobatoria() {
        return escala.esAprobatoria(valor);
    }

    /** RB-13: indica si debe notificarse bajo rendimiento. */
    public boolean esBajoRendimiento() {
        return !esAprobatoria();
    }

    public BigDecimal getValor() {
        return valor;
    }
}
