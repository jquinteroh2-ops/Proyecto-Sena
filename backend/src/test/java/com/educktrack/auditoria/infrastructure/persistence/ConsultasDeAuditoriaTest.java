package com.educktrack.auditoria.infrastructure.persistence;

import com.educktrack.auditoria.domain.TipoOperacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de las consultas del log de auditoria (RS-07, RF-05).
 *
 * <p>Mismo motivo que {@code ConsultasDeAlcanceTest}: las pruebas del servicio
 * usan dobles, de modo que un nombre de metodo derivado mal escrito no se
 * detectaria hasta arrancar la aplicacion. Aqui se ejercitan las consultas de
 * verdad, incluido el orden cronologico inverso, que es una promesa del
 * contrato y no un detalle.</p>
 */
@DataJpaTest
@ActiveProfiles("test")
class ConsultasDeAuditoriaTest {

    private static final String COORDINADOR = "coordinador@colegio.edu.co";
    private static final String DOCENTE = "docente@colegio.edu.co";

    @Autowired private AuditoriaRepository repository;

    @BeforeEach
    void prepararLog() {
        LocalDateTime base = LocalDateTime.of(2026, 3, 1, 8, 0);

        registrar(COORDINADOR, TipoOperacion.ACCESO_EXITOSO, base);
        registrar(COORDINADOR, TipoOperacion.ACCESO_FALLIDO, base.plusMinutes(5));
        registrar(COORDINADOR, TipoOperacion.ESTUDIANTE_RETIRADO, base.plusMinutes(10));
        registrar(DOCENTE, TipoOperacion.NOTA_REGISTRADA, base.plusMinutes(15));
        registrar(DOCENTE, TipoOperacion.ACCESO_EXITOSO, base.plusMinutes(20));
    }

    @Test
    void devuelveElLogCompletoEnOrdenCronologicoInverso() {
        Page<AuditoriaJpaEntity> pagina = repository.findByOrderByFechaDesc(PageRequest.of(0, 10));

        assertEquals(5, pagina.getTotalElements());
        // Lo ultimo que paso va primero: auditar es casi siempre mirar hacia atras.
        assertEquals(TipoOperacion.ACCESO_EXITOSO, pagina.getContent().get(0).getOperacion());
        assertEquals(DOCENTE, pagina.getContent().get(0).getUsuario());
    }

    @Test
    void filtraElLogPorUsuario() {
        Page<AuditoriaJpaEntity> pagina =
                repository.findByUsuarioOrderByFechaDesc(DOCENTE, PageRequest.of(0, 10));

        assertEquals(2, pagina.getTotalElements());
        assertTrue(pagina.getContent().stream().allMatch(r -> DOCENTE.equals(r.getUsuario())));
    }

    @Test
    void filtraElLogPorTipoDeOperacion() {
        Page<AuditoriaJpaEntity> pagina = repository.findByOperacionOrderByFechaDesc(
                TipoOperacion.NOTA_REGISTRADA, PageRequest.of(0, 10));

        assertEquals(1, pagina.getTotalElements());
        assertEquals(DOCENTE, pagina.getContent().get(0).getUsuario());
    }

    @Test
    void elHistorialDeAccesosIncluyeLosIntentosFallidos() {
        Page<AuditoriaJpaEntity> pagina = repository.findByUsuarioAndOperacionInOrderByFechaDesc(
                COORDINADOR,
                Set.of(TipoOperacion.ACCESO_EXITOSO, TipoOperacion.ACCESO_FALLIDO),
                PageRequest.of(0, 10));

        // RF-05: los dos accesos del coordinador, no el retiro que tambien hizo.
        assertEquals(2, pagina.getTotalElements());
        assertTrue(pagina.getContent().stream().allMatch(r -> r.getOperacion().esAcceso()));
    }

    @Test
    void lasPaginasAcotanElVolumenDevuelto() {
        Page<AuditoriaJpaEntity> primera = repository.findByOrderByFechaDesc(PageRequest.of(0, 2));

        // El log crece sin limite: la paginacion no es cosmetica.
        assertEquals(2, primera.getContent().size());
        assertEquals(5, primera.getTotalElements());
        assertEquals(3, primera.getTotalPages());
    }

    @Test
    void unUsuarioSinActividadDevuelveUnaPaginaVacia() {
        Page<AuditoriaJpaEntity> pagina = repository.findByUsuarioOrderByFechaDesc(
                "nadie@colegio.edu.co", PageRequest.of(0, 10));

        assertEquals(0, pagina.getTotalElements());
        assertEquals(List.of(), pagina.getContent());
    }

    // ---------------------------------------------------------------------

    private void registrar(String usuario, TipoOperacion operacion, LocalDateTime fecha) {
        AuditoriaJpaEntity registro = new AuditoriaJpaEntity();
        registro.setUsuario(usuario);
        registro.setOperacion(operacion);
        registro.setDescripcion("Registro de prueba: " + operacion);
        registro.setFecha(fecha);
        repository.save(registro);
    }
}
