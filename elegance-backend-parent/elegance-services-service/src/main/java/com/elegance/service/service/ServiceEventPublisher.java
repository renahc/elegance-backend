package com.elegance.service.service;

import com.elegance.service.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class ServiceEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(ServiceEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;
    private final String exchange;

    public ServiceEventPublisher(RabbitTemplate rabbitTemplate, @Value("${rabbitmq.exchange:elegance.domain.exchange}") String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishServiceCreated(ServiceCreatedEvent event) {
        log.info("📤 Publicando evento: service.created -> {}", event);
        rabbitTemplate.convertAndSend(exchange, "service.created", event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishServiceUpdated(ServiceUpdatedEvent event) {
        log.info("📤 Publicando evento: service.updated -> {}", event);
        rabbitTemplate.convertAndSend(exchange, "service.updated", event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishServiceDeleted(ServiceDeletedEvent event) {
        log.info("📤 Publicando evento: service.deleted -> {}", event);
        rabbitTemplate.convertAndSend(exchange, "service.deleted", event);
    }
}
