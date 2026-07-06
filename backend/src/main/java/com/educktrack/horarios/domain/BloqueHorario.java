package com.educktrack.horarios.domain;

import com.educktrack.cursos.domain.Jornada;
import com.educktrack.shared.domain.ReglaNegocioException;

import java.time.LocalTime;

/**
 * Modelo de dominio de un bloque horario (RF-21). Valida que la hora de fin sea
 * posterior a la de inicio (HU-11) y esta asociado a una jornada (RD-04).
 */
public class BloqueHorario {

    private Long id;
    private DiaSemana dia;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private Jornada jornada;

    public BloqueHorario(Long id, DiaSemana dia, LocalTime horaInicio, LocalTime horaFin, Jornada jornada) {
        if (horaInicio == null || horaFin == null || !horaFin.isAfter(horaInicio)) {
            throw new ReglaNegocioException("HU-11",
                    "La hora de fin debe ser posterior a la hora de inicio.");
        }
        this.id = id;
        this.dia = dia;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.jornada = jornada;
    }

    public static BloqueHorario crear(DiaSemana dia, LocalTime horaInicio, LocalTime horaFin, Jornada jornada) {
        return new BloqueHorario(null, dia, horaInicio, horaFin, jornada);
    }

    /** Indica si este bloque se solapa en el tiempo con otro el mismo dia (RB-18). */
    public boolean seSolapaCon(BloqueHorario otro) {
        return this.dia == otro.dia
                && this.horaInicio.isBefore(otro.horaFin)
                && otro.horaInicio.isBefore(this.horaFin);
    }

    public Long getId() { return id; }
    public DiaSemana getDia() { return dia; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public Jornada getJornada() { return jornada; }
}
