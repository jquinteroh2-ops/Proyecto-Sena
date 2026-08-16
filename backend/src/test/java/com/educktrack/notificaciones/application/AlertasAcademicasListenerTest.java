package com.educktrack.notificaciones.application;

import com.educktrack.asistencia.domain.evento.EventosDeAsistencia.AsistenciaBajoMinimo;
import com.educktrack.estudiantes.domain.evento.EventosDeEstudiantes.EstudianteRetirado;
import com.educktrack.notas.domain.evento.EventosDeNotas.CorteCerrado;
import com.educktrack.notas.domain.evento.EventosDeNotas.NotaBajaRegistrada;
import com.educktrack.notificaciones.domain.TipoNotificacion;
import com.educktrack.usuarios.domain.evento.EventosDeUsuarios.UsuarioDesactivado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de la traduccion de hechos academicos a avisos (RS-08).
 *
 * <p>Lo que se comprueba es a quien llega cada aviso: equivocarse de
 * destinatario en un sistema escolar significa contarle a una familia algo de
 * otro menor.</p>
 */
@ExtendWith(MockitoExtension.class)
class AlertasAcademicasListenerTest {

    @Mock private NotificacionService notificaciones;
    @Mock private DestinatariosService destinatarios;

    @InjectMocks private AlertasAcademicasListener listener;

    @Test
    void elBajoRendimientoLlegaAlEstudianteYASusAcudientes() {
        when(destinatarios.delEstudianteYSusAcudientes(10L)).thenReturn(Set.of(100L, 200L));

        listener.alRegistrarUnaNotaBaja(new NotaBajaRegistrada(10L, 7L, 1L, 1L, 2.5));

        // RB-13: lo que le pasa a un menor le importa a quien responde por el.
        verify(notificaciones).notificar(eq(100L), anyString(), anyString(),
                eq(TipoNotificacion.BAJO_RENDIMIENTO));
        verify(notificaciones).notificar(eq(200L), anyString(), anyString(),
                eq(TipoNotificacion.BAJO_RENDIMIENTO));
    }

    @Test
    void laBajaAsistenciaSeMarcaComoCriticaEnLaBandeja() {
        when(destinatarios.delEstudianteYSusAcudientes(10L)).thenReturn(Set.of(100L));

        listener.alCaerBajoElMinimoDeAsistencia(new AsistenciaBajoMinimo(10L, 7L, 1L, 72.5));

        // HU-27: BAJA_ASISTENCIA es de las que se resaltan.
        verify(notificaciones).notificar(eq(100L), anyString(), anyString(),
                eq(TipoNotificacion.BAJA_ASISTENCIA));
    }

    @Test
    void elCierreDeCorteAvisaADosPublicosPorMotivosDistintos() {
        when(destinatarios.deLosDocentesDelCurso(5L)).thenReturn(Set.of(300L));
        when(destinatarios.delCursoCompleto(5L)).thenReturn(Set.of(100L, 200L));

        listener.alCerrarUnCorte(new CorteCerrado(5L, 1L));

        // RF-55: al docente, que ya no puede modificar notas.
        verify(notificaciones).notificar(eq(300L), anyString(), anyString(),
                eq(TipoNotificacion.CIERRE_PERIODO));
        // RF-56: a estudiantes y acudientes, que ya pueden ver el boletin.
        verify(notificaciones).notificar(eq(100L), anyString(), anyString(),
                eq(TipoNotificacion.BOLETIN_DISPONIBLE));
        verify(notificaciones).notificar(eq(200L), anyString(), anyString(),
                eq(TipoNotificacion.BOLETIN_DISPONIBLE));
    }

    @Test
    void elRetiroSeComunicaSoloAlAcudienteYNoAlEstudiante() {
        when(destinatarios.deLosAcudientes(10L)).thenReturn(Set.of(200L));

        listener.alRetirarUnEstudiante(new EstudianteRetirado(10L, "Ana Perez", "Traslado de ciudad"));

        // HU-07 dirige el aviso al acudiente; no se usa el conjunto que
        // incluiria tambien la cuenta del estudiante.
        verify(destinatarios, never()).delEstudianteYSusAcudientes(any());
        verify(notificaciones).notificar(eq(200L), anyString(), anyString(), eq(TipoNotificacion.GENERAL));
    }

    @Test
    void laDesactivacionAvisaALaPropiaCuentaAfectada() {
        listener.alDesactivarUnaCuenta(new UsuarioDesactivado(42L, "ana@colegio.edu.co", "Ana"));

        // HU-02: se entera la persona afectada, no un tercero.
        verify(notificaciones).notificar(eq(42L), anyString(), anyString(), eq(TipoNotificacion.GENERAL));
    }

    @Test
    void unPerfilSinCuentaAsociadaNoProvocaNingunEnvio() {
        // El vinculo usuario-perfil es opcional (V9): un estudiante puede estar
        // matriculado antes de que le creen cuenta.
        when(destinatarios.delEstudianteYSusAcudientes(10L)).thenReturn(Set.of());

        listener.alRegistrarUnaNotaBaja(new NotaBajaRegistrada(10L, 7L, 1L, 1L, 2.0));

        verify(notificaciones, never()).notificar(any(), anyString(), anyString(), any());
    }

    @Test
    void unFalloDeCanalNoImpideAvisarALosDemasDestinatarios() {
        when(destinatarios.delEstudianteYSusAcudientes(10L)).thenReturn(Set.of(100L, 200L));
        when(notificaciones.notificar(eq(100L), anyString(), anyString(), any()))
                .thenThrow(new IllegalStateException("canal caido"));

        assertDoesNotThrow(() -> listener.alRegistrarUnaNotaBaja(
                new NotaBajaRegistrada(10L, 7L, 1L, 1L, 2.0)));

        // El segundo destinatario recibe el aviso pese al fallo del primero.
        verify(notificaciones, times(2)).notificar(any(), anyString(), anyString(), any());
    }
}
