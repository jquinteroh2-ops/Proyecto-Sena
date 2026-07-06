package com.educktrack.cursos.infrastructure.rest;

import com.educktrack.cursos.domain.Jornada;
import com.educktrack.cursos.domain.NivelEducativo;
import com.educktrack.estudiantes.domain.EstadoMatricula;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTOs del modulo de cursos (RF-43..RF-46).
 */
public final class CursoDtos {

    private CursoDtos() {
    }

    public record GuardarCursoRequest(
            @NotBlank(message = "El nombre es obligatorio") String nombre,
            @Positive(message = "El grado debe ser positivo") int grado,
            @NotNull(message = "El nivel es obligatorio") NivelEducativo nivel,
            @NotNull(message = "La jornada es obligatoria") Jornada jornada,
            @Positive(message = "El cupo maximo debe ser mayor que cero") int cupoMaximo,
            @NotNull(message = "El periodo academico es obligatorio") Long periodoAcademicoId) {
    }

    public record CursoDto(
            Long id,
            String nombre,
            int grado,
            NivelEducativo nivel,
            Jornada jornada,
            int cupoMaximo,
            Long periodoAcademicoId,
            Long directorGrupoId) {
    }

    /** RF-46: disponibilidad de cupo. */
    public record CupoDto(int cupoMaximo, long matriculados, long disponibles) {
    }

    /** RF-45: estudiante dentro del listado de un curso. */
    public record EstudianteEnCursoDto(
            Long estudianteId,
            String documento,
            String nombres,
            String apellidos,
            EstadoMatricula estado) {
    }
}
