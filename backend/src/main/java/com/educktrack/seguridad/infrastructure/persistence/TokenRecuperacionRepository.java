package com.educktrack.seguridad.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repositorio Spring Data de los enlaces de recuperacion (RF-64, HU-04).
 */
public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacionJpaEntity, Long> {

    /** La busqueda al restablecer es siempre por el hash del token recibido. */
    Optional<TokenRecuperacionJpaEntity> findByTokenHash(String tokenHash);

    /**
     * Invalida los enlaces sin usar de una cuenta marcandolos como consumidos.
     *
     * <p>Se hace al emitir uno nuevo, para que nunca haya dos enlaces vivos a la
     * vez: si pedir otro enlace no anulara el anterior, un correo antiguo
     * reenviado o filtrado seguiria abriendo la cuenta.</p>
     */
    @Modifying
    @Query("update TokenRecuperacionJpaEntity t set t.fechaUso = :momento "
            + "where t.usuarioId = :usuarioId and t.fechaUso is null")
    int invalidarPendientes(Long usuarioId, LocalDateTime momento);
}
