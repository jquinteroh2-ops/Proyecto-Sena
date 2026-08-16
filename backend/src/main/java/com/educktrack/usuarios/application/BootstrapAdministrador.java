package com.educktrack.usuarios.application;

import com.educktrack.usuarios.domain.NombreRol;
import com.educktrack.usuarios.infrastructure.persistence.UsuarioRepository;
import com.educktrack.usuarios.infrastructure.rest.RegistrarUsuarioRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Crea la cuenta de Administrador inicial en un despliegue nuevo (RF-01, RS-03).
 *
 * <p>Registrar usuarios exige rol ADMINISTRADOR y las migraciones no siembran
 * ninguna cuenta, de modo que una base recien creada no admite el primer inicio
 * de sesion: no hay forma de entrar para crear a quien podria entrar. Este
 * arranque rompe ese circulo.</p>
 *
 * <p>Tres condiciones lo mantienen acotado:</p>
 * <ul>
 *   <li>solo actua si la base <strong>no tiene ningun usuario</strong>, asi que
 *       nunca puede crear una cuenta en un sistema ya en uso ni reactivar un
 *       administrador que se haya desactivado a proposito;</li>
 *   <li>las credenciales llegan por configuracion, nunca desde el repositorio;</li>
 *   <li>la cuenta nace con la marca de cambio de contrasena obligatorio que
 *       aplica {@code Usuario.nueva}, de modo que la contrasena de arranque no
 *       sobrevive al primer acceso.</li>
 * </ul>
 *
 * <p>Sin configuracion no hace nada, que es lo que corresponde en desarrollo y
 * en los entornos donde las cuentas ya existen.</p>
 */
@Component
public class BootstrapAdministrador implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdministrador.class);

    private final UsuarioRepository usuarioRepository;
    private final GestionUsuarioService gestionUsuarioService;
    private final String correo;
    private final String password;
    private final String nombre;

    public BootstrapAdministrador(
            UsuarioRepository usuarioRepository,
            GestionUsuarioService gestionUsuarioService,
            @Value("${educktrack.bootstrap.admin.correo:}") String correo,
            @Value("${educktrack.bootstrap.admin.password:}") String password,
            @Value("${educktrack.bootstrap.admin.nombre:Administrador}") String nombre) {
        this.usuarioRepository = usuarioRepository;
        this.gestionUsuarioService = gestionUsuarioService;
        this.correo = correo;
        this.password = password;
        this.nombre = nombre;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (correo.isBlank() || password.isBlank()) {
            return;
        }
        if (usuarioRepository.count() > 0) {
            log.info("Bootstrap de administrador omitido: el sistema ya tiene cuentas registradas.");
            return;
        }
        try {
            gestionUsuarioService.registrar(new RegistrarUsuarioRequest(
                    nombre, correo, password, List.of(NombreRol.ADMINISTRADOR)));
            log.info("Administrador inicial creado para {}. Debe cambiar la contrasena en el primer acceso.", correo);
        } catch (RuntimeException ex) {
            // Un fallo aqui no debe impedir que la aplicacion arranque: el
            // sistema sigue siendo utilizable y el error queda registrado.
            log.error("No se pudo crear el administrador inicial: {}", ex.getMessage());
        }
    }
}
