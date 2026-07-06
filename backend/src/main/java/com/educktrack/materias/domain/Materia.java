package com.educktrack.materias.domain;

/**
 * Modelo de dominio de una materia (RF-17). Pertenece a un area de conocimiento,
 * que restringe que docente puede dictarla (RB-16), y tiene una intensidad
 * horaria semanal.
 */
public class Materia {

    private Long id;
    private String codigo;
    private String nombre;
    private String area;
    private int intensidadHorariaSemanal;

    public Materia(Long id, String codigo, String nombre, String area, int intensidadHorariaSemanal) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.area = area;
        this.intensidadHorariaSemanal = intensidadHorariaSemanal;
    }

    public static Materia registrar(String codigo, String nombre, String area, int intensidadHorariaSemanal) {
        return new Materia(null, codigo, nombre, area, intensidadHorariaSemanal);
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getArea() { return area; }
    public int getIntensidadHorariaSemanal() { return intensidadHorariaSemanal; }
}
