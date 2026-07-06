package com.educktrack.horarios.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio Spring Data de asignaciones horarias (RF-22..RF-25).
 */
public interface AsignacionHorariaRepository extends JpaRepository<AsignacionHorariaJpaEntity, Long> {

    /** RB-18: cruce del docente en el mismo bloque y periodo. */
    boolean existsByBloqueIdAndDocenteIdAndPeriodoAcademicoId(Long bloqueId, Long docenteId, Long periodoAcademicoId);

    /** RB-18: cruce del curso en el mismo bloque y periodo. */
    boolean existsByBloqueIdAndCursoIdAndPeriodoAcademicoId(Long bloqueId, Long cursoId, Long periodoAcademicoId);

    /** RF-24: horario del curso. */
    List<AsignacionHorariaJpaEntity> findByCursoIdAndPeriodoAcademicoId(Long cursoId, Long periodoAcademicoId);

    /** RF-25: horario del docente. */
    List<AsignacionHorariaJpaEntity> findByDocenteIdAndPeriodoAcademicoId(Long docenteId, Long periodoAcademicoId);
}
