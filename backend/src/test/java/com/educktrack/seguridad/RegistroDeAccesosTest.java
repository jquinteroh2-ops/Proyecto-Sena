package com.educktrack.seguridad;

import com.educktrack.auditoria.application.AuditoriaService;
import com.educktrack.seguridad.application.AutenticacionService;
import com.educktrack.seguridad.infrastructure.security.JwtService;
import com.educktrack.usuarios.infrastructure.persistence.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas del historial de inicios de sesion (RF-05, RS-07).
 *
 * <p>El caso que importa es el rechazado: es el que revela un acceso indebido y
 * el unico que podria perderse si la anotacion dependiera de que la operacion
 * prospere.</p>
 */
@ExtendWith(MockitoExtension.class)
class RegistroDeAccesosTest {

    private static final String CORREO = "usuario@colegio.edu.co";

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private JwtService jwtService;
    @Mock private AuditoriaService auditoria;

    @InjectMocks private AutenticacionService service;

    @Test
    void anotaElIntentoRechazadoAntesDePropagarElError() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Credenciales invalidas"));

        assertThrows(BadCredentialsException.class, () -> service.login(CORREO, "clave-erronea"));

        // El rechazo se sigue propagando (el cliente recibe su 401) y ademas
        // queda registrado.
        verify(auditoria).registrarAcceso(eq(CORREO), eq(false), any());
    }

    @Test
    void noEmiteTokenNiConsultaLaCuentaCuandoLasCredencialesFallan() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Credenciales invalidas"));

        assertThrows(BadCredentialsException.class, () -> service.login(CORREO, "clave-erronea"));

        verify(jwtService, org.mockito.Mockito.never()).generarToken(any(), any());
    }
}
