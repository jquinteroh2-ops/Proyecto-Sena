package com.educktrack.estudiantes.domain.evento;

/**
 * Hechos del modulo de estudiantes que otros modulos pueden querer atender
 * (RS-08).
 */
public final class EventosDeEstudiantes {

    private EventosDeEstudiantes() {
    }

    /**
     * RF-10 / HU-07: se retiro a un estudiante. HU-07 exige avisar al acudiente
     * vinculado, que puede no haber estado presente en el tramite.
     */
    public record EstudianteRetirado(
            Long estudianteId,
            String nombreCompleto,
            String motivo) {
    }
}
