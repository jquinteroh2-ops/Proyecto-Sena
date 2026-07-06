package com.educktrack.estudiantes.infrastructure.persistence;

import com.educktrack.estudiantes.domain.Estudiante;

/**
 * Mapper dominio <-> entidad JPA para {@link Estudiante}.
 */
public final class EstudianteMapper {

    private EstudianteMapper() {
    }

    public static Estudiante toDomain(EstudianteJpaEntity e) {
        return new Estudiante(
                e.getId(), e.getDocumento(), e.getNombres(), e.getApellidos(),
                e.getFechaNacimiento(), e.getDireccion(), e.getEstado(),
                e.getAcudienteNombre(), e.getAcudienteTelefono(), e.getAcudienteParentesco());
    }

    public static EstudianteJpaEntity toEntity(Estudiante d) {
        EstudianteJpaEntity e = new EstudianteJpaEntity();
        e.setId(d.getId());
        e.setDocumento(d.getDocumento());
        e.setNombres(d.getNombres());
        e.setApellidos(d.getApellidos());
        e.setFechaNacimiento(d.getFechaNacimiento());
        e.setDireccion(d.getDireccion());
        e.setEstado(d.getEstado());
        e.setAcudienteNombre(d.getAcudienteNombre());
        e.setAcudienteTelefono(d.getAcudienteTelefono());
        e.setAcudienteParentesco(d.getAcudienteParentesco());
        return e;
    }
}
