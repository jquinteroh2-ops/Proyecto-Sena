package com.educktrack.notas.domain;

import com.educktrack.shared.domain.ReglaNegocioException;

/**
 * Modelo de dominio de una calificacion (RF-31). Encapsula la escala
 * institucional RB-03: valor numerico entre 1.0 y 5.0, con 3.0 como nota minima
 * aprobatoria (RD-01).
 */
public class Calificacion {

    public static final double NOTA_MINIMA = 1.0;
    public static final double NOTA_MAXIMA = 5.0;
    public static final double NOTA_APROBATORIA = 3.0;

    private double valor;

    public Calificacion(double valor) {
        this.valor = validar(valor);
    }

    /** RB-03: la nota debe estar en la escala 1.0 a 5.0. */
    private static double validar(double valor) {
        if (valor < NOTA_MINIMA || valor > NOTA_MAXIMA) {
            throw new ReglaNegocioException("RB-03",
                    "La calificacion debe estar entre " + NOTA_MINIMA + " y " + NOTA_MAXIMA + ".");
        }
        return valor;
    }

    /** RB-12 / RD-01: indica si la nota es aprobatoria (>= 3.0). */
    public boolean esAprobatoria() {
        return valor >= NOTA_APROBATORIA;
    }

    /** RB-13: indica si debe notificarse bajo rendimiento (< 3.0). */
    public boolean esBajoRendimiento() {
        return valor < NOTA_APROBATORIA;
    }

    public double getValor() {
        return valor;
    }
}
