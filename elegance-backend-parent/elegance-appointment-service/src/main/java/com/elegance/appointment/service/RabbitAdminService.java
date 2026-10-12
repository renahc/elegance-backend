package com.elegance.appointment.service;

import com.elegance.appointment.dto.BindingDefinitionRequest;
import com.elegance.appointment.dto.ExchangeDefinitionRequest;
import com.elegance.appointment.dto.QueueDefinitionRequest;
import com.elegance.appointment.dto.QueueInfoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Service
public class RabbitAdminService {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminService.class);

    private final RabbitAdmin rabbitAdmin;

    @Autowired
    public RabbitAdminService(RabbitAdmin rabbitAdmin) {
        this.rabbitAdmin = rabbitAdmin;
    }

    /**
     * Declara o crea una nueva cola dinámicamente en RabbitMQ.
     */
    public String createQueue(QueueDefinitionRequest request) {
        log.info("Declarando cola: {} (durable={}, exclusive={}, autoDelete={})",
                request.getName(), request.isDurable(), request.isExclusive(), request.isAutoDelete());

        QueueBuilder builder = request.isDurable()
                ? QueueBuilder.durable(request.getName())
                : QueueBuilder.nonDurable(request.getName());

        if (request.isExclusive()) {
            builder = builder.exclusive();
        }
        if (request.isAutoDelete()) {
            builder = builder.autoDelete();
        }
        if (request.getDeadLetterExchange() != null && !request.getDeadLetterExchange().isBlank()) {
            builder = builder.withArgument("x-dead-letter-exchange", request.getDeadLetterExchange());
        }
        if (request.getDeadLetterRoutingKey() != null && !request.getDeadLetterRoutingKey().isBlank()) {
            builder = builder.withArgument("x-dead-letter-routing-key", request.getDeadLetterRoutingKey());
        }
        if (request.getArguments() != null) {
            for (Map.Entry<String, Object> entry : request.getArguments().entrySet()) {
                builder = builder.withArgument(entry.getKey(), entry.getValue());
            }
        }

        Queue queue = builder.build();
        String declaredName = rabbitAdmin.declareQueue(queue);
        log.info("Cola '{}' declarada exitosamente en el clúster RabbitMQ", declaredName);
        return declaredName;
    }

    /**
     * Obtiene estadísticas e información de una cola existente.
     */
    public QueueInfoResponse getQueueInfo(String queueName) {
        Properties properties = rabbitAdmin.getQueueProperties(queueName);
        if (properties == null) {
            log.warn("La cola '{}' no existe o no tiene propiedades disponibles.", queueName);
            return QueueInfoResponse.builder()
                    .name(queueName)
                    .exists(false)
                    .messageCount(0)
                    .consumerCount(0)
                    .arguments(Map.of())
                    .build();
        }

        Integer messageCount = (Integer) properties.get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
        Integer consumerCount = (Integer) properties.get(RabbitAdmin.QUEUE_CONSUMER_COUNT);

        Map<String, Object> arguments = new HashMap<>();
        properties.forEach((k, v) -> arguments.put(k.toString(), v));

        return QueueInfoResponse.builder()
                .name(queueName)
                .exists(true)
                .messageCount(messageCount != null ? messageCount : 0)
                .consumerCount(consumerCount != null ? consumerCount : 0)
                .arguments(arguments)
                .build();
    }

    /**
     * Elimina una cola existente en RabbitMQ.
     */
    public boolean deleteQueue(String queueName, boolean ifUnused, boolean ifEmpty) {
        log.info("Eliminando cola: {} (ifUnused={}, ifEmpty={})", queueName, ifUnused, ifEmpty);
        if (ifUnused || ifEmpty) {
            rabbitAdmin.deleteQueue(queueName, ifUnused, ifEmpty);
            return true;
        }
        return rabbitAdmin.deleteQueue(queueName);
    }

    /**
     * Purga todos los mensajes pendientes de una cola.
     */
    public void purgeQueue(String queueName) {
        log.info("Purgando mensajes en la cola: {}", queueName);
        rabbitAdmin.purgeQueue(queueName, false);
    }

    /**
     * Declara o crea un nuevo exchange dinámicamente en RabbitMQ.
     */
    public void createExchange(ExchangeDefinitionRequest request) {
        log.info("Declarando exchange: {} (tipo={}, durable={}, autoDelete={})",
                request.getName(), request.getType(), request.isDurable(), request.isAutoDelete());

        Exchange exchange;
        String type = request.getType().toLowerCase();

        switch (type) {
            case "topic":
                exchange = new TopicExchange(request.getName(), request.isDurable(), request.isAutoDelete(), request.getArguments());
                break;
            case "fanout":
                exchange = new FanoutExchange(request.getName(), request.isDurable(), request.isAutoDelete(), request.getArguments());
                break;
            case "headers":
                exchange = new HeadersExchange(request.getName(), request.isDurable(), request.isAutoDelete(), request.getArguments());
                break;
            case "direct":
            default:
                exchange = new DirectExchange(request.getName(), request.isDurable(), request.isAutoDelete(), request.getArguments());
                break;
        }

        rabbitAdmin.declareExchange(exchange);
        log.info("Exchange '{}' ({}) declarado exitosamente en RabbitMQ", request.getName(), type);
    }

    /**
     * Elimina un exchange de RabbitMQ.
     */
    public boolean deleteExchange(String exchangeName) {
        log.info("Eliminando exchange: {}", exchangeName);
        return rabbitAdmin.deleteExchange(exchangeName);
    }

    /**
     * Crea un enlace (binding) entre una cola y un exchange con su routing key.
     */
    public void createBinding(BindingDefinitionRequest request) {
        log.info("Creando binding: Cola '{}' <-> Exchange '{}' [RoutingKey: '{}']",
                request.getQueueName(), request.getExchangeName(), request.getRoutingKey());

        Binding binding = new Binding(
                request.getQueueName(),
                Binding.DestinationType.QUEUE,
                request.getExchangeName(),
                request.getRoutingKey(),
                request.getArguments()
        );

        rabbitAdmin.declareBinding(binding);
        log.info("Binding creado exitosamente.");
    }

    /**
     * Elimina un enlace (binding) existente.
     */
    public void deleteBinding(BindingDefinitionRequest request) {
        log.info("Eliminando binding: Cola '{}' <-> Exchange '{}' [RoutingKey: '{}']",
                request.getQueueName(), request.getExchangeName(), request.getRoutingKey());

        Binding binding = new Binding(
                request.getQueueName(),
                Binding.DestinationType.QUEUE,
                request.getExchangeName(),
                request.getRoutingKey(),
                request.getArguments()
        );

        rabbitAdmin.removeBinding(binding);
        log.info("Binding eliminado exitosamente.");
    }
}
