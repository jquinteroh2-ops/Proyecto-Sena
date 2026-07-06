package com.educktrack.asistencia.domain;

import com.educktrack.shared.domain.ReglaNegocioException;

import java.time.LocalDateTime;

/**
 * Modelo de dominio de un registro de asistencia (RF-26).
 *
 * <p>Concentra la regla temporal RB-06: un registro no puede modificarse despues
 * de 48 horas de su creacion (sin autorizacion de coordinacion).</p>
 */
public class Asistencia {

    /** Ventana de modificacion permitida sin autorizacion (RB-06 / RF-29). */
    public static final int HORAS_EDICION = 48;

    private EstadoAsistencia estado;
    private boolean justificada;
    private String motivoJustificacion;
    private final LocalDateTime fechaRegistro;

    public Asistencia(EstadoAsistencia estado, boolean justificada, String motivoJustificacion,
                      LocalDateTime fechaRegistro) {
        this.estado = estado;
        this.justificada = justificada;
        this.motivoJustificacion = motivoJustificacion;
        this.fechaRegistro = fechaRegistro;
    }

    /** RB-06: indica si el registro aun puede modificarse (dentro de 48h). */
    public boolean puedeModificarse(LocalDateTime ahora) {
        return fechaRegistro.plusHours(HORAS_EDICION).isAfter(ahora);
    }

    /** RF-29 / RB-06: cambia el estado si esta dentro de la ventana de 48h. */
    public void cambiarEstado(EstadoAsistencia nuevo, LocalDateTime ahora) {
        if (!puedeModificarse(ahora)) {
            throw new ReglaNegocioException("RB-06",
                    "No se puede modificar la asistencia despues de 48 horas sin autorizacion de coordinacion.");
        }
        this.estado = nuevo;
    }

    /** RF-27 / HU-15: marca la inasistencia como justificada con su motivo. */
    public void justificar(String motivo) {
        this.justificada = true;
        this.motivoJustificacion = motivo;
    }

    public EstadoAsistencia getEstado() { return estado; }
    public boolean isJustificada() { return justificada; }
    public String getMotivoJustificacion() { return motivoJustificacion; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
}
