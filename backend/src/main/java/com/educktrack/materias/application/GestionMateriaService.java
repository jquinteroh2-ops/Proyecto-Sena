package com.educktrack.materias.application;

import com.educktrack.materias.domain.Materia;
import com.educktrack.materias.infrastructure.persistence.MateriaJpaEntity;
import com.educktrack.materias.infrastructure.persistence.MateriaMapper;
import com.educktrack.materias.infrastructure.persistence.MateriaRepository;
import com.educktrack.materias.infrastructure.rest.MateriaDtos.GuardarMateriaRequest;
import com.educktrack.materias.infrastructure.rest.MateriaDtos.MateriaDto;
import com.educktrack.shared.domain.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Casos de uso de gestion de materias (RF-17, RF-18).
 */
@Service
public class GestionMateriaService {

    private final MateriaRepository materiaRepository;

    public GestionMateriaService(MateriaRepository materiaRepository) {
        this.materiaRepository = materiaRepository;
    }

    /** RF-17: registra una materia con codigo unico. */
    @Transactional
    public MateriaDto registrar(GuardarMateriaRequest req) {
        if (materiaRepository.existsByCodigo(req.codigo())) {
            throw new ReglaNegocioException("RF-17", "Ya existe una materia con ese codigo.");
        }
        Materia materia = Materia.registrar(req.codigo(), req.nombre(), req.area(),
                req.intensidadHorariaSemanal());
        return toDto(materiaRepository.save(MateriaMapper.toEntity(materia)));
    }

    /** RF-18: edita una materia existente. */
    @Transactional
    public MateriaDto actualizar(Long id, GuardarMateriaRequest req) {
        MateriaJpaEntity e = obtener(id);
        if (!e.getCodigo().equals(req.codigo()) && materiaRepository.existsByCodigo(req.codigo())) {
            throw new ReglaNegocioException("RF-18", "Ya existe otra materia con ese codigo.");
        }
        e.setCodigo(req.codigo());
        e.setNombre(req.nombre());
        e.setArea(req.area());
        e.setIntensidadHorariaSemanal(req.intensidadHorariaSemanal());
        return toDto(materiaRepository.save(e));
    }

    @Transactional(readOnly = true)
    public List<MateriaDto> listar() {
        return materiaRepository.findAll().stream().map(GestionMateriaService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public MateriaDto consultar(Long id) {
        return toDto(obtener(id));
    }

    private MateriaJpaEntity obtener(Long id) {
        return materiaRepository.findById(id)
                .orElseThrow(() -> new ReglaNegocioException("RF-18", "La materia no existe."));
    }

    private static MateriaDto toDto(MateriaJpaEntity e) {
        return new MateriaDto(e.getId(), e.getCodigo(), e.getNombre(), e.getArea(),
                e.getIntensidadHorariaSemanal());
    }
}
