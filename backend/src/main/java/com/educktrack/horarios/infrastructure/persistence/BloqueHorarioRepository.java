package com.educktrack.horarios.infrastructure.persistence;

import com.educktrack.cursos.domain.Jornada;
import com.educktrack.horarios.domain.DiaSemana;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalTime;

/**
 * Repositorio Spring Data de bloques horarios (RF-21).
 */
public interface BloqueHorarioRepository extends JpaRepository<BloqueHorarioJpaEntity, Long> {

    boolean existsByDiaAndHoraInicioAndHoraFinAndJornada(
            DiaSemana dia, LocalTime horaInicio, LocalTime horaFin, Jornada jornada);
}
