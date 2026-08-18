package com.educktrack.notas.application;

import com.educktrack.auditoria.application.AuditoriaService;
import com.educktrack.notas.domain.TipoEvaluacion;
import com.educktrack.notas.infrastructure.persistence.PonderacionRepository;
import com.educktrack.notas.infrastructure.rest.NotaDtos.ConfigurarPonderacionRequest;
import com.educktrack.notas.infrastructure.rest.NotaDtos.PonderacionItem;
import com.educktrack.shared.domain.ReglaNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Pruebas de RB-07: los porcentajes de ponderacion de una materia deben sumar
 * 100%.
 *
 * <p>Lo que se comprueba, mas alla de la suma, es que ninguna configuracion
 * invalida llegue a persistirse: {@code configurar} borra la ponderacion previa
 * antes de escribir la nueva, asi que aceptar una entrada mal formada no dejaria
 * la materia con la configuracion vieja, sino sin ninguna.</p>
 */
@ExtendWith(MockitoExtension.class)
class PonderacionServiceTest {

    private static final Long MATERIA = 7L;
    private static final Long PERIODO = 1L;

    @Mock private PonderacionRepository ponderacionRepository;
    @Mock private AuditoriaService auditoria;

    @InjectMocks private PonderacionService service;

    @Test
    void rechazaUnaConfiguracionQueNoSuma100() {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.configurar(peticion(
                        new PonderacionItem(TipoEvaluacion.EXAMEN, 60),
                        new PonderacionItem(TipoEvaluacion.QUIZ, 30))));

        assertEquals("RB-07", error.getCodigoRegla());
        verify(ponderacionRepository, never()).deleteByMateriaIdAndPeriodoAcademicoId(MATERIA, PERIODO);
    }

    @Test
    void rechazaPorcentajesNegativosAunqueLaSumaCuadre() {
        // 120 - 20 suma 100, pero una ponderacion negativa resta del promedio.
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.configurar(peticion(
                        new PonderacionItem(TipoEvaluacion.EXAMEN, 120),
                        new PonderacionItem(TipoEvaluacion.QUIZ, -20))));

        assertEquals("RB-07", error.getCodigoRegla());
    }

    @Test
    void rechazaElMismoTipoDeEvaluacionRepetido() {
        // Pasa la suma (50+50) pero deja dos filas del mismo tipo: el promedio
        // ponderado contaria dos veces los examenes y la materia se quedaria sin
        // el 100% real que exige RB-07.
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.configurar(peticion(
                        new PonderacionItem(TipoEvaluacion.EXAMEN, 50),
                        new PonderacionItem(TipoEvaluacion.EXAMEN, 50))));

        assertEquals("RB-07", error.getCodigoRegla());
        verify(ponderacionRepository, never()).deleteByMateriaIdAndPeriodoAcademicoId(MATERIA, PERIODO);
    }

    @Test
    void aceptaUnaConfiguracionValidaYLaDejaRegistradaEnAuditoria() {
        service.configurar(peticion(
                new PonderacionItem(TipoEvaluacion.EXAMEN, 60),
                new PonderacionItem(TipoEvaluacion.QUIZ, 40)));

        verify(ponderacionRepository).deleteByMateriaIdAndPeriodoAcademicoId(MATERIA, PERIODO);
        // HU-10: cambiar la ponderacion recalcula todos los promedios de la
        // materia, de modo que es una operacion critica aunque no toque notas.
        verify(auditoria).registrar(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq("materia"),
                org.mockito.ArgumentMatchers.eq(MATERIA),
                org.mockito.ArgumentMatchers.anyString());
    }

    private static ConfigurarPonderacionRequest peticion(PonderacionItem... items) {
        return new ConfigurarPonderacionRequest(MATERIA, PERIODO, List.of(items));
    }
}
