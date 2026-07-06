package com.educktrack.notas.application;

import com.educktrack.notas.infrastructure.persistence.PonderacionEvaluacionJpaEntity;
import com.educktrack.notas.infrastructure.persistence.PonderacionRepository;
import com.educktrack.notas.infrastructure.rest.NotaDtos.ConfigurarPonderacionRequest;
import com.educktrack.notas.infrastructure.rest.NotaDtos.PonderacionDto;
import com.educktrack.notas.infrastructure.rest.NotaDtos.PonderacionItem;
import com.educktrack.shared.domain.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Caso de uso de configuracion de ponderaciones (RF-20). Aplica RB-07: los
 * porcentajes de los tipos de evaluacion de una materia deben sumar 100%.
 */
@Service
public class PonderacionService {

    private final PonderacionRepository ponderacionRepository;

    public PonderacionService(PonderacionRepository ponderacionRepository) {
        this.ponderacionRepository = ponderacionRepository;
    }

    /** RF-20 / RB-07: define las ponderaciones de una materia/periodo (suman 100%). */
    @Transactional
    public List<PonderacionDto> configurar(ConfigurarPonderacionRequest req) {
        int suma = req.ponderaciones().stream().mapToInt(PonderacionItem::porcentaje).sum();
        if (suma != 100) {
            throw new ReglaNegocioException("RB-07",
                    "Las ponderaciones deben sumar 100%. Suma actual: " + suma + "%.");
        }
        boolean hayNegativos = req.ponderaciones().stream().anyMatch(p -> p.porcentaje() < 0);
        if (hayNegativos) {
            throw new ReglaNegocioException("RB-07", "Los porcentajes no pueden ser negativos.");
        }

        // Reemplaza la configuracion previa de la materia/periodo.
        ponderacionRepository.deleteByMateriaIdAndPeriodoAcademicoId(req.materiaId(), req.periodoAcademicoId());
        for (PonderacionItem item : req.ponderaciones()) {
            PonderacionEvaluacionJpaEntity e = new PonderacionEvaluacionJpaEntity();
            e.setMateriaId(req.materiaId());
            e.setPeriodoAcademicoId(req.periodoAcademicoId());
            e.setTipo(item.tipo());
            e.setPorcentaje(item.porcentaje());
            ponderacionRepository.save(e);
        }
        return listar(req.materiaId(), req.periodoAcademicoId());
    }

    @Transactional(readOnly = true)
    public List<PonderacionDto> listar(Long materiaId, Long periodoAcademicoId) {
        return ponderacionRepository.findByMateriaIdAndPeriodoAcademicoId(materiaId, periodoAcademicoId)
                .stream().map(p -> new PonderacionDto(p.getTipo(), p.getPorcentaje())).toList();
    }
}
