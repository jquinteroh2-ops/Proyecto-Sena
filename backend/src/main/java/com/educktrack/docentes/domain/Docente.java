package com.educktrack.docentes.domain;

import com.educktrack.shared.domain.ReglaNegocioException;

import java.util.HashSet;
import java.util.Set;

/**
 * Modelo de dominio de un docente (RF-12). Pertenece a una o mas areas de
 * formacion (RD-09), que determinan las materias que puede dictar (RB-16).
 */
public class Docente {

    private Long id;
    private String documento;
    private String nombres;
    private String apellidos;
    private String correo;
    private String telefono;
    private final Set<String> areasFormacion;

    public Docente(Long id, String documento, String nombres, String apellidos,
                   String correo, String telefono, Set<String> areasFormacion) {
        this.id = id;
        this.documento = documento;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.correo = correo;
        this.telefono = telefono;
        this.areasFormacion = (areasFormacion == null) ? new HashSet<>() : new HashSet<>(areasFormacion);
        validarAreas();
    }

    /** Registra un nuevo docente; exige al menos un area de formacion (HU-08). */
    public static Docente registrar(String documento, String nombres, String apellidos,
                                    String correo, String telefono, Set<String> areasFormacion) {
        return new Docente(null, documento, nombres, apellidos, correo, telefono, areasFormacion);
    }

    private void validarAreas() {
        if (areasFormacion.isEmpty()) {
            throw new ReglaNegocioException("HU-08",
                    "El docente debe tener al menos un area de formacion.");
        }
    }

    /** RB-16: indica si el docente puede dictar materias de un area dada. */
    public boolean puedeDictarArea(String area) {
        return areasFormacion.contains(area);
    }

    public Long getId() { return id; }
    public String getDocumento() { return documento; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public String getCorreo() { return correo; }
    public String getTelefono() { return telefono; }
    public Set<String> getAreasFormacion() { return new HashSet<>(areasFormacion); }
}
