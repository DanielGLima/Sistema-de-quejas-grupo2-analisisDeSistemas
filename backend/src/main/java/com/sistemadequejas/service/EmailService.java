package com.sistemadequejas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

// Envio de correo generico, reutilizable para CU-02 (codigo de recuperacion),
// CU-05 (confirmacion de caso) y, mas adelante, CU-14 (notificaciones).
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String remitente;

    public void enviar(String destinatario, String asunto, String cuerpo) {
        if (remitente == null || remitente.isBlank()) {
            // Sin credenciales configuradas (MAIL_USERNAME/MAIL_PASSWORD): no se
            // bloquea la operacion que disparo el correo, solo se deja constancia.
            log.warn("Envio de correo omitido (no hay MAIL_USERNAME configurado). Destinatario: {}, asunto: {}", destinatario, asunto);
            return;
        }

        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente);
            mensaje.setTo(destinatario);
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);
            mailSender.send(mensaje);
        } catch (Exception e) {
            // CU-14, FA01: un fallo de envio no debe bloquear la operacion original.
            log.error("No se pudo enviar el correo a {}: {}", destinatario, e.getMessage());
        }
    }
}
