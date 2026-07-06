package com.educktrack.cursos.infrastructure.persistence;

import com.educktrack.cursos.domain.Curso;

/**
 * Mapper dominio <-> entidad JPA para {@link Curso}.
 */
public final class CursoMapper {

    private CursoMapper() {
    }

    public static Curso toDomain(CursoJpaEntity e) {
        return new Curso(e.getId(), e.getNombre(), e.getGrado(), e.getNivel(), e.getJornada(),
                e.getCupoMaximo(), e.getPeriodoAcademicoId(), e.getDirectorGrupoId());
    }

    public static CursoJpaEntity toEntity(Curso d) {
        CursoJpaEntity e = new CursoJpaEntity();
        e.setId(d.getId());
        e.setNombre(d.getNombre());
        e.setGrado(d.getGrado());
        e.setNivel(d.getNivel());
        e.setJornada(d.getJornada());
        e.setCupoMaximo(d.getCupoMaximo());
        e.setPeriodoAcademicoId(d.getPeriodoAcademicoId());
        e.setDirectorGrupoId(d.getDirectorGrupoId());
        return e;
    }
}
