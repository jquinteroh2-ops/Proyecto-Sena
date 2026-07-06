package com.educktrack.usuarios.application;

import com.educktrack.shared.domain.ReglaNegocioException;
import com.educktrack.usuarios.domain.NombreRol;
import com.educktrack.usuarios.domain.Usuario;
import com.educktrack.usuarios.infrastructure.persistence.RolJpaEntity;
import com.educktrack.usuarios.infrastructure.persistence.RolRepository;
import com.educktrack.usuarios.infrastructure.persistence.UsuarioJpaEntity;
import com.educktrack.usuarios.infrastructure.persistence.UsuarioMapper;
import com.educktrack.usuarios.infrastructure.persistence.UsuarioRepository;
import com.educktrack.usuarios.infrastructure.rest.RegistrarUsuarioRequest;
import com.educktrack.usuarios.infrastructure.rest.UsuarioDto;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Casos de uso de gestion de usuarios (RF-01, RF-03, RF-04). Aplica las reglas
 * de negocio en la capa de aplicacion: unicidad de correo (HU-01) y exclusion
 * de roles Estudiante/Docente (RB-14, invariante del dominio {@link Usuario}).
 */
@Service
public class GestionUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public GestionUsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                                 PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** RF-01 / HU-01: registra una cuenta con contrasena cifrada (RS-05) y roles. */
    @Transactional
    public UsuarioDto registrar(RegistrarUsuarioRequest request) {
        if (usuarioRepository.existsByCorreoInstitucional(request.correo())) {
            throw new ReglaNegocioException("HU-01", "El correo institucional ya esta registrado.");
        }

        Set<NombreRol> roles = new LinkedHashSet<>(request.roles());
        // Construir el dominio valida RB-14 (roles Estudiante/Docente excluyentes).
        Usuario usuario = Usuario.nueva(
                request.nombre(), request.correo(),
                passwordEncoder.encode(request.password()), roles);

        Set<RolJpaEntity> rolesGestionados = cargarRoles(roles);
        UsuarioJpaEntity guardado = usuarioRepository.save(UsuarioMapper.toEntity(usuario, rolesGestionados));
        return toDto(guardado);
    }

    /** RF-03 / HU-02: desactiva una cuenta sin eliminar su historial. */
    @Transactional
    public UsuarioDto desactivar(Long usuarioId) {
        UsuarioJpaEntity usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ReglaNegocioException("RF-03", "El usuario no existe."));
        usuario.setActivo(false);
        return toDto(usuarioRepository.save(usuario));
    }

    @Transactional(readOnly = true)
    public List<UsuarioDto> listar() {
        return usuarioRepository.findAll().stream().map(GestionUsuarioService::toDto).toList();
    }

    private Set<RolJpaEntity> cargarRoles(Set<NombreRol> roles) {
        List<RolJpaEntity> encontrados = rolRepository.findAllById(roles);
        if (encontrados.size() != roles.size()) {
            throw new ReglaNegocioException("RF-04", "Uno o mas roles indicados no existen.");
        }
        return new LinkedHashSet<>(encontrados);
    }

    private static UsuarioDto toDto(UsuarioJpaEntity e) {
        List<NombreRol> roles = e.getRoles().stream().map(RolJpaEntity::getNombre).toList();
        return new UsuarioDto(e.getId(), e.getNombre(), e.getCorreoInstitucional(),
                e.isActivo(), e.isDebeCambiarPassword(), roles);
    }
}
