package com.educktrack.notas.domain;

import com.educktrack.shared.domain.ReglaNegocioException;

import java.math.BigDecimal;

/**
 * Escala de calificacion institucional (RB-03, RF-59).
 *
 * <p>Antes de la Fase 9 los tres limites eran constantes de
 * {@link Calificacion}. RF-59 pide que los defina el Rector, de modo que dejan
 * de ser propiedad del codigo y pasan a ser un dato que se le entrega al
 * dominio.</p>
 *
 * <p>El invariante se valida <strong>al construir la escala</strong> y no al
 * usarla: una escala imposible (aprobatoria por encima del maximo) haria que
 * ninguna nota aprobase nunca, y ese fallo aparecería mucho despues, calificando,
 * en vez de al guardarla.</p>
 */
public record EscalaCalificacion(BigDecimal minima, BigDecimal maxima, BigDecimal aprobatoria) {

    /** La escala del enunciado (RD-01): 1.0 a 5.0, aprobando desde 3.0. */
    public static final EscalaCalificacion POR_DEFECTO = new EscalaCalificacion(
            new BigDecimal("1.00"), new BigDecimal("5.00"), new BigDecimal("3.00"));

    public EscalaCalificacion {
        if (minima == null || maxima == null || aprobatoria == null) {
            throw new ReglaNegocioException("RF-59", "La escala de calificacion esta incompleta.");
        }
        if (minima.compareTo(maxima) >= 0) {
            throw new ReglaNegocioException("RF-59",
                    "La nota minima debe ser menor que la maxima.");
        }
        if (aprobatoria.compareTo(minima) <= 0 || aprobatoria.compareTo(maxima) > 0) {
            throw new ReglaNegocioException("RF-59",
                    "La nota aprobatoria debe estar dentro de la escala y por encima de la minima.");
        }
    }

    /** RB-03: indica si un valor cae dentro de la escala. */
    public boolean contiene(BigDecimal valor) {
        return valor != null && valor.compareTo(minima) >= 0 && valor.compareTo(maxima) <= 0;
    }

    /** RB-12 / RD-01: indica si un valor de esta escala es aprobatorio. */
    public boolean esAprobatoria(BigDecimal valor) {
        return valor != null && valor.compareTo(aprobatoria) >= 0;
    }
}
