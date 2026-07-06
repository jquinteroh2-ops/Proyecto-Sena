package com.educktrack.notificaciones.infrastructure.rest;

import com.educktrack.notificaciones.domain.CanalNotificacion;
import com.educktrack.notificaciones.domain.TipoNotificacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs del modulo de notificaciones (RF-52..RF-56).
 */
public final class NotificacionDtos {

    private NotificacionDtos() {
    }

    /** RF-52 / HU-28: configurar canal institucional. */
    public record ConfigurarCanalRequest(
            @NotNull(message = "El canal es obligatorio") CanalNotificacion canal) {
    }

    /** RF-53: envio manual de notificacion. */
    public record EnviarManualRequest(
            @NotEmpty(message = "Debe indicar al menos un destinatario") List<Long> destinatariosUsuarioId,
            @NotBlank(message = "El titulo es obligatorio") String titulo,
            @NotBlank(message = "El mensaje es obligatorio") String mensaje) {
    }

    public record NotificacionDto(
            Long id, Long destinatarioUsuarioId, String titulo, String mensaje,
            TipoNotificacion tipo, boolean critica, boolean leida, LocalDateTime fechaCreacion) {
    }

    public record BandejaDto(long noLeidas, List<NotificacionDto> notificaciones) {
    }
}
