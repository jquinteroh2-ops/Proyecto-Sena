package com.educktrack.identidad.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio Spring Data del vinculo acudiente-estudiante (RF-11). Soporta la
 * comprobacion de RB-08 en cada peticion de un usuario con rol PADRE_FAMILIA.
 */
public interface VinculoAcudienteRepository extends JpaRepository<VinculoAcudienteJpaEntity, Long> {

    /** RB-08: estudiantes formalmente vinculados a la cuenta del acudiente. */
    List<VinculoAcudienteJpaEntity> findByUsuarioId(Long usuarioId);

    /** Acudientes vinculados a un estudiante (consulta de coordinacion, RF-11). */
    List<VinculoAcudienteJpaEntity> findByEstudianteId(Long estudianteId);

    /** RB-08: comprobacion directa de visibilidad sin cargar la lista completa. */
    boolean existsByUsuarioIdAndEstudianteId(Long usuarioId, Long estudianteId);
}
