package com.educktrack.auditoria.infrastructure.rest;

import com.educktrack.auditoria.domain.TipoOperacion;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs del log de auditoria (RS-07, RF-05, RF-63).
 */
public final class AuditoriaDtos {

    private AuditoriaDtos() {
    }

    /** Una entrada del log: quien, que, sobre que y cuando (RS-07). */
    public record RegistroAuditoriaDto(
            Long id,
            String usuario,
            TipoOperacion operacion,
            String entidad,
            Long entidadId,
            String descripcion,
            LocalDateTime fecha) {
    }

    /**
     * Pagina de resultados. El log no se devuelve entero nunca: crece sin
     * limite por diseno.
     */
    public record PaginaAuditoriaDto(
            List<RegistroAuditoriaDto> registros,
            int pagina,
            int tamano,
            long totalRegistros,
            int totalPaginas) {
    }
}
