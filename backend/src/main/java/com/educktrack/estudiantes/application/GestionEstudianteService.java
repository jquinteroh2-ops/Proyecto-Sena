package com.educktrack.estudiantes.application;

import com.educktrack.estudiantes.domain.Estudiante;
import com.educktrack.estudiantes.infrastructure.persistence.EstudianteJpaEntity;
import com.educktrack.estudiantes.infrastructure.persistence.EstudianteMapper;
import com.educktrack.estudiantes.infrastructure.persistence.EstudianteRepository;
import com.educktrack.estudiantes.infrastructure.rest.EstudianteDtos.ActualizarEstudianteRequest;
import com.educktrack.estudiantes.infrastructure.rest.EstudianteDtos.EstudianteDto;
import com.educktrack.estudiantes.infrastructure.rest.EstudianteDtos.RegistrarEstudianteRequest;
import com.educktrack.estudiantes.infrastructure.rest.EstudianteDtos.RetirarEstudianteRequest;
import com.educktrack.shared.domain.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Casos de uso de gestion de estudiantes (RF-06, RF-07, RF-08, RF-10).
 */
@Service
public class GestionEstudianteService {

    private final EstudianteRepository estudianteRepository;

    public GestionEstudianteService(EstudianteRepository estudianteRepository) {
        this.estudianteRepository = estudianteRepository;
    }

    /** RF-06 / HU-05: registra un estudiante en estado ACTIVO. */
    @Transactional
    public EstudianteDto registrar(RegistrarEstudianteRequest req) {
        if (estudianteRepository.existsByDocumento(req.documento())) {
            throw new ReglaNegocioException("HU-05", "El documento de identidad ya esta registrado.");
        }
        if (esVacio(req.acudienteNombre()) && esVacio(req.acudienteTelefono())) {
            throw new ReglaNegocioException("HU-05",
                    "Debe registrar al menos un dato de contacto del acudiente.");
        }
        Estudiante estudiante = Estudiante.registrar(req.documento(), req.nombres(), req.apellidos(),
                req.fechaNacimiento(), req.direccion(), req.acudienteNombre(),
                req.acudienteTelefono(), req.acudienteParentesco());
        EstudianteJpaEntity guardado = estudianteRepository.save(EstudianteMapper.toEntity(estudiante));
        return toDto(guardado);
    }

    /** RF-07: actualiza los datos personales de un estudiante. */
    @Transactional
    public EstudianteDto actualizar(Long id, ActualizarEstudianteRequest req) {
        EstudianteJpaEntity e = obtener(id);
        e.setNombres(req.nombres());
        e.setApellidos(req.apellidos());
        e.setFechaNacimiento(req.fechaNacimiento());
        e.setDireccion(req.direccion());
        e.setAcudienteNombre(req.acudienteNombre());
        e.setAcudienteTelefono(req.acudienteTelefono());
        e.setAcudienteParentesco(req.acudienteParentesco());
        return toDto(estudianteRepository.save(e));
    }

    /** RF-08: consulta el perfil de un estudiante. */
    @Transactional(readOnly = true)
    public EstudianteDto consultar(Long id) {
        return toDto(obtener(id));
    }

    @Transactional(readOnly = true)
    public List<EstudianteDto> listar() {
        return estudianteRepository.findAll().stream().map(GestionEstudianteService::toDto).toList();
    }

    /** RF-10 / RB-20: retira un estudiante registrando motivo, fecha y autorizacion. */
    @Transactional
    public EstudianteDto retirar(Long id, RetirarEstudianteRequest req) {
        EstudianteJpaEntity e = obtener(id);
        // La invariante RB-20 (no retirar dos veces) se valida en el dominio.
        Estudiante dominio = EstudianteMapper.toDomain(e);
        dominio.retirar();
        e.setEstado(dominio.getEstado());
        e.setMotivoRetiro(req.motivo());
        e.setAutorizadoRetiro(req.autorizadoPor());
        e.setFechaRetiro(LocalDate.now());
        return toDto(estudianteRepository.save(e));
    }

    private EstudianteJpaEntity obtener(Long id) {
        return estudianteRepository.findById(id)
                .orElseThrow(() -> new ReglaNegocioException("RF-08", "El estudiante no existe."));
    }

    private static boolean esVacio(String s) {
        return s == null || s.isBlank();
    }

    private static EstudianteDto toDto(EstudianteJpaEntity e) {
        return new EstudianteDto(e.getId(), e.getDocumento(), e.getNombres(), e.getApellidos(),
                e.getFechaNacimiento(), e.getDireccion(), e.getEstado(),
                e.getAcudienteNombre(), e.getAcudienteTelefono(), e.getAcudienteParentesco());
    }
}
