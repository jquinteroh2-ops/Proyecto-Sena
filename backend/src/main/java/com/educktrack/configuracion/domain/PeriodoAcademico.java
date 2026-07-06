package com.educktrack.configuracion.domain;

import com.educktrack.shared.domain.ReglaNegocioException;

import java.time.LocalDate;

/**
 * Modelo de dominio de un periodo academico / corte (RF-57, RD-02).
 *
 * <p>Solo puede existir un periodo activo por ano lectivo (RB-05); esa unicidad
 * se valida en la capa de aplicacion contra el repositorio. El cierre del corte
 * (RB-19) bloquea modificaciones de notas y habilita boletines.</p>
 */
public class PeriodoAcademico {

    private Long id;
    private String nombre;
    private int anioLectivo;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private boolean activo;
    private boolean cerrado;

    public PeriodoAcademico(Long id, String nombre, int anioLectivo, LocalDate fechaInicio,
                            LocalDate fechaFin, boolean activo, boolean cerrado) {
        if (fechaFin != null && fechaInicio != null && fechaFin.isBefore(fechaInicio)) {
            throw new ReglaNegocioException("RD-02",
                    "La fecha de cierre del periodo no puede ser anterior a la de inicio.");
        }
        this.id = id;
        this.nombre = nombre;
        this.anioLectivo = anioLectivo;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.activo = activo;
        this.cerrado = cerrado;
    }

    public static PeriodoAcademico crear(String nombre, int anioLectivo, LocalDate fechaInicio,
                                         LocalDate fechaFin) {
        return new PeriodoAcademico(null, nombre, anioLectivo, fechaInicio, fechaFin, false, false);
    }

    /** RB-19: cierra el corte, bloqueando modificaciones posteriores de notas. */
    public void cerrar() {
        if (cerrado) {
            throw new ReglaNegocioException("RB-19", "El periodo academico ya se encuentra cerrado.");
        }
        this.cerrado = true;
        this.activo = false;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public int getAnioLectivo() { return anioLectivo; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public boolean isActivo() { return activo; }
    public boolean isCerrado() { return cerrado; }
}
