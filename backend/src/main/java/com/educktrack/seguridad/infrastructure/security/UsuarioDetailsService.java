package com.educktrack.seguridad.infrastructure.security;

import com.educktrack.usuarios.infrastructure.persistence.UsuarioJpaEntity;
import com.educktrack.usuarios.infrastructure.persistence.UsuarioRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Carga las credenciales y roles del usuario para Spring Security (RS-03, RF-60).
 * El nombre de usuario es el correo institucional. Los roles se exponen como
 * authorities con prefijo {@code ROLE_} para el control de acceso por rol.
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        UsuarioJpaEntity usuario = usuarioRepository.findByCorreoInstitucional(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + correo));

        List<GrantedAuthority> authorities = usuario.getRoles().stream()
                .map(rol -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + rol.getNombre().name()))
                .toList();

        // HU-02: una cuenta desactivada no puede iniciar sesion (enabled=false).
        return User.withUsername(usuario.getCorreoInstitucional())
                .password(usuario.getPasswordHash())
                .authorities(authorities)
                .disabled(!usuario.isActivo())
                .build();
    }
}
