package com.educktrack.usuarios.infrastructure.config;

import com.educktrack.usuarios.domain.NombreRol;
import com.educktrack.usuarios.domain.Usuario;
import com.educktrack.usuarios.infrastructure.persistence.RolJpaEntity;
import com.educktrack.usuarios.infrastructure.persistence.RolRepository;
import com.educktrack.usuarios.infrastructure.persistence.UsuarioMapper;
import com.educktrack.usuarios.infrastructure.persistence.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/**
 * Crea una cuenta de Administrador inicial si no existe (idempotente), para
 * permitir el primer acceso al sistema (RF-01, RS-03).
 *
 * <p>DECISION DE DISENO: se usa un inicializador en arranque en lugar de un seed
 * en Flyway para no versionar un hash de contrasena en el repositorio. Las
 * credenciales por defecto son configurables por variables de entorno y la
 * cuenta nace con {@code debeCambiarPassword=true} (HU-01).</p>
 */
@Component
public class InicializadorAdmin implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InicializadorAdmin.class);

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final String correoAdmin;
    private final String passwordAdmin;

    public InicializadorAdmin(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                              PasswordEncoder passwordEncoder,
                              @Value("${educktrack.bootstrap.admin-correo:admin@educktrack.edu.co}") String correoAdmin,
                              @Value("${educktrack.bootstrap.admin-password:Admin123*}") String passwordAdmin) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.correoAdmin = correoAdmin;
        this.passwordAdmin = passwordAdmin;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.existsByCorreoInstitucional(correoAdmin)) {
            return; // Ya existe: nada que hacer.
        }

        Optional<RolJpaEntity> rolAdmin = rolRepository.findById(NombreRol.ADMINISTRADOR);
        if (rolAdmin.isEmpty()) {
            log.warn("No se encontro el rol ADMINISTRADOR; se omite la creacion del admin inicial.");
            return;
        }

        Usuario admin = Usuario.nueva("Administrador", correoAdmin,
                passwordEncoder.encode(passwordAdmin), Set.of(NombreRol.ADMINISTRADOR));
        usuarioRepository.save(UsuarioMapper.toEntity(admin, Set.of(rolAdmin.get())));

        log.warn("=== Usuario ADMINISTRADOR inicial creado: {} (contrasena por defecto; cambiela en el primer ingreso) ===",
                correoAdmin);
    }
}
