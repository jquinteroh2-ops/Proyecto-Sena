package com.educktrack.notas.domain;

/**
 * Tipos de evaluacion institucionales (RD-06). Cada uno tiene una ponderacion
 * propia que debe sumar 100% por materia (RB-07).
 */
public enum TipoEvaluacion {
    QUIZ,
    EXAMEN,
    TALLER,
    PROYECTO,
    AUTOEVALUACION
}
