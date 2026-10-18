package com.elegance.user.service;

import com.elegance.user.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Service
public class UserEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(UserEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;

    public UserEventPublisher(RabbitTemplate rabbitTemplate,
                              @Value("${rabbitmq.exchange:elegance.domain.exchange}") String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishUserCreated(UserCreatedEvent event) {
        log.info("📤 Publicando evento: user.created -> {}", event);
        rabbitTemplate.convertAndSend(exchange, "user.created", event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishUserUpdated(UserUpdatedEvent event) {
        log.info("📤 Publicando evento: user.updated -> {}", event);
        rabbitTemplate.convertAndSend(exchange, "user.updated", event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishUserDeleted(UserDeletedEvent event) {
        log.info("📤 Publicando evento: user.deleted -> {}", event);
        rabbitTemplate.convertAndSend(exchange, "user.deleted", event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishStylistCreated(StylistCreatedEvent event) {
        log.info("📤 Publicando evento: stylist.created -> {}", event);
        rabbitTemplate.convertAndSend(exchange, "stylist.created", event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishStylistUpdated(StylistUpdatedEvent event) {
        log.info("📤 Publicando evento: stylist.updated -> {}", event);
        rabbitTemplate.convertAndSend(exchange, "stylist.updated", event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishStylistDeleted(StylistDeletedEvent event) {
        log.info("📤 Publicando evento: stylist.deleted -> {}", event);
        rabbitTemplate.convertAndSend(exchange, "stylist.deleted", event);
    }
}
