package com.educktrack.auditoria.infrastructure.persistence;

import com.educktrack.auditoria.domain.TipoOperacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

/**
 * Repositorio del log de auditoria (RS-07, RF-05).
 *
 * <p>Todas las consultas son paginadas: el log crece sin limite por diseno, y
 * devolverlo entero seria una forma segura de tumbar la aplicacion con el
 * tiempo.</p>
 */
public interface AuditoriaRepository extends JpaRepository<AuditoriaJpaEntity, Long> {

    Page<AuditoriaJpaEntity> findByOrderByFechaDesc(Pageable pageable);

    /** RF-05: historial de un usuario concreto. */
    Page<AuditoriaJpaEntity> findByUsuarioOrderByFechaDesc(String usuario, Pageable pageable);

    /** RS-07: log filtrado por tipo de operacion (p. ej. solo cambios de nota). */
    Page<AuditoriaJpaEntity> findByOperacionOrderByFechaDesc(TipoOperacion operacion, Pageable pageable);

    /** RF-05: historial de inicios de sesion de un usuario. */
    Page<AuditoriaJpaEntity> findByUsuarioAndOperacionInOrderByFechaDesc(
            String usuario, Collection<TipoOperacion> operaciones, Pageable pageable);
}
