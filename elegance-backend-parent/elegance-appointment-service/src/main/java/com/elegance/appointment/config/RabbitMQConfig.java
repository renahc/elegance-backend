package com.elegance.appointment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchanges
    public static final String DIRECT_EXCHANGE = "elegance.direct.exchange";
    public static final String DLX_EXCHANGE = "elegance.dlx.exchange";
    public static final String TOPIC_EXCHANGE = "elegance.topic.exchange";

    // Colas Principales y DLQs
    public static final String APPOINTMENT_CREATED_QUEUE = "appointment.created.queue";
    public static final String APPOINTMENT_CREATED_DLQ = "appointment.created.dlq";
    public static final String APPOINTMENT_CREATED_RK = "appointment.created";

    public static final String APPOINTMENT_STATUS_QUEUE = "appointment.status.queue";
    public static final String APPOINTMENT_STATUS_DLQ = "appointment.status.dlq";
    public static final String APPOINTMENT_STATUS_RK = "appointment.status.updated";

    public static final String NOTIFICATION_SEND_QUEUE = "notification.send.queue";
    public static final String NOTIFICATION_SEND_DLQ = "notification.send.dlq";
    public static final String NOTIFICATION_SEND_RK = "notification.send";

    // ========================================================
    // 1. EXCHANGES (Direct, Dead Letter Exchange y Topic Exchange)
    // ========================================================
    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(DIRECT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(TOPIC_EXCHANGE, true, false);
    }

    // ========================================================
    // 2. COLAS PRINCIPALES (Con argumentos Dead Letter Exchange)
    // ========================================================
    @Bean
    public Queue appointmentCreatedQueue() {
        return QueueBuilder.durable(APPOINTMENT_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", APPOINTMENT_CREATED_DLQ)
                .build();
    }

    @Bean
    public Queue appointmentStatusQueue() {
        return QueueBuilder.durable(APPOINTMENT_STATUS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", APPOINTMENT_STATUS_DLQ)
                .build();
    }

    @Bean
    public Queue notificationSendQueue() {
        return QueueBuilder.durable(NOTIFICATION_SEND_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", NOTIFICATION_SEND_DLQ)
                .build();
    }

    // ========================================================
    // 3. DEAD LETTER QUEUES (DLQ)
    // ========================================================
    @Bean
    public Queue appointmentCreatedDlq() {
        return QueueBuilder.durable(APPOINTMENT_CREATED_DLQ).build();
    }

    @Bean
    public Queue appointmentStatusDlq() {
        return QueueBuilder.durable(APPOINTMENT_STATUS_DLQ).build();
    }

    @Bean
    public Queue notificationSendDlq() {
        return QueueBuilder.durable(NOTIFICATION_SEND_DLQ).build();
    }

    // ========================================================
    // 4. BINDINGS (Colas Principales ➔ Direct Exchange)
    // ========================================================
    @Bean
    public Binding appointmentCreatedBinding(Queue appointmentCreatedQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(appointmentCreatedQueue).to(directExchange).with(APPOINTMENT_CREATED_RK);
    }

    @Bean
    public Binding appointmentStatusBinding(Queue appointmentStatusQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(appointmentStatusQueue).to(directExchange).with(APPOINTMENT_STATUS_RK);
    }

    @Bean
    public Binding notificationSendBinding(Queue notificationSendQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(notificationSendQueue).to(directExchange).with(NOTIFICATION_SEND_RK);
    }

    // ========================================================
    // 5. BINDINGS (DLQs ➔ Dead Letter Exchange)
    // ========================================================
    @Bean
    public Binding appointmentCreatedDlqBinding(Queue appointmentCreatedDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(appointmentCreatedDlq).to(dlxExchange).with(APPOINTMENT_CREATED_DLQ);
    }

    @Bean
    public Binding appointmentStatusDlqBinding(Queue appointmentStatusDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(appointmentStatusDlq).to(dlxExchange).with(APPOINTMENT_STATUS_DLQ);
    }

    @Bean
    public Binding notificationSendDlqBinding(Queue notificationSendDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(notificationSendDlq).to(dlxExchange).with(NOTIFICATION_SEND_DLQ);
    }

    // ========================================================
    // 6. SERIALIZADOR JSON Y TEMPLATE
    // ========================================================
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    // ========================================================
    // 7. RABBIT ADMIN (Auto-declaración de infraestructura)
    // ========================================================
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }
}
