package com.educktrack.cursos.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio Spring Data de cursos (RF-43..RF-46).
 */
public interface CursoRepository extends JpaRepository<CursoJpaEntity, Long> {

    List<CursoJpaEntity> findByPeriodoAcademicoId(Long periodoAcademicoId);
}
