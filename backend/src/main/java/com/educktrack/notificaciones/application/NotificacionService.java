package com.educktrack.notificaciones.application;

import com.educktrack.notificaciones.domain.CanalNotificacion;
import com.educktrack.notificaciones.domain.TipoNotificacion;
import com.educktrack.notificaciones.infrastructure.persistence.ConfiguracionNotificacionJpaEntity;
import com.educktrack.notificaciones.infrastructure.persistence.ConfiguracionNotificacionRepository;
import com.educktrack.notificaciones.infrastructure.persistence.NotificacionJpaEntity;
import com.educktrack.notificaciones.infrastructure.persistence.NotificacionRepository;
import com.educktrack.notificaciones.infrastructure.rest.NotificacionDtos.BandejaDto;
import com.educktrack.notificaciones.infrastructure.rest.NotificacionDtos.NotificacionDto;
import com.educktrack.shared.domain.ReglaNegocioException;
import com.educktrack.usuarios.infrastructure.persistence.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Casos de uso de notificaciones (RF-52..RF-56). Registra notificaciones
 * internas y, segun el canal configurado (RF-52), intenta el envio por correo
 * (RS-08). Expone {@link #notificar} para que el sistema genere alertas
 * automaticas (RB-13, RF-55, RF-56).
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);
    private static final Integer CONFIG_ID = 1;

    private final NotificacionRepository notificacionRepository;
    private final ConfiguracionNotificacionRepository configRepository;
    private final UsuarioRepository usuarioRepository;
    private final JavaMailSender mailSender;

    public NotificacionService(NotificacionRepository notificacionRepository,
                               ConfiguracionNotificacionRepository configRepository,
                               UsuarioRepository usuarioRepository,
                               JavaMailSender mailSender) {
        this.notificacionRepository = notificacionRepository;
        this.configRepository = configRepository;
        this.usuarioRepository = usuarioRepository;
        this.mailSender = mailSender;
    }

    /** RF-52 / HU-28: configura el canal institucional. */
    @Transactional
    public CanalNotificacion configurarCanal(CanalNotificacion canal) {
        ConfiguracionNotificacionJpaEntity config = configRepository.findById(CONFIG_ID)
                .orElseGet(ConfiguracionNotificacionJpaEntity::new);
        config.setId(CONFIG_ID);
        config.setCanal(canal);
        return configRepository.save(config).getCanal();
    }

    @Transactional(readOnly = true)
    public CanalNotificacion canalActual() {
        return configRepository.findById(CONFIG_ID)
                .map(ConfiguracionNotificacionJpaEntity::getCanal)
                .orElse(CanalNotificacion.INTERNO);
    }

    /** RF-53: envia una notificacion manual a varios destinatarios. */
    @Transactional
    public int enviarManual(List<Long> destinatarios, String titulo, String mensaje) {
        destinatarios.forEach(id -> notificar(id, titulo, mensaje, TipoNotificacion.GENERAL));
        return destinatarios.size();
    }

    /**
     * Genera una notificacion (interna + correo segun canal). Punto de entrada
     * para las alertas automaticas del sistema (RB-13, RF-30, RF-42, RF-55, RF-56).
     */
    @Transactional
    public NotificacionDto notificar(Long usuarioId, String titulo, String mensaje, TipoNotificacion tipo) {
        NotificacionJpaEntity n = new NotificacionJpaEntity();
        n.setDestinatarioUsuarioId(usuarioId);
        n.setTitulo(titulo);
        n.setMensaje(mensaje);
        n.setTipo(tipo);
        n.setLeida(false);
        n.setFechaCreacion(LocalDateTime.now());
        NotificacionJpaEntity guardada = notificacionRepository.save(n);

        CanalNotificacion canal = canalActual();
        if (canal == CanalNotificacion.CORREO || canal == CanalNotificacion.AMBOS) {
            enviarCorreoBestEffort(usuarioId, titulo, mensaje);
        }
        return toDto(guardada);
    }

    /** RF-54 / HU-27: bandeja del usuario con contador de no leidas. */
    @Transactional(readOnly = true)
    public BandejaDto bandeja(Long usuarioId) {
        List<NotificacionDto> lista = notificacionRepository
                .findByDestinatarioUsuarioIdOrderByFechaCreacionDesc(usuarioId)
                .stream().map(NotificacionService::toDto).toList();
        long noLeidas = notificacionRepository.countByDestinatarioUsuarioIdAndLeidaFalse(usuarioId);
        return new BandejaDto(noLeidas, lista);
    }

    /** HU-27: marca una notificacion como leida. */
    @Transactional
    public void marcarLeida(Long id) {
        NotificacionJpaEntity n = notificacionRepository.findById(id)
                .orElseThrow(() -> new ReglaNegocioException("RF-54", "La notificacion no existe."));
        n.setLeida(true);
        notificacionRepository.save(n);
    }

    private void enviarCorreoBestEffort(Long usuarioId, String titulo, String mensaje) {
        try {
            String correo = usuarioRepository.findById(usuarioId)
                    .map(u -> u.getCorreoInstitucional()).orElse(null);
            if (correo == null) {
                return;
            }
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(correo);
            msg.setSubject(titulo);
            msg.setText(mensaje);
            mailSender.send(msg);
        } catch (Exception ex) {
            // DECISION DE DISENO: el correo es best-effort; su fallo no invalida
            // la notificacion interna ya registrada (RS-08).
            log.warn("No se pudo enviar el correo de notificacion al usuario {}: {}", usuarioId, ex.getMessage());
        }
    }

    private static NotificacionDto toDto(NotificacionJpaEntity e) {
        return new NotificacionDto(e.getId(), e.getDestinatarioUsuarioId(), e.getTitulo(), e.getMensaje(),
                e.getTipo(), e.getTipo().esCritica(), e.isLeida(), e.getFechaCreacion());
    }
}
