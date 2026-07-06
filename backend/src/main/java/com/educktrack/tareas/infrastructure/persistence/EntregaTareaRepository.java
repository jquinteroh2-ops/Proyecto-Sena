package com.educktrack.tareas.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data de entregas de tareas (RF-39..RF-41).
 */
public interface EntregaTareaRepository extends JpaRepository<EntregaTareaJpaEntity, Long> {

    Optional<EntregaTareaJpaEntity> findByTareaIdAndEstudianteId(Long tareaId, Long estudianteId);

    boolean existsByTareaIdAndEstudianteId(Long tareaId, Long estudianteId);

    /** RF-41: entregas de un estudiante (para su estado de tareas). */
    List<EntregaTareaJpaEntity> findByEstudianteId(Long estudianteId);
}
