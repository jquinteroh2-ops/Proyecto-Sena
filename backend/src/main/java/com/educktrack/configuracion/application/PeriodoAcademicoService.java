package com.educktrack.configuracion.application;

import com.educktrack.configuracion.domain.PeriodoAcademico;
import com.educktrack.configuracion.infrastructure.persistence.PeriodoAcademicoJpaEntity;
import com.educktrack.configuracion.infrastructure.persistence.PeriodoAcademicoMapper;
import com.educktrack.configuracion.infrastructure.persistence.PeriodoAcademicoRepository;
import com.educktrack.configuracion.infrastructure.rest.PeriodoDtos.CrearPeriodoRequest;
import com.educktrack.configuracion.infrastructure.rest.PeriodoDtos.PeriodoDto;
import com.educktrack.shared.domain.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Casos de uso de periodo academico (RF-57, RD-02). Aplica RB-05 (un solo
 * periodo activo por ano lectivo). Version base de la Fase 3.
 */
@Service
public class PeriodoAcademicoService {

    private final PeriodoAcademicoRepository periodoRepository;

    public PeriodoAcademicoService(PeriodoAcademicoRepository periodoRepository) {
        this.periodoRepository = periodoRepository;
    }

    /** RF-57: crea un periodo academico (inactivo hasta activarlo). */
    @Transactional
    public PeriodoDto crear(CrearPeriodoRequest req) {
        // La validacion de fechas (fin >= inicio) la realiza el dominio (RD-02).
        PeriodoAcademico periodo = PeriodoAcademico.crear(
                req.nombre(), req.anioLectivo(), req.fechaInicio(), req.fechaFin());
        return toDto(periodoRepository.save(PeriodoAcademicoMapper.toEntity(periodo)));
    }

    /** RF-57 / RB-05: activa un periodo, garantizando uno solo activo por ano. */
    @Transactional
    public PeriodoDto activar(Long id) {
        PeriodoAcademicoJpaEntity periodo = periodoRepository.findById(id)
                .orElseThrow(() -> new ReglaNegocioException("RF-57", "El periodo no existe."));
        if (periodo.isCerrado()) {
            throw new ReglaNegocioException("RB-05", "No se puede activar un periodo cerrado.");
        }
        if (periodoRepository.existsByAnioLectivoAndActivoTrue(periodo.getAnioLectivo())
                && !periodo.isActivo()) {
            throw new ReglaNegocioException("RB-05",
                    "Ya existe un periodo activo para el ano lectivo " + periodo.getAnioLectivo() + ".");
        }
        periodo.setActivo(true);
        return toDto(periodoRepository.save(periodo));
    }

    @Transactional(readOnly = true)
    public List<PeriodoDto> listar() {
        return periodoRepository.findAll().stream().map(PeriodoAcademicoService::toDto).toList();
    }

    private static PeriodoDto toDto(PeriodoAcademicoJpaEntity e) {
        return new PeriodoDto(e.getId(), e.getNombre(), e.getAnioLectivo(),
                e.getFechaInicio(), e.getFechaFin(), e.isActivo(), e.isCerrado());
    }
}
