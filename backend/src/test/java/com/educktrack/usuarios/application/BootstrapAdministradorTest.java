package com.educktrack.usuarios.application;

import com.educktrack.usuarios.domain.NombreRol;
import com.educktrack.usuarios.infrastructure.persistence.UsuarioRepository;
import com.educktrack.usuarios.infrastructure.rest.RegistrarUsuarioRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas del arranque que crea el Administrador inicial (RF-01, RS-03). Lo que
 * importa es que no pueda crear cuentas fuera del caso para el que existe: una
 * base sin ningun usuario.
 */
@ExtendWith(MockitoExtension.class)
class BootstrapAdministradorTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private GestionUsuarioService gestionUsuarioService;

    @Test
    void creaElAdministradorCuandoEseCorreoNoExiste() {
        when(usuarioRepository.existsByCorreoInstitucional("admin@colegio.edu.co")).thenReturn(false);

        bootstrap("admin@colegio.edu.co", "contrasena-larga").run(null);

        ArgumentCaptor<RegistrarUsuarioRequest> captor =
                ArgumentCaptor.forClass(RegistrarUsuarioRequest.class);
        verify(gestionUsuarioService).registrar(captor.capture());
        assertEquals("admin@colegio.edu.co", captor.getValue().correo());
        assertEquals(java.util.List.of(NombreRol.ADMINISTRADOR), captor.getValue().roles());
    }

    @Test
    void esIdempotenteSiLaCuentaYaExiste() {
        when(usuarioRepository.existsByCorreoInstitucional("admin@colegio.edu.co")).thenReturn(true);

        bootstrap("admin@colegio.edu.co", "contrasena-larga").run(null);

        // No debe pisar la cuenta ni devolverle la contrasena de arranque.
        verify(gestionUsuarioService, never()).registrar(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void noHaceNadaSinCredencialesConfiguradas() {
        bootstrap("", "").run(null);

        // Ni siquiera consulta la base: sin configuracion no es asunto suyo.
        verify(usuarioRepository, never()).existsByCorreoInstitucional(org.mockito.ArgumentMatchers.any());
        verify(gestionUsuarioService, never()).registrar(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void unFalloAlCrearNoImpideQueLaAplicacionArranque() {
        when(usuarioRepository.existsByCorreoInstitucional("admin@colegio.edu.co")).thenReturn(false);
        when(gestionUsuarioService.registrar(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new IllegalStateException("correo duplicado"));

        assertDoesNotThrow(() -> bootstrap("admin@colegio.edu.co", "contrasena-larga").run(null));
    }

    private BootstrapAdministrador bootstrap(String correo, String password) {
        return new BootstrapAdministrador(
                usuarioRepository, gestionUsuarioService, correo, password, "Administrador");
    }
}
