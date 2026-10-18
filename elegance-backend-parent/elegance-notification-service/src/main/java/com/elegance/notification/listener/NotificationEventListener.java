package com.elegance.notification.listener;

import com.elegance.notification.event.ServiceCreatedEvent;
import com.elegance.notification.event.StylistCreatedEvent;
import com.elegance.notification.event.UserCreatedEvent;
import com.elegance.notification.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final EmailService emailService;

    @Autowired
    public NotificationEventListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @RabbitListener(queues = "${rabbitmq.queue.user.created:elegance.user.created.queue}", containerFactory = "rabbitListenerContainerFactory")
    public void handleUserCreated(UserCreatedEvent event) {
        log.info("📩 Recibido evento user.created. Enviando bienvenida a: {}", event.email());
        try {
            emailService.sendWelcomeEmail(event.email(), event.name());
        } catch (Exception e) {
            log.error("❌ Fallo al enviar email de bienvenida. El mensaje irá a la DLQ.", e);
            throw new RuntimeException("Fallo en envío de email de bienvenida", e);
        }
    }

    @RabbitListener(queues = "${rabbitmq.queue.stylist.created:elegance.stylist.created.queue}", containerFactory = "rabbitListenerContainerFactory")
    public void handleStylistCreated(StylistCreatedEvent event) {
        log.info("📩 Recibido evento stylist.created. Enviando bienvenida al estilista: {}", event.name());
        try {
            emailService.sendWelcomeStylistEmail(event.name(), null);
        } catch (Exception e) {
            log.error("❌ Fallo al enviar email al estilista.", e);
            throw new RuntimeException("Fallo en envío de email al estilista", e);
        }
    }

    @RabbitListener(queues = "${rabbitmq.queue.service.created:elegance.service.created.queue}", containerFactory = "rabbitListenerContainerFactory")
    public void handleServiceCreated(ServiceCreatedEvent event) {
        log.info("📩 Recibido evento service.created. Nuevo servicio: {} (${})", event.name(), event.price());
        // Lógica opcional: notificar a clientes interesados o actualizar caché
    }
}
