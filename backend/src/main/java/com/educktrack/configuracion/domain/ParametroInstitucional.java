package com.educktrack.configuracion.domain;

import java.math.BigDecimal;

/**
 * Parametros institucionales configurables por el Rector (RF-59, RS-14).
 *
 * <p>Es un enum cerrado <strong>a proposito</strong>, igual que
 * {@code TipoOperacion}: los parametros son decisiones de negocio con un
 * consumidor concreto en el codigo, no un diccionario libre. Si cualquiera
 * pudiera inventar claves, la tabla acumularia filas que nadie lee y dejaria de
 * poder responderse "que se puede configurar aqui".</p>
 *
 * <p>Cada valor declara su tipo y su rango admisible, porque un parametro mal
 * puesto no falla al guardarlo sino mucho despues, calificando.</p>
 */
public enum ParametroInstitucional {

    /** RB-03: extremo inferior de la escala de calificacion. */
    NOTA_MINIMA(Tipo.DECIMAL),

    /** RB-03: extremo superior de la escala de calificacion. */
    NOTA_MAXIMA(Tipo.DECIMAL),

    /** RB-03 / RB-12 / RD-01: nota a partir de la cual se aprueba. */
    NOTA_APROBATORIA(Tipo.DECIMAL),

    /** RB-04: porcentaje minimo de asistencia para conservar derecho a evaluacion. */
    PORCENTAJE_MINIMO_ASISTENCIA(Tipo.PORCENTAJE),

    /** RB-09: maximo de horas semanales que se puede asignar a un docente. */
    MAX_HORAS_DOCENTE(Tipo.ENTERO_POSITIVO);

    /** Forma admisible del valor. Lo comprueba el servicio al actualizar. */
    public enum Tipo {
        /** Numero con decimales, mayor que cero. */
        DECIMAL,
        /** Entero de 0 a 100. */
        PORCENTAJE,
        /** Entero mayor que cero. */
        ENTERO_POSITIVO
    }

    private final Tipo tipo;

    ParametroInstitucional(Tipo tipo) {
        this.tipo = tipo;
    }

    public Tipo getTipo() {
        return tipo;
    }

    /** Valor con el que se siembra el parametro en V15; tambien es el de respaldo. */
    public String getValorPorDefecto() {
        return switch (this) {
            case NOTA_MINIMA -> "1.00";
            case NOTA_MAXIMA -> "5.00";
            case NOTA_APROBATORIA -> "3.00";
            case PORCENTAJE_MINIMO_ASISTENCIA -> "80";
            case MAX_HORAS_DOCENTE -> "30";
        };
    }

    public BigDecimal decimalPorDefecto() {
        return new BigDecimal(getValorPorDefecto());
    }
}
