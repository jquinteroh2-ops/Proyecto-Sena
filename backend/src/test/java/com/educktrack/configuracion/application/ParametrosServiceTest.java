package com.educktrack.configuracion.application;

import com.educktrack.auditoria.application.AuditoriaService;
import com.educktrack.auditoria.domain.TipoOperacion;
import com.educktrack.configuracion.domain.ParametroInstitucional;
import com.educktrack.configuracion.infrastructure.persistence.ParametroJpaEntity;
import com.educktrack.configuracion.infrastructure.persistence.ParametroRepository;
import com.educktrack.notas.domain.EscalaCalificacion;
import com.educktrack.shared.domain.ReglaNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de los parametros institucionales (RF-59, RS-14).
 *
 * <p>Lo que importa aqui es que un parametro invalido se rechace <em>al
 * guardarlo</em>. Un valor imposible no falla donde se escribe sino mucho
 * despues, calificando, y para entonces nadie relaciona el sintoma con el
 * cambio de configuracion que lo provoco.</p>
 */
@ExtendWith(MockitoExtension.class)
class ParametrosServiceTest {

    @Mock private ParametroRepository repository;
    @Mock private AuditoriaService auditoria;

    @InjectMocks private ParametrosService service;

    @Test
    void usaLosValoresDelEnunciadoCuandoLaTablaEstaVacia() {
        // Una base anterior a V15, o un parametro anadido despues, no puede
        // dejar el sistema sin arrancar por una fila que falta.
        when(repository.findAll()).thenReturn(List.of());

        assertEquals(EscalaCalificacion.POR_DEFECTO, service.escalaCalificacion());
        assertEquals(80.0, service.porcentajeMinimoAsistencia());
        assertEquals(30, service.maxHorasDocente());
    }

    @Test
    void leeLosValoresConfigurados() {
        when(repository.findAll()).thenReturn(List.of(
                fila(ParametroInstitucional.NOTA_MAXIMA, "10.00"),
                fila(ParametroInstitucional.NOTA_APROBATORIA, "6.00"),
                fila(ParametroInstitucional.PORCENTAJE_MINIMO_ASISTENCIA, "75")));

        EscalaCalificacion escala = service.escalaCalificacion();
        assertEquals(new BigDecimal("10.00"), escala.maxima());
        assertEquals(new BigDecimal("6.00"), escala.aprobatoria());
        assertEquals(75.0, service.porcentajeMinimoAsistencia());
    }

    @Test
    void rechazaUnValorNoNumerico() {
        when(repository.findAll()).thenReturn(List.of());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.actualizar(ParametroInstitucional.NOTA_MAXIMA, "cinco"));

        assertEquals("RF-59", error.getCodigoRegla());
        verify(repository, never()).save(any());
    }

    @Test
    void rechazaUnPorcentajeFueraDeRango() {
        when(repository.findAll()).thenReturn(List.of());

        assertThrows(ReglaNegocioException.class, () -> service.actualizar(
                ParametroInstitucional.PORCENTAJE_MINIMO_ASISTENCIA, "150"));
        verify(repository, never()).save(any());
    }

    @Test
    void rechazaUnMaximoDeHorasNoEntero() {
        when(repository.findAll()).thenReturn(List.of());

        assertThrows(ReglaNegocioException.class,
                () -> service.actualizar(ParametroInstitucional.MAX_HORAS_DOCENTE, "22.5"));
    }

    @Test
    void rechazaElCambioQueDejariaUnaEscalaEnLaQueNadiePuedeAprobar() {
        // Subir la nota aprobatoria por encima del maximo es valido mirando solo
        // ese parametro, y deja una escala en la que ninguna nota aprueba. Por
        // eso la escala se valida como conjunto y no parametro a parametro.
        when(repository.findAll()).thenReturn(List.of());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.actualizar(ParametroInstitucional.NOTA_APROBATORIA, "9.00"));

        assertEquals("RF-59", error.getCodigoRegla());
        verify(repository, never()).save(any());
    }

    @Test
    void guardaElCambioValidoYLoDejaEnElLogDeAuditoria() {
        when(repository.findAll()).thenReturn(List.of());

        service.actualizar(ParametroInstitucional.PORCENTAJE_MINIMO_ASISTENCIA, "75");

        ArgumentCaptor<ParametroJpaEntity> captor = ArgumentCaptor.forClass(ParametroJpaEntity.class);
        verify(repository).save(captor.capture());
        assertEquals("75", captor.getValue().getValor());
        // RS-07: cambiar el minimo de asistencia decide quien pierde el derecho
        // a evaluacion, de modo que es critico aunque no toque ninguna nota.
        verify(auditoria).registrar(eq(TipoOperacion.PARAMETRO_ACTUALIZADO),
                eq("parametro_institucional"), isNull(), anyString());
    }

    @Test
    void relesLosValoresDespuesDeUnCambio() {
        when(repository.findAll())
                .thenReturn(List.of(fila(ParametroInstitucional.MAX_HORAS_DOCENTE, "30")))
                .thenReturn(List.of(fila(ParametroInstitucional.MAX_HORAS_DOCENTE, "22")));

        assertEquals(30, service.maxHorasDocente());
        service.actualizar(ParametroInstitucional.MAX_HORAS_DOCENTE, "22");

        // Si la cache no se invalidara, el cambio no surtiria efecto hasta
        // reiniciar, que es justo lo que RF-59 viene a evitar.
        assertEquals(22, service.maxHorasDocente());
    }

    private static ParametroJpaEntity fila(ParametroInstitucional clave, String valor) {
        ParametroJpaEntity e = new ParametroJpaEntity();
        e.setClave(clave);
        e.setValor(valor);
        e.setActualizado(LocalDateTime.now());
        e.setActualizadoPor("rector@colegio.edu.co");
        return e;
    }
}
