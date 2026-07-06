package com.educktrack.cursos.domain;

import com.educktrack.shared.domain.ReglaNegocioException;

/**
 * Modelo de dominio de un curso / grupo (RF-43).
 *
 * <p>Referencia por id al periodo academico y al docente director de grupo
 * (RB-02) para mantener bajo acoplamiento entre modulos. Aplica la capacidad
 * maxima de matricula (RB-17).</p>
 */
public class Curso {

    private Long id;
    private String nombre;
    private int grado;
    private NivelEducativo nivel;
    private Jornada jornada;
    private int cupoMaximo;
    private Long periodoAcademicoId;
    private Long directorGrupoId; // docente director (RB-02), opcional al crear

    public Curso(Long id, String nombre, int grado, NivelEducativo nivel, Jornada jornada,
                 int cupoMaximo, Long periodoAcademicoId, Long directorGrupoId) {
        if (cupoMaximo <= 0) {
            throw new ReglaNegocioException("RB-17", "El cupo maximo del curso debe ser mayor que cero.");
        }
        this.id = id;
        this.nombre = nombre;
        this.grado = grado;
        this.nivel = nivel;
        this.jornada = jornada;
        this.cupoMaximo = cupoMaximo;
        this.periodoAcademicoId = periodoAcademicoId;
        this.directorGrupoId = directorGrupoId;
    }

    public static Curso registrar(String nombre, int grado, NivelEducativo nivel, Jornada jornada,
                                  int cupoMaximo, Long periodoAcademicoId) {
        return new Curso(null, nombre, grado, nivel, jornada, cupoMaximo, periodoAcademicoId, null);
    }

    /**
     * RB-17 / RF-46: indica si el curso admite una matricula adicional dado el
     * numero de estudiantes ya matriculados.
     */
    public boolean tieneCupoDisponible(int matriculadosActuales) {
        return matriculadosActuales < cupoMaximo;
    }

    /** RB-17: valida y falla si se excede el cupo maximo. */
    public void validarCupo(int matriculadosActuales) {
        if (!tieneCupoDisponible(matriculadosActuales)) {
            throw new ReglaNegocioException("RB-17",
                    "El curso alcanzo su cupo maximo de " + cupoMaximo + " estudiantes.");
        }
    }

    /** RB-02: designa el docente director de grupo (director titular unico). */
    public void designarDirectorGrupo(Long docenteId) {
        this.directorGrupoId = docenteId;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public int getGrado() { return grado; }
    public NivelEducativo getNivel() { return nivel; }
    public Jornada getJornada() { return jornada; }
    public int getCupoMaximo() { return cupoMaximo; }
    public Long getPeriodoAcademicoId() { return periodoAcademicoId; }
    public Long getDirectorGrupoId() { return directorGrupoId; }
}
