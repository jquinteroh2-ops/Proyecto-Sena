package com.educktrack.auditoria.application;

import com.educktrack.auditoria.domain.TipoOperacion;
import com.educktrack.auditoria.infrastructure.persistence.AuditoriaJpaEntity;
import com.educktrack.auditoria.infrastructure.persistence.AuditoriaRepository;
import com.educktrack.auditoria.infrastructure.rest.AuditoriaDtos.PaginaAuditoriaDto;
import com.educktrack.auditoria.infrastructure.rest.AuditoriaDtos.RegistroAuditoriaDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;

/**
 * Consulta del log de auditoria (RF-05, RF-63).
 *
 * <p>Separado de {@link AuditoriaService} a proposito: escribir el log ocurre
 * dentro de las operaciones de negocio y no debe arrastrar la maquinaria de
 * paginacion ni el riesgo de que alguien lea el log desde un servicio de
 * dominio. Aqui solo se lee.</p>
 *
 * <p>El control de acceso vive en el controlador y es de rol puro: el log de
 * auditoria es competencia exclusiva de Administracion (RF-05), de modo que no
 * hay alcance por dato que resolver como en la Fase 2.</p>
 */
@Service
public class ConsultaAuditoriaService {

    /** Tope de pagina: evita que un tamano enorme convierta la consulta en un volcado. */
    private static final int TAMANO_MAXIMO = 200;
    private static final int TAMANO_POR_DEFECTO = 50;

    private static final Set<TipoOperacion> OPERACIONES_DE_ACCESO =
            EnumSet.of(TipoOperacion.ACCESO_EXITOSO, TipoOperacion.ACCESO_FALLIDO);

    private final AuditoriaRepository repository;

    public ConsultaAuditoriaService(AuditoriaRepository repository) {
        this.repository = repository;
    }

    /**
     * RF-63: log completo, opcionalmente acotado por usuario o por tipo de
     * operacion. Siempre en orden cronologico inverso: lo que se audita casi
     * siempre es "que ha pasado ultimamente".
     */
    @Transactional(readOnly = true)
    public PaginaAuditoriaDto consultar(String usuario, TipoOperacion operacion, int pagina, int tamano) {
        Pageable pageable = paginaDe(pagina, tamano);
        Page<AuditoriaJpaEntity> resultado;

        if (usuario != null && !usuario.isBlank()) {
            resultado = repository.findByUsuarioOrderByFechaDesc(usuario, pageable);
        } else if (operacion != null) {
            resultado = repository.findByOperacionOrderByFechaDesc(operacion, pageable);
        } else {
            resultado = repository.findByOrderByFechaDesc(pageable);
        }
        return toPagina(resultado);
    }

    /**
     * RF-05: historial de inicios de sesion de una cuenta, exitosos y fallidos.
     * Los fallidos entran porque una racha sobre la misma cuenta es justo lo
     * que este historial debe dejar ver.
     */
    @Transactional(readOnly = true)
    public PaginaAuditoriaDto historialDeAccesos(String usuario, int pagina, int tamano) {
        return toPagina(repository.findByUsuarioAndOperacionInOrderByFechaDesc(
                usuario, OPERACIONES_DE_ACCESO, paginaDe(pagina, tamano)));
    }

    private static Pageable paginaDe(int pagina, int tamano) {
        int p = Math.max(0, pagina);
        int t = tamano <= 0 ? TAMANO_POR_DEFECTO : Math.min(tamano, TAMANO_MAXIMO);
        return PageRequest.of(p, t);
    }

    private static PaginaAuditoriaDto toPagina(Page<AuditoriaJpaEntity> page) {
        return new PaginaAuditoriaDto(
                page.getContent().stream().map(ConsultaAuditoriaService::toDto).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    private static RegistroAuditoriaDto toDto(AuditoriaJpaEntity e) {
        return new RegistroAuditoriaDto(e.getId(), e.getUsuario(), e.getOperacion(),
                e.getEntidad(), e.getEntidadId(), e.getDescripcion(), e.getFecha());
    }
}
