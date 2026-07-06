package com.educktrack.docentes.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio Spring Data de asignaciones academicas (RF-14, RF-15).
 */
public interface AsignacionDocenteRepository extends JpaRepository<AsignacionDocenteJpaEntity, Long> {

    List<AsignacionDocenteJpaEntity> findByDocenteIdAndPeriodoAcademicoId(Long docenteId, Long periodoAcademicoId);

    List<AsignacionDocenteJpaEntity> findByCursoId(Long cursoId);

    boolean existsByDocenteIdAndMateriaIdAndCursoIdAndPeriodoAcademicoId(
            Long docenteId, Long materiaId, Long cursoId, Long periodoAcademicoId);
}
