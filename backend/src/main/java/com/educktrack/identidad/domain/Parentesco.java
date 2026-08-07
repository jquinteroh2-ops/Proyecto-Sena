package com.educktrack.identidad.domain;

/**
 * Tipo de vinculo familiar entre un acudiente y un estudiante (RD-08).
 *
 * <p>La normativa reconoce la relacion de padre, madre o acudiente autorizado.
 * El vinculo formal es el que habilita la visibilidad de la informacion del
 * estudiante para la cuenta del acudiente (RB-08).</p>
 */
public enum Parentesco {
    PADRE,
    MADRE,
    ACUDIENTE_AUTORIZADO
}
