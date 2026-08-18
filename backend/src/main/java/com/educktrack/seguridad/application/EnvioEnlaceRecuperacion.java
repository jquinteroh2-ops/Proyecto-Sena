package com.educktrack.seguridad.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Entrega al usuario el enlace de recuperacion por correo institucional
 * (RF-64, HU-04).
 *
 * <h2>Por que no usa el modulo de notificaciones</h2>
 *
 * <p>{@code NotificacionService.notificar} siempre deja copia en la bandeja
 * interna y solo envia correo si el canal configurado lo incluye. Ninguna de
 * las dos cosas sirve aqui:</p>
 *
 * <ul>
 *   <li>La bandeja interna se lee <em>entrando al sistema</em>, que es
 *       exactamente lo que esta persona no puede hacer.</li>
 *   <li>Un enlace vivo guardado en la bandeja es una credencial almacenada en
 *       claro, legible por cualquiera que consulte esa tabla.</li>
 *   <li>El canal es configurable (RF-52) y su valor por defecto es
 *       {@code INTERNO}: el aviso mas importante del sistema no puede depender
 *       de una preferencia que puede dejarlo sin enviar.</li>
 * </ul>
 *
 * <p>Por eso el enlace va por correo directo y no queda registrado en ningun
 * sitio salvo su hash (V13).</p>
 */
@Component
public class EnvioEnlaceRecuperacion {

    private static final Logger log = LoggerFactory.getLogger(EnvioEnlaceRecuperacion.class);

    private final JavaMailSender mailSender;
    private final String urlBase;
    private final String remitente;

    public EnvioEnlaceRecuperacion(JavaMailSender mailSender,
                                   @Value("${educktrack.seguridad.recuperacion.url-base:http://localhost:5173/restablecer-password}")
                                   String urlBase,
                                   @Value("${educktrack.correo.remitente:no-reply@educktrack.edu.co}")
                                   String remitente) {
        this.mailSender = mailSender;
        this.urlBase = urlBase;
        this.remitente = remitente;
    }

    /**
     * Envia el enlace. Devuelve {@code true} si el correo salio.
     *
     * <p>El fallo se registra pero no se propaga: la respuesta al solicitante es
     * siempre la misma (RF-64), de modo que dejar escapar la excepcion
     * convertiria un error de infraestructura en la confirmacion de que ese
     * correo si existe.</p>
     */
    public boolean enviar(String correoDestino, String token, int minutosValidez) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            // JavaMail falla sin remitente, y Gmail exige ademas que coincida
            // con la cuenta autenticada o uno de sus alias.
            msg.setFrom(remitente);
            msg.setTo(correoDestino);
            msg.setSubject("Recuperacion de contrasena - EduckTrack");
            msg.setText("Recibimos una solicitud para restablecer la contrasena de esta cuenta.\n\n"
                    + urlBase + "?token=" + token + "\n\n"
                    + "El enlace caduca en " + minutosValidez + " minutos y solo puede usarse una vez.\n"
                    + "Si no solicitaste el cambio, ignora este mensaje: tu contrasena actual sigue siendo valida.");
            mailSender.send(msg);
            return true;
        } catch (Exception ex) {
            // Sin el correo la persona se queda sin poder recuperar la cuenta, de
            // modo que esto es un fallo real y se registra como tal, no como aviso.
            log.error("No se pudo enviar el enlace de recuperacion: {}", ex.getMessage());
            return false;
        }
    }
}
