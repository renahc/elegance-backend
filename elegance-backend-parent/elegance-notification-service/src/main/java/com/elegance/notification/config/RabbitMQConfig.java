package com.elegance.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String DIRECT_EXCHANGE = "elegance.direct.exchange";
    public static final String DLX_EXCHANGE = "elegance.dlx.exchange";

    public static final String APPOINTMENT_CREATED_QUEUE = "appointment.created.queue";
    public static final String APPOINTMENT_CREATED_DLQ = "appointment.created.dlq";
    public static final String APPOINTMENT_CREATED_RK = "appointment.created";

    public static final String APPOINTMENT_STATUS_QUEUE = "appointment.status.queue";
    public static final String APPOINTMENT_STATUS_DLQ = "appointment.status.dlq";
    public static final String APPOINTMENT_STATUS_RK = "appointment.status.updated";

    public static final String NOTIFICATION_SEND_QUEUE = "notification.send.queue";
    public static final String NOTIFICATION_SEND_DLQ = "notification.send.dlq";
    public static final String NOTIFICATION_SEND_RK = "notification.send";

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(DIRECT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue appointmentCreatedQueue() {
        return QueueBuilder.durable(APPOINTMENT_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", APPOINTMENT_CREATED_DLQ)
                .build();
    }

    @Bean
    public Queue appointmentCreatedDlq() {
        return QueueBuilder.durable(APPOINTMENT_CREATED_DLQ).build();
    }

    @Bean
    public Binding appointmentCreatedBinding(Queue appointmentCreatedQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(appointmentCreatedQueue).to(directExchange).with(APPOINTMENT_CREATED_RK);
    }

    @Bean
    public Binding appointmentCreatedDlqBinding(Queue appointmentCreatedDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(appointmentCreatedDlq).to(dlxExchange).with(APPOINTMENT_CREATED_DLQ);
    }

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

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setDefaultRequeueRejected(false);

        // Reintentos automáticos (3 intentos) y desvío a DLQ mediante RejectAndDontRequeueRecoverer
        factory.setAdviceChain(
            RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(1000, 1.0, 10000)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build()
        );

        return factory;
    }
}
