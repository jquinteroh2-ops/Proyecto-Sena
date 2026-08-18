package com.educktrack.notas.domain;

import com.educktrack.shared.domain.ReglaNegocioException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Modelo de dominio de una calificacion (RF-31). Encapsula la escala
 * institucional RB-03: valor numerico entre 1.0 y 5.0, con 3.0 como nota minima
 * aprobatoria (RD-01).
 *
 * <p><strong>El valor es {@link BigDecimal} y no {@code double} (Fase 8).</strong>
 * {@code double} es binario y no representa exactamente valores como 2.9 o
 * 3.05. Con notas eso no es teorico: RB-12 decide la aprobacion comparando
 * contra 3.0, y una nota que vale 2.9999999999999996 cae del lado equivocado de
 * una comparacion que decide si alguien pierde el ano.</p>
 */
public class Calificacion {

    /** Decimales con los que se registran y comparan las notas (DECIMAL(3,2)). */
    public static final int ESCALA = 2;

    public static final BigDecimal NOTA_MINIMA = new BigDecimal("1.00");
    public static final BigDecimal NOTA_MAXIMA = new BigDecimal("5.00");
    public static final BigDecimal NOTA_APROBATORIA = new BigDecimal("3.00");

    private final BigDecimal valor;

    public Calificacion(BigDecimal valor) {
        this.valor = validar(valor);
    }

    /** RB-03: la nota debe estar en la escala 1.0 a 5.0. */
    private static BigDecimal validar(BigDecimal valor) {
        if (valor == null) {
            throw new ReglaNegocioException("RB-03", "La calificacion es obligatoria.");
        }
        BigDecimal normalizado = normalizar(valor);
        // compareTo y no equals: 3.0 y 3.00 son el mismo numero con distinta
        // escala, y equals los considera distintos.
        if (normalizado.compareTo(NOTA_MINIMA) < 0 || normalizado.compareTo(NOTA_MAXIMA) > 0) {
            throw new ReglaNegocioException("RB-03",
                    "La calificacion debe estar entre " + NOTA_MINIMA.toPlainString()
                            + " y " + NOTA_MAXIMA.toPlainString() + ".");
        }
        return normalizado;
    }

    /** Lleva un valor a la escala institucional de dos decimales. */
    public static BigDecimal normalizar(BigDecimal valor) {
        return valor.setScale(ESCALA, RoundingMode.HALF_UP);
    }

    /** RB-12 / RD-01: indica si la nota es aprobatoria (>= 3.0). */
    public boolean esAprobatoria() {
        return esAprobatoria(valor);
    }

    /** RB-12 / RD-01: indica si un valor cualquiera de la escala es aprobatorio. */
    public static boolean esAprobatoria(BigDecimal valor) {
        return valor != null && valor.compareTo(NOTA_APROBATORIA) >= 0;
    }

    /** RB-13: indica si debe notificarse bajo rendimiento (< 3.0). */
    public boolean esBajoRendimiento() {
        return !esAprobatoria();
    }

    public BigDecimal getValor() {
        return valor;
    }
}
