package com.educktrack.auditoria.application;

import com.educktrack.auditoria.domain.TipoOperacion;
import com.educktrack.auditoria.infrastructure.persistence.AuditoriaJpaEntity;
import com.educktrack.auditoria.infrastructure.persistence.AuditoriaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

/**
 * Pruebas del registro del log de auditoria (RS-07, RF-63, RF-05).
 *
 * <p>Lo que se comprueba es la propiedad que hace util un log: que la
 * atribucion no dependa de lo que el llamador diga, y que ningun dato de
 * entrada pueda impedir que la anotacion se escriba.</p>
 */
@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {

    private static final String CORREO = "coordinador@colegio.edu.co";

    @Mock private AuditoriaRepository repository;
    @InjectMocks private AuditoriaService service;

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void atribuyeLaOperacionAlUsuarioDelContextoDeSeguridad() {
        autenticar(CORREO);

        service.registrar(TipoOperacion.NOTA_EDITADA, "calificacion", 42L, "Nota cambiada de 3.0 a 4.0.");

        AuditoriaJpaEntity guardado = capturar();
        // La atribucion sale del token, no de un parametro: un log donde cada
        // servicio elige que nombre escribe es falsificable por descuido.
        assertEquals(CORREO, guardado.getUsuario());
        assertEquals(TipoOperacion.NOTA_EDITADA, guardado.getOperacion());
        assertEquals("calificacion", guardado.getEntidad());
        assertEquals(42L, guardado.getEntidadId());
        assertEquals("Nota cambiada de 3.0 a 4.0.", guardado.getDescripcion());
    }

    @Test
    void anotaComoSistemaLoQueNoNaceDeUnaPeticionAutenticada() {
        SecurityContextHolder.clearContext();

        service.registrar(TipoOperacion.CORTE_CERRADO, "cierre_corte", 1L, "Cierre automatico.");

        assertEquals(AuditoriaService.USUARIO_SISTEMA, capturar().getUsuario());
    }

    @Test
    void registraLaFechaDeLaOperacion() {
        autenticar(CORREO);

        service.registrar(TipoOperacion.USUARIO_DESACTIVADO, "usuario", 3L, "Baja.");

        // RS-07 exige usuario, fecha y descripcion: la fecha no puede faltar.
        assertTrue(capturar().getFecha() != null);
    }

    @Test
    void recortaLasDescripcionesDemasiadoLargasEnVezDeFallar() {
        autenticar(CORREO);
        // Los motivos de retiro y de justificacion los escribe una persona y
        // pueden desbordar la columna; eso no debe tumbar la operacion auditada.
        String motivoLarguisimo = "x".repeat(900);

        service.registrar(TipoOperacion.ESTUDIANTE_RETIRADO, "estudiante", 9L, motivoLarguisimo);

        String descripcion = capturar().getDescripcion();
        assertEquals(500, descripcion.length());
        assertTrue(descripcion.endsWith("..."));
    }

    @Test
    void elAccesoSeAtribuyeALaCuentaQueIntentaEntrar() {
        // En un intento de acceso todavia no hay contexto de seguridad del que
        // resolver el autor, ni siquiera cuando prospera.
        SecurityContextHolder.clearContext();

        service.registrarAcceso("atacante@colegio.edu.co", false, "Intento rechazado.");

        AuditoriaJpaEntity guardado = capturar();
        assertEquals("atacante@colegio.edu.co", guardado.getUsuario());
        assertEquals(TipoOperacion.ACCESO_FALLIDO, guardado.getOperacion());
    }

    @Test
    void distingueElAccesoCorrectoDelRechazado() {
        service.registrarAcceso(CORREO, true, "Inicio correcto.");

        assertEquals(TipoOperacion.ACCESO_EXITOSO, capturar().getOperacion());
    }

    @Test
    void unAccesoSinCorreoQuedaAtribuidoAlSistemaYNoRompe() {
        service.registrarAcceso("  ", false, "Intento sin credenciales.");

        assertEquals(AuditoriaService.USUARIO_SISTEMA, capturar().getUsuario());
    }

    @Test
    void losAccesosSonLosUnicosRegistrosSinEntidadAsociada() {
        service.registrarAcceso(CORREO, true, "Inicio correcto.");

        AuditoriaJpaEntity guardado = capturar();
        assertEquals(null, guardado.getEntidad());
        assertEquals(null, guardado.getEntidadId());
        assertTrue(guardado.getOperacion().esAcceso());
    }

    // ---------------------------------------------------------------------

    private AuditoriaJpaEntity capturar() {
        ArgumentCaptor<AuditoriaJpaEntity> captor = ArgumentCaptor.forClass(AuditoriaJpaEntity.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    private static void autenticar(String correo) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(correo, null, List.of()));
    }
}
