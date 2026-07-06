package com.educktrack.asistencia.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio Spring Data de asistencia (RF-26..RF-30).
 */
public interface AsistenciaRepository extends JpaRepository<AsistenciaJpaEntity, Long> {

    /** RB-06: evita registro duplicado por estudiante/bloque/fecha. */
    boolean existsByEstudianteIdAndBloqueIdAndFecha(Long estudianteId, Long bloqueId, LocalDate fecha);

    /** RF-28: registros de un estudiante en una materia y periodo (para el %). */
    List<AsistenciaJpaEntity> findByEstudianteIdAndMateriaIdAndPeriodoAcademicoId(
            Long estudianteId, Long materiaId, Long periodoAcademicoId);

    /** RF-30: registros de un curso/materia/periodo (para detectar riesgo). */
    List<AsistenciaJpaEntity> findByCursoIdAndMateriaIdAndPeriodoAcademicoId(
            Long cursoId, Long materiaId, Long periodoAcademicoId);
}
