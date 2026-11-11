package com.elegance.notification.listener;

import com.elegance.notification.event.*;
import com.elegance.notification.service.EmailService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final EmailService emailService;

    @Autowired
    public NotificationEventListener(EmailService emailService) {
        this.emailService = emailService;
    }

    // ==================== USER EVENTS ====================

    @RabbitListener(queues = "${rabbitmq.queue.user.created:elegance.user.created.queue}", containerFactory = "rabbitListenerContainerFactory")
    public void handleUserCreated(UserCreatedEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("📩 Recibido evento user.created -> {}", event);
        try {
            emailService.sendWelcomeEmail(event.email(), event.name());
            channel.basicAck(deliveryTag, false);
            log.info("✅ user.created ACKed deliveryTag={}", deliveryTag);
        } catch (Exception e) {
            log.error("❌ Error al procesar user.created, enviando a DLQ", e);
            try { channel.basicNack(deliveryTag, false, false); } catch (Exception ex) { log.error("Error al basicNack", ex); }
        }
    }

    @RabbitListener(queues = "${rabbitmq.queue.user.updated:elegance.user.updated.queue}", containerFactory = "rabbitListenerContainerFactory")
    public void handleUserUpdated(UserUpdatedEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("📩 Recibido evento user.updated -> {}", event);
        try {
            // opcional: acciones a realizar en actualización
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("❌ Error al procesar user.updated", e);
            try { channel.basicNack(deliveryTag, false, false); } catch (Exception ex) { log.error("Error al basicNack", ex); }
        }
    }

    @RabbitListener(queues = "${rabbitmq.queue.user.deleted:elegance.user.deleted.queue}", containerFactory = "rabbitListenerContainerFactory")
    public void handleUserDeleted(UserDeletedEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("📩 Recibido evento user.deleted -> {}", event);
        try {
            // opcional: limpieza, notificaciones
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("❌ Error al procesar user.deleted", e);
            try { channel.basicNack(deliveryTag, false, false); } catch (Exception ex) { log.error("Error al basicNack", ex); }
        }
    }


    // ==================== STYLIST EVENTS ====================

    @RabbitListener(queues = "${rabbitmq.queue.stylist.created:elegance.stylist.created.queue}", containerFactory = "rabbitListenerContainerFactory")
    public void handleStylistCreated(StylistCreatedEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("📩 Recibido evento stylist.created -> {}", event);
        try {
            emailService.sendWelcomeStylistEmail(event.name(), null);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("❌ Error al procesar stylist.created, enviando a DLQ", e);
            try { channel.basicNack(deliveryTag, false, false); } catch (Exception ex) { log.error("Error al basicNack", ex); }
        }
    }

    @RabbitListener(queues = "${rabbitmq.queue.stylist.updated:elegance.stylist.updated.queue}", containerFactory = "rabbitListenerContainerFactory")
    public void handleStylistUpdated(StylistUpdatedEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("📩 Recibido evento stylist.updated -> {}", event);
        try {
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("❌ Error al procesar stylist.updated", e);
            try { channel.basicNack(deliveryTag, false, false); } catch (Exception ex) { log.error("Error al basicNack", ex); }
        }
    }

    @RabbitListener(queues = "${rabbitmq.queue.stylist.deleted:elegance.stylist.deleted.queue}", containerFactory = "rabbitListenerContainerFactory")
    public void handleStylistDeleted(StylistDeletedEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("📩 Recibido evento stylist.deleted -> {}", event);
        try {
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("❌ Error al procesar stylist.deleted", e);
            try { channel.basicNack(deliveryTag, false, false); } catch (Exception ex) { log.error("Error al basicNack", ex); }
        }
    }

    // ==================== SERVICE EVENTS ====================

    @RabbitListener(queues = "${rabbitmq.queue.service.created:elegance.service.created.queue}", containerFactory = "rabbitListenerContainerFactory")
    public void handleServiceCreated(ServiceCreatedEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("📩 Recibido evento service.created -> {}", event);
        try {
            // notificaciones opcionales
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("❌ Error al procesar service.created", e);
            try { channel.basicNack(deliveryTag, false, false); } catch (Exception ex) { log.error("Error al basicNack", ex); }
        }
    }

    @RabbitListener(queues = "${rabbitmq.queue.service.updated:elegance.service.updated.queue}", containerFactory = "rabbitListenerContainerFactory")
    public void handleServiceUpdated(ServiceUpdatedEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("📩 Recibido evento service.updated -> {}", event);
        try {
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("❌ Error al procesar service.updated", e);
            try { channel.basicNack(deliveryTag, false, false); } catch (Exception ex) { log.error("Error al basicNack", ex); }
        }
    }

    @RabbitListener(queues = "${rabbitmq.queue.service.deleted:elegance.service.deleted.queue}", containerFactory = "rabbitListenerContainerFactory")
    public void handleServiceDeleted(ServiceDeletedEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("📩 Recibido evento service.deleted -> {}", event);
        try {
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("❌ Error al procesar service.deleted", e);
            try { channel.basicNack(deliveryTag, false, false); } catch (Exception ex) { log.error("Error al basicNack", ex); }
        }
    }
}
