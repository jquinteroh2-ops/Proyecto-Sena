package com.educktrack.identidad.application;

import com.educktrack.docentes.infrastructure.persistence.DocenteJpaEntity;
import com.educktrack.docentes.infrastructure.persistence.DocenteRepository;
import com.educktrack.estudiantes.infrastructure.persistence.EstudianteJpaEntity;
import com.educktrack.estudiantes.infrastructure.persistence.EstudianteRepository;
import com.educktrack.identidad.domain.Parentesco;
import com.educktrack.identidad.infrastructure.persistence.VinculoAcudienteJpaEntity;
import com.educktrack.identidad.infrastructure.persistence.VinculoAcudienteRepository;
import com.educktrack.identidad.infrastructure.rest.VinculacionDtos.IdentidadDto;
import com.educktrack.identidad.infrastructure.rest.VinculacionDtos.VinculoAcudienteDto;
import com.educktrack.identidad.infrastructure.rest.VinculacionDtos.VinculoCuentaDto;
import com.educktrack.shared.domain.ReglaNegocioException;
import com.educktrack.usuarios.domain.NombreRol;
import com.educktrack.usuarios.infrastructure.persistence.RolJpaEntity;
import com.educktrack.usuarios.infrastructure.persistence.UsuarioJpaEntity;
import com.educktrack.usuarios.infrastructure.persistence.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Casos de uso de vinculacion entre cuentas de usuario y perfiles academicos.
 *
 * <p>Implementa RF-11 (vincular padre a estudiante) y habilita RS-03 / RNF-07:
 * sin este vinculo el sistema no puede determinar que registro academico
 * corresponde al usuario autenticado, por lo que los roles ESTUDIANTE, DOCENTE
 * y PADRE_FAMILIA no podian aplicarse sobre datos propios.</p>
 *
 * <p>Refuerza RB-14 a nivel de datos: una misma cuenta no puede quedar
 * vinculada simultaneamente a un perfil de estudiante y a uno de docente,
 * complemento de la invariante que el dominio ya valida sobre los roles.</p>
 */
@Service
public class VinculacionService {

    private final UsuarioRepository usuarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final DocenteRepository docenteRepository;
    private final VinculoAcudienteRepository vinculoRepository;
    private final ContextoUsuario contextoUsuario;

    public VinculacionService(UsuarioRepository usuarioRepository,
                              EstudianteRepository estudianteRepository,
                              DocenteRepository docenteRepository,
                              VinculoAcudienteRepository vinculoRepository,
                              ContextoUsuario contextoUsuario) {
        this.usuarioRepository = usuarioRepository;
        this.estudianteRepository = estudianteRepository;
        this.docenteRepository = docenteRepository;
        this.vinculoRepository = vinculoRepository;
        this.contextoUsuario = contextoUsuario;
    }

    /**
     * Vincula una cuenta existente con el perfil de un estudiante (RS-03).
     * La cuenta debe tener el rol ESTUDIANTE y no estar vinculada a otro perfil.
     */
    @Transactional
    public VinculoCuentaDto vincularEstudiante(Long estudianteId, Long usuarioId) {
        EstudianteJpaEntity estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new ReglaNegocioException("RF-08", "El estudiante no existe."));
        UsuarioJpaEntity usuario = usuarioActivo(usuarioId);
        exigirRol(usuario, NombreRol.ESTUDIANTE);

        if (estudiante.getUsuarioId() != null && !estudiante.getUsuarioId().equals(usuarioId)) {
            throw new ReglaNegocioException("RS-03",
                    "El estudiante ya tiene una cuenta de usuario vinculada.");
        }
        // RB-14 a nivel de datos: la cuenta no puede ser tambien de un docente.
        if (docenteRepository.existsByUsuarioId(usuarioId)) {
            throw new ReglaNegocioException("RB-14",
                    "La cuenta ya esta vinculada a un docente; no puede vincularse ademas a un estudiante.");
        }
        if (estudianteRepository.findByUsuarioId(usuarioId)
                .filter(otro -> !otro.getId().equals(estudianteId)).isPresent()) {
            throw new ReglaNegocioException("RS-03",
                    "La cuenta ya esta vinculada a otro estudiante.");
        }

        estudiante.setUsuarioId(usuarioId);
        estudianteRepository.save(estudiante);
        return new VinculoCuentaDto(estudiante.getId(),
                estudiante.getNombres() + " " + estudiante.getApellidos(),
                usuario.getId(), usuario.getCorreoInstitucional());
    }

    /**
     * Vincula una cuenta existente con el perfil de un docente (RS-03).
     * La cuenta debe tener el rol DOCENTE y no estar vinculada a otro perfil.
     */
    @Transactional
    public VinculoCuentaDto vincularDocente(Long docenteId, Long usuarioId) {
        DocenteJpaEntity docente = docenteRepository.findById(docenteId)
                .orElseThrow(() -> new ReglaNegocioException("RF-13", "El docente no existe."));
        UsuarioJpaEntity usuario = usuarioActivo(usuarioId);
        exigirRol(usuario, NombreRol.DOCENTE);

        if (docente.getUsuarioId() != null && !docente.getUsuarioId().equals(usuarioId)) {
            throw new ReglaNegocioException("RS-03",
                    "El docente ya tiene una cuenta de usuario vinculada.");
        }
        // RB-14 a nivel de datos: la cuenta no puede ser tambien de un estudiante.
        if (estudianteRepository.existsByUsuarioId(usuarioId)) {
            throw new ReglaNegocioException("RB-14",
                    "La cuenta ya esta vinculada a un estudiante; no puede vincularse ademas a un docente.");
        }
        if (docenteRepository.findByUsuarioId(usuarioId)
                .filter(otro -> !otro.getId().equals(docenteId)).isPresent()) {
            throw new ReglaNegocioException("RS-03",
                    "La cuenta ya esta vinculada a otro docente.");
        }

        docente.setUsuarioId(usuarioId);
        docenteRepository.save(docente);
        return new VinculoCuentaDto(docente.getId(),
                docente.getNombres() + " " + docente.getApellidos(),
                usuario.getId(), usuario.getCorreoInstitucional());
    }

    /**
     * RF-11 / RD-08: vincula la cuenta de un padre de familia con un estudiante
     * bajo su responsabilidad. Es el vinculo que habilita RB-08.
     */
    @Transactional
    public VinculoAcudienteDto vincularAcudiente(Long estudianteId, Long usuarioId, Parentesco parentesco) {
        if (!estudianteRepository.existsById(estudianteId)) {
            throw new ReglaNegocioException("RF-11", "El estudiante no existe.");
        }
        UsuarioJpaEntity usuario = usuarioActivo(usuarioId);
        exigirRol(usuario, NombreRol.PADRE_FAMILIA);

        if (vinculoRepository.existsByUsuarioIdAndEstudianteId(usuarioId, estudianteId)) {
            throw new ReglaNegocioException("RF-11",
                    "El acudiente ya esta vinculado a este estudiante.");
        }

        VinculoAcudienteJpaEntity vinculo = new VinculoAcudienteJpaEntity();
        vinculo.setUsuarioId(usuarioId);
        vinculo.setEstudianteId(estudianteId);
        vinculo.setParentesco(parentesco);
        vinculo.setFechaVinculo(LocalDateTime.now());
        return toDto(vinculoRepository.save(vinculo), usuario);
    }

    /** RF-11: elimina un vinculo de acudiente (revoca la visibilidad de RB-08). */
    @Transactional
    public void desvincularAcudiente(Long vinculoId) {
        if (!vinculoRepository.existsById(vinculoId)) {
            throw new ReglaNegocioException("RF-11", "El vinculo de acudiente no existe.");
        }
        vinculoRepository.deleteById(vinculoId);
    }

    /** RF-11: acudientes formalmente vinculados a un estudiante. */
    @Transactional(readOnly = true)
    public List<VinculoAcudienteDto> acudientesDe(Long estudianteId) {
        return vinculoRepository.findByEstudianteId(estudianteId).stream()
                .map(v -> toDto(v, usuarioRepository.findById(v.getUsuarioId()).orElse(null)))
                .toList();
    }

    /**
     * Identidad del usuario autenticado (RS-03). El cliente la usa para saber
     * que perfil le corresponde sin enviar identificadores manipulables.
     */
    @Transactional(readOnly = true)
    public IdentidadDto identidadActual() {
        UsuarioJpaEntity usuario = contextoUsuario.usuarioActual();
        Set<NombreRol> roles = usuario.getRoles().stream()
                .map(RolJpaEntity::getNombre)
                .collect(Collectors.toSet());
        return new IdentidadDto(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreoInstitucional(),
                roles,
                contextoUsuario.estudianteIdActual().orElse(null),
                contextoUsuario.docenteIdActual().orElse(null),
                contextoUsuario.estudiantesTutelados());
    }

    // ---------------------------------------------------------------------

    private UsuarioJpaEntity usuarioActivo(Long usuarioId) {
        UsuarioJpaEntity usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ReglaNegocioException("RF-01", "La cuenta de usuario no existe."));
        if (!usuario.isActivo()) {
            throw new ReglaNegocioException("RF-03",
                    "La cuenta de usuario esta desactivada y no puede vincularse.");
        }
        return usuario;
    }

    /** RS-03: el perfil solo puede vincularse a una cuenta con el rol adecuado. */
    private static void exigirRol(UsuarioJpaEntity usuario, NombreRol requerido) {
        boolean tieneRol = usuario.getRoles().stream()
                .map(RolJpaEntity::getNombre)
                .anyMatch(requerido::equals);
        if (!tieneRol) {
            throw new ReglaNegocioException("RS-03",
                    "La cuenta debe tener el rol " + requerido.name() + " para realizar esta vinculacion.");
        }
    }

    private static VinculoAcudienteDto toDto(VinculoAcudienteJpaEntity v, UsuarioJpaEntity usuario) {
        return new VinculoAcudienteDto(
                v.getId(), v.getUsuarioId(),
                usuario != null ? usuario.getNombre() : null,
                usuario != null ? usuario.getCorreoInstitucional() : null,
                v.getEstudianteId(), v.getParentesco(), v.getFechaVinculo());
    }
}
