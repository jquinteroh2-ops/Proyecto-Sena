package com.educktrack.seguridad.application;

import com.educktrack.auditoria.application.AuditoriaService;
import com.educktrack.seguridad.infrastructure.rest.LoginResponse;
import com.educktrack.seguridad.infrastructure.rest.UsuarioAutenticadoDto;
import com.educktrack.seguridad.infrastructure.security.JwtService;
import com.educktrack.usuarios.domain.NombreRol;
import com.educktrack.usuarios.infrastructure.persistence.RolJpaEntity;
import com.educktrack.usuarios.infrastructure.persistence.UsuarioJpaEntity;
import com.educktrack.usuarios.infrastructure.persistence.UsuarioRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
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
    private final AuditoriaService auditoria;

    public AutenticacionService(AuthenticationManager authenticationManager,
                                UsuarioRepository usuarioRepository,
                                JwtService jwtService,
                                AuditoriaService auditoria) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
        this.auditoria = auditoria;
    }

    /**
     * Autentica al usuario y devuelve el token con sus datos (RF-60). Ante
     * credenciales invalidas, el AuthenticationManager lanza AuthenticationException,
     * traducida a 401 por el manejador global (RNF-10).
     */
    public LoginResponse login(String correo, String password) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(correo, password));
        } catch (AuthenticationException ex) {
            // RF-05 / RS-07: el intento fallido se anota antes de propagar. Es
            // el registro que mas importa de este historial, porque una racha
            // sobre la misma cuenta es la senal de un acceso indebido.
            // No se anota el motivo exacto del rechazo (cuenta inexistente o
            // contrasena erronea) para no convertir el log en un oraculo que
            // confirme que correos estan dados de alta.
            auditoria.registrarAcceso(correo, false, "Intento de inicio de sesion rechazado.");
            throw ex;
        }

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

        // RF-05: historial de inicios de sesion.
        auditoria.registrarAcceso(usuario.getCorreoInstitucional(), true,
                "Inicio de sesion correcto con roles " + roles + ".");

        return new LoginResponse(token, "Bearer", jwtService.getExpiracionSegundos(), dto);
    }
}
