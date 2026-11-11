package com.elegance.service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange:elegance.domain.exchange}")
    private String exchangeName;

    @Bean
    public TopicExchange domainExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    private Queue durableQueueWithDlq(String queueName, String dlqRoutingKey) {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", exchangeName)
                .withArgument("x-dead-letter-routing-key", dlqRoutingKey)
                .build();
    }

    @Bean public Queue serviceCreatedQueue(@Value("${rabbitmq.queue.service.created:elegance.service.created.queue}") String q){ return durableQueueWithDlq(q,"elegance.service.created.dlq");}
    @Bean public Queue serviceCreatedDlq(@Value("${rabbitmq.dlq.service.created:elegance.service.created.dlq}") String dlq){ return QueueBuilder.durable(dlq).build();}
    @Bean public Binding serviceCreatedBinding(Queue serviceCreatedQueue, TopicExchange domainExchange){ return BindingBuilder.bind(serviceCreatedQueue).to(domainExchange).with("service.created");}
    @Bean public Binding serviceCreatedDlqBinding(Queue serviceCreatedDlq, TopicExchange domainExchange){ return BindingBuilder.bind(serviceCreatedDlq).to(domainExchange).with("elegance.service.created.dlq");}

    @Bean public Queue serviceUpdatedQueue(@Value("${rabbitmq.queue.service.updated:elegance.service.updated.queue}") String q){ return durableQueueWithDlq(q,"elegance.service.updated.dlq");}
    @Bean public Queue serviceUpdatedDlq(@Value("${rabbitmq.dlq.service.updated:elegance.service.updated.dlq}") String dlq){ return QueueBuilder.durable(dlq).build();}
    @Bean public Binding serviceUpdatedBinding(Queue serviceUpdatedQueue, TopicExchange domainExchange){ return BindingBuilder.bind(serviceUpdatedQueue).to(domainExchange).with("service.updated");}
    @Bean public Binding serviceUpdatedDlqBinding(Queue serviceUpdatedDlq, TopicExchange domainExchange){ return BindingBuilder.bind(serviceUpdatedDlq).to(domainExchange).with("elegance.service.updated.dlq");}

    @Bean public Queue serviceDeletedQueue(@Value("${rabbitmq.queue.service.deleted:elegance.service.deleted.queue}") String q){ return durableQueueWithDlq(q,"elegance.service.deleted.dlq");}
    @Bean public Queue serviceDeletedDlq(@Value("${rabbitmq.dlq.service.deleted:elegance.service.deleted.dlq}") String dlq){ return QueueBuilder.durable(dlq).build();}
    @Bean public Binding serviceDeletedBinding(Queue serviceDeletedQueue, TopicExchange domainExchange){ return BindingBuilder.bind(serviceDeletedQueue).to(domainExchange).with("service.deleted");}
    @Bean public Binding serviceDeletedDlqBinding(Queue serviceDeletedDlq, TopicExchange domainExchange){ return BindingBuilder.bind(serviceDeletedDlq).to(domainExchange).with("elegance.service.deleted.dlq");}

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }
}
