package com.educktrack.notas.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio Spring Data de calificaciones (RF-31..RF-37).
 */
public interface CalificacionRepository extends JpaRepository<CalificacionJpaEntity, Long> {

    /** RF-33: notas de un estudiante en una materia y periodo (para el promedio). */
    List<CalificacionJpaEntity> findByEstudianteIdAndMateriaIdAndPeriodoAcademicoId(
            Long estudianteId, Long materiaId, Long periodoAcademicoId);

    /** RF-35: notas de un estudiante en un periodo (para el boletin). */
    List<CalificacionJpaEntity> findByEstudianteIdAndPeriodoAcademicoId(Long estudianteId, Long periodoAcademicoId);

    /** RF-37: historico completo de un estudiante. */
    List<CalificacionJpaEntity> findByEstudianteId(Long estudianteId);
}
