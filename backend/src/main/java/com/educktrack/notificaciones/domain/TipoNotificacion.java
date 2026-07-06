package com.educktrack.notificaciones.domain;

/**
 * Tipos de evento que generan notificaciones (RS-08). Las criticas se resaltan
 * en la bandeja (HU-27): bajo rendimiento (RB-13) y tareas por vencer.
 */
public enum TipoNotificacion {
    GENERAL,
    BAJO_RENDIMIENTO,
    BAJA_ASISTENCIA,
    TAREA_POR_VENCER,
    BOLETIN_DISPONIBLE,
    CIERRE_PERIODO;

    /** HU-27: eventos criticos que se resaltan visualmente. */
    public boolean esCritica() {
        return this == BAJO_RENDIMIENTO || this == BAJA_ASISTENCIA || this == TAREA_POR_VENCER;
    }
}
