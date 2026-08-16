package com.educktrack.asistencia.domain.evento;

/**
 * Hechos del modulo de asistencia que otros modulos pueden querer atender
 * (RS-08).
 */
public final class EventosDeAsistencia {

    private EventosDeAsistencia() {
    }

    /**
     * RF-30 / RB-04: al registrar asistencia, un estudiante quedo por debajo
     * del porcentaje minimo exigido para conservar el derecho a evaluacion.
     *
     * <p>Se publica en el momento en que se cruza el umbral hacia abajo, no en
     * cada registro posterior: avisar todos los dias de lo mismo convierte la
     * alerta en ruido y deja de leerse, que es la forma mas eficaz de que una
     * alerta no sirva para nada.</p>
     */
    public record AsistenciaBajoMinimo(
            Long estudianteId,
            Long materiaId,
            Long periodoAcademicoId,
            double porcentaje) {
    }
}
