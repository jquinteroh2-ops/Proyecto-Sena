package com.educktrack.materias.infrastructure.persistence;

import com.educktrack.materias.domain.Materia;

/**
 * Mapper dominio <-> entidad JPA para {@link Materia}.
 */
public final class MateriaMapper {

    private MateriaMapper() {
    }

    public static Materia toDomain(MateriaJpaEntity e) {
        return new Materia(e.getId(), e.getCodigo(), e.getNombre(), e.getArea(),
                e.getIntensidadHorariaSemanal());
    }

    public static MateriaJpaEntity toEntity(Materia d) {
        MateriaJpaEntity e = new MateriaJpaEntity();
        e.setId(d.getId());
        e.setCodigo(d.getCodigo());
        e.setNombre(d.getNombre());
        e.setArea(d.getArea());
        e.setIntensidadHorariaSemanal(d.getIntensidadHorariaSemanal());
        return e;
    }
}
