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
 *   <li>solo crea la cuenta <strong>si ese correo no existe todavia</strong>, de
 *       modo que es idempotente: no pisa una cuenta existente ni le devuelve la
 *       contrasena de arranque a un administrador cuya clave ya se cambio;</li>
 *   <li>las credenciales llegan por configuracion, nunca desde el repositorio;</li>
 *   <li>la cuenta nace con la marca de cambio de contrasena obligatorio que
 *       aplica {@code Usuario.nueva}, de modo que la contrasena de arranque no
 *       sobrevive al primer acceso.</li>
 * </ul>
 *
 * <p>La condicion es el correo concreto y no "que no haya ningun usuario":
 * ese guardian mas amplio parece mas seguro, pero deja el sistema sin salida en
 * cuanto existe cualquier fila en la tabla sin que haya un administrador capaz
 * de entrar, que es justo el atolladero que este arranque debe resolver.</p>
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
        if (usuarioRepository.existsByCorreoInstitucional(correo)) {
            log.info("Bootstrap de administrador omitido: la cuenta {} ya existe.", correo);
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
