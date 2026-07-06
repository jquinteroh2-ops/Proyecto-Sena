package com.educktrack.docentes.infrastructure.persistence;

import com.educktrack.docentes.domain.Docente;

/**
 * Mapper dominio <-> entidad JPA para {@link Docente}.
 */
public final class DocenteMapper {

    private DocenteMapper() {
    }

    public static Docente toDomain(DocenteJpaEntity e) {
        return new Docente(e.getId(), e.getDocumento(), e.getNombres(), e.getApellidos(),
                e.getCorreo(), e.getTelefono(), e.getAreasFormacion());
    }

    public static DocenteJpaEntity toEntity(Docente d) {
        DocenteJpaEntity e = new DocenteJpaEntity();
        e.setId(d.getId());
        e.setDocumento(d.getDocumento());
        e.setNombres(d.getNombres());
        e.setApellidos(d.getApellidos());
        e.setCorreo(d.getCorreo());
        e.setTelefono(d.getTelefono());
        e.getAreasFormacion().addAll(d.getAreasFormacion());
        return e;
    }
}
