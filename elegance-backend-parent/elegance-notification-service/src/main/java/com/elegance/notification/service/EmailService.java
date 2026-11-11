package com.elegance.notification.service;

import com.elegance.notification.dto.EmailRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    public void sendEmail(EmailRequest request) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(request.getTo());
        message.setSubject(request.getSubject());
        message.setText(request.getBody());

        mailSender.send(message);
    }

    // Convenience helper used by NotificationEventListener
    public void sendWelcomeEmail(String to, String name) {
        if (to == null || to.isBlank()) {
            log.warn("sendWelcomeEmail called with null/empty 'to' address for user '{}', skipping send", name);
            return;
        }
        EmailRequest req = new EmailRequest();
        req.setTo(to);
        req.setSubject("Bienvenido a Elegance, " + (name == null ? "" : name));
        req.setBody(buildWelcomeUserBody(name));
        sendEmail(req);
    }

    // Convenience helper for stylist welcome. Email may be null; in that case we just log.
    public void sendWelcomeStylistEmail(String name, String email) {
        if (email == null || email.isBlank()) {
            log.info("No email for stylist '{}', skipping sendWelcomeStylistEmail", name);
            return;
        }
        EmailRequest req = new EmailRequest();
        req.setTo(email);
        req.setSubject("Bienvenido como estilista a Elegance, " + (name == null ? "" : name));
        req.setBody(buildWelcomeStylistBody(name));
        sendEmail(req);
    }

    private String buildWelcomeUserBody(String name) {
        String displayName = name == null ? "" : name;
        return "Hola " + displayName + ",\n\n" +
               "Gracias por unirte a Elegance. Estamos encantados de tenerte con nosotros.\n\n" +
               "Saludos,\nEl equipo de Elegance";
    }

    private String buildWelcomeStylistBody(String name) {
        String displayName = name == null ? "" : name;
        return "Hola " + displayName + ",\n\n" +
               "Gracias por unirte al equipo de estilistas de Elegance. Pronto nos pondremos en contacto para completar tu registro.\n\n" +
               "Saludos,\nEl equipo de Elegance";
    }
}
