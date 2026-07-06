package com.educktrack.tareas.domain;

import com.educktrack.shared.domain.ReglaNegocioException;

import java.time.LocalDate;

/**
 * Modelo de dominio de una tarea (RF-38). Valida que la fecha limite no sea
 * anterior a hoy (HU-23) y decide si una entrega esta dentro de plazo (RB-10).
 */
public class Tarea {

    private final LocalDate fechaLimite;
    private final boolean permiteEntregaTardia;

    public Tarea(LocalDate fechaLimite, boolean permiteEntregaTardia, LocalDate hoy) {
        if (fechaLimite == null || fechaLimite.isBefore(hoy)) {
            throw new ReglaNegocioException("HU-23",
                    "La fecha limite no puede ser anterior a la fecha actual.");
        }
        this.fechaLimite = fechaLimite;
        this.permiteEntregaTardia = permiteEntregaTardia;
    }

    /**
     * RB-10: una tarea solo puede entregarse dentro de la fecha limite, salvo
     * autorizacion expresa del docente (entrega tardia habilitada).
     */
    public boolean admiteEntrega(LocalDate fechaEntrega) {
        return permiteEntregaTardia || !fechaEntrega.isAfter(fechaLimite);
    }

    /** RB-10: valida la entrega y falla si esta fuera de plazo sin autorizacion. */
    public void validarEntrega(LocalDate fechaEntrega) {
        if (!admiteEntrega(fechaEntrega)) {
            throw new ReglaNegocioException("RB-10",
                    "La tarea esta fuera del plazo de entrega y no tiene autorizacion para entrega tardia.");
        }
    }

    public LocalDate getFechaLimite() { return fechaLimite; }
    public boolean isPermiteEntregaTardia() { return permiteEntregaTardia; }
}
