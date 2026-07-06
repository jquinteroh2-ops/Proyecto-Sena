package com.educktrack.seguridad.application;

import com.educktrack.seguridad.infrastructure.rest.LoginResponse;
import com.educktrack.seguridad.infrastructure.rest.UsuarioAutenticadoDto;
import com.educktrack.seguridad.infrastructure.security.JwtService;
import com.educktrack.usuarios.domain.NombreRol;
import com.educktrack.usuarios.infrastructure.persistence.RolJpaEntity;
import com.educktrack.usuarios.infrastructure.persistence.UsuarioJpaEntity;
import com.educktrack.usuarios.infrastructure.persistence.UsuarioRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Caso de uso de autenticacion (RF-60). Valida credenciales con el
 * {@link AuthenticationManager} (que usa BCrypt, RS-05) y emite el JWT (RS-04).
 */
@Service
public class AutenticacionService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    public AutenticacionService(AuthenticationManager authenticationManager,
                                UsuarioRepository usuarioRepository,
                                JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
    }

    /**
     * Autentica al usuario y devuelve el token con sus datos (RF-60). Ante
     * credenciales invalidas, el AuthenticationManager lanza AuthenticationException,
     * traducida a 401 por el manejador global (RNF-10).
     */
    public LoginResponse login(String correo, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(correo, password));

        UsuarioJpaEntity usuario = usuarioRepository.findByCorreoInstitucional(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + correo));

        List<String> roles = usuario.getRoles().stream()
                .map(RolJpaEntity::getNombre)
                .map(NombreRol::name)
                .toList();

        String token = jwtService.generarToken(usuario.getCorreoInstitucional(), roles);

        UsuarioAutenticadoDto dto = new UsuarioAutenticadoDto(
                usuario.getId(), usuario.getNombre(), usuario.getCorreoInstitucional(),
                roles, usuario.isDebeCambiarPassword());

        return new LoginResponse(token, "Bearer", jwtService.getExpiracionSegundos(), dto);
    }
}
