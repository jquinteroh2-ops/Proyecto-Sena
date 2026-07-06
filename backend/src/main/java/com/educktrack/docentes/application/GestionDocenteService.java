package com.educktrack.docentes.application;

import com.educktrack.docentes.domain.Docente;
import com.educktrack.docentes.infrastructure.persistence.DocenteJpaEntity;
import com.educktrack.docentes.infrastructure.persistence.DocenteMapper;
import com.educktrack.docentes.infrastructure.persistence.DocenteRepository;
import com.educktrack.docentes.infrastructure.rest.DocenteDtos.ActualizarDocenteRequest;
import com.educktrack.docentes.infrastructure.rest.DocenteDtos.DocenteDto;
import com.educktrack.docentes.infrastructure.rest.DocenteDtos.RegistrarDocenteRequest;
import com.educktrack.shared.domain.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

/**
 * Casos de uso de gestion de docentes (RF-12, RF-13, RF-15).
 */
@Service
public class GestionDocenteService {

    private final DocenteRepository docenteRepository;

    public GestionDocenteService(DocenteRepository docenteRepository) {
        this.docenteRepository = docenteRepository;
    }

    /** RF-12 / HU-08: registra un docente (al menos un area la valida el dominio). */
    @Transactional
    public DocenteDto registrar(RegistrarDocenteRequest req) {
        if (docenteRepository.existsByDocumento(req.documento())) {
            throw new ReglaNegocioException("HU-08", "El documento de identidad ya esta registrado.");
        }
        Docente docente = Docente.registrar(req.documento(), req.nombres(), req.apellidos(),
                req.correo(), req.telefono(), req.areasFormacion());
        return toDto(docenteRepository.save(DocenteMapper.toEntity(docente)));
    }

    /** RF-13: actualiza los datos del docente. */
    @Transactional
    public DocenteDto actualizar(Long id, ActualizarDocenteRequest req) {
        DocenteJpaEntity e = obtener(id);
        e.setNombres(req.nombres());
        e.setApellidos(req.apellidos());
        e.setCorreo(req.correo());
        e.setTelefono(req.telefono());
        e.setAreasFormacion(new HashSet<>(req.areasFormacion()));
        return toDto(docenteRepository.save(e));
    }

    @Transactional(readOnly = true)
    public DocenteDto consultar(Long id) {
        return toDto(obtener(id));
    }

    @Transactional(readOnly = true)
    public List<DocenteDto> listar() {
        return docenteRepository.findAll().stream().map(GestionDocenteService::toDto).toList();
    }

    private DocenteJpaEntity obtener(Long id) {
        return docenteRepository.findById(id)
                .orElseThrow(() -> new ReglaNegocioException("RF-13", "El docente no existe."));
    }

    private static DocenteDto toDto(DocenteJpaEntity e) {
        return new DocenteDto(e.getId(), e.getDocumento(), e.getNombres(), e.getApellidos(),
                e.getCorreo(), e.getTelefono(), e.getAreasFormacion().stream().toList());
    }
}
