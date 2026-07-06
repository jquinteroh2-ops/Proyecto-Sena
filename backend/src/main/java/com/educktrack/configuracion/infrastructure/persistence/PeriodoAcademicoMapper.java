package com.educktrack.configuracion.infrastructure.persistence;

import com.educktrack.configuracion.domain.PeriodoAcademico;

/**
 * Mapper dominio <-> entidad JPA para {@link PeriodoAcademico}.
 */
public final class PeriodoAcademicoMapper {

    private PeriodoAcademicoMapper() {
    }

    public static PeriodoAcademico toDomain(PeriodoAcademicoJpaEntity e) {
        return new PeriodoAcademico(e.getId(), e.getNombre(), e.getAnioLectivo(),
                e.getFechaInicio(), e.getFechaFin(), e.isActivo(), e.isCerrado());
    }

    public static PeriodoAcademicoJpaEntity toEntity(PeriodoAcademico d) {
        PeriodoAcademicoJpaEntity e = new PeriodoAcademicoJpaEntity();
        e.setId(d.getId());
        e.setNombre(d.getNombre());
        e.setAnioLectivo(d.getAnioLectivo());
        e.setFechaInicio(d.getFechaInicio());
        e.setFechaFin(d.getFechaFin());
        e.setActivo(d.isActivo());
        e.setCerrado(d.isCerrado());
        return e;
    }
}
