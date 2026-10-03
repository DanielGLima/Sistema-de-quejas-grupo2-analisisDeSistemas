package com.sistemadequejas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

// Envio de correo generico, reutilizable para CU-02 (codigo de recuperacion),
// CU-05 (confirmacion de caso) y CU-14 (notificaciones).
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String remitente;

    public boolean estaConfigurado() {
        return remitente != null && !remitente.isBlank();
    }

    // Devuelve true solo si el correo realmente salio hacia el servidor SMTP.
    // Nunca lanza excepcion: un fallo de envio no debe bloquear la operacion original (CU-14, FA01).
    public boolean enviar(String destinatario, String asunto, String cuerpo) {
        if (!estaConfigurado()) {
            log.warn("Envio de correo omitido (no hay MAIL_USERNAME configurado). Destinatario: {}, asunto: {}", destinatario, asunto);
            return false;
        }

        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente);
            mensaje.setTo(destinatario);
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);
            mailSender.send(mensaje);
            return true;
        } catch (Exception e) {
            log.error("No se pudo enviar el correo a {}: {}", destinatario, e.getMessage());
            return false;
        }
    }
}
