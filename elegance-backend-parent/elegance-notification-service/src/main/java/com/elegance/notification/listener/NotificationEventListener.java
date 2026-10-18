package com.elegance.notification.listener;

import com.elegance.notification.event.ServiceCreatedEvent;
import com.elegance.notification.event.StylistCreatedEvent;
import com.elegance.notification.event.UserCreatedEvent;
import com.elegance.notification.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);
    private final EmailService emailService;

    public NotificationEventListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @RabbitListener(queues = "elegance.user.created.queue")
    public void handleUserCreated(UserCreatedEvent event) {
        log.info("📩 Recibido evento user.created. Enviando bienvenida a: {}", event.email());
        try {
            emailService.sendWelcomeEmail(event.email(), event.name());
        } catch (Exception e) {
            log.error("Fallo al enviar email. Lanzando RuntimeException para enviar a DLQ", e);
            throw new RuntimeException(e);
        }
    }

    @RabbitListener(queues = "elegance.stylist.created.queue")
    public void handleStylistCreated(StylistCreatedEvent event) {
        log.info("📩 Recibido evento stylist.created. Enviando bienvenida a estilista: {}", event.name());
        try {
            emailService.sendWelcomeStylistEmail(event.name(), null);
        } catch (Exception e) {
            log.error("Fallo al enviar email a estilista. Lanzando RuntimeException para enviar a DLQ", e);
            throw new RuntimeException(e);
        }
    }

    @RabbitListener(queues = "elegance.service.created.queue")
    public void handleServiceCreated(ServiceCreatedEvent event) {
        log.info("📩 Recibido evento service.created. Evento para servicio: {}", event.name());
        try {
            // aquí podrías notificar a usuarios suscritos
        } catch (Exception e) {
            log.error("Fallo en manejo de service.created. Lanzando RuntimeException para enviar a DLQ", e);
            throw new RuntimeException(e);
        }
    }
}
