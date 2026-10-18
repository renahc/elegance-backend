package com.elegance.user.config;

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

    @Bean
    public Queue userCreatedQueue(@Value("${rabbitmq.queue.user.created:elegance.user.created.queue}") String q) {
        return durableQueueWithDlq(q, "elegance.user.created.dlq");
    }

    @Bean
    public Queue userCreatedDlq(@Value("${rabbitmq.dlq.user.created:elegance.user.created.dlq}") String dlq) {
        return QueueBuilder.durable(dlq).build();
    }

    @Bean
    public Binding userCreatedBinding(Queue userCreatedQueue, TopicExchange domainExchange) {
        return BindingBuilder.bind(userCreatedQueue).to(domainExchange).with("user.created");
    }

    @Bean
    public Binding userCreatedDlqBinding(Queue userCreatedDlq, TopicExchange domainExchange) {
        return BindingBuilder.bind(userCreatedDlq).to(domainExchange).with("elegance.user.created.dlq");
    }

    @Bean public Queue userUpdatedQueue(@Value("${rabbitmq.queue.user.updated:elegance.user.updated.queue}") String q){ return durableQueueWithDlq(q,"elegance.user.updated.dlq");}
    @Bean public Queue userUpdatedDlq(@Value("${rabbitmq.dlq.user.updated:elegance.user.updated.dlq}") String dlq){ return QueueBuilder.durable(dlq).build();}
    @Bean public Binding userUpdatedBinding(Queue userUpdatedQueue, TopicExchange domainExchange){ return BindingBuilder.bind(userUpdatedQueue).to(domainExchange).with("user.updated");}
    @Bean public Binding userUpdatedDlqBinding(Queue userUpdatedDlq, TopicExchange domainExchange){ return BindingBuilder.bind(userUpdatedDlq).to(domainExchange).with("elegance.user.updated.dlq");}

    @Bean public Queue userDeletedQueue(@Value("${rabbitmq.queue.user.deleted:elegance.user.deleted.queue}") String q){ return durableQueueWithDlq(q,"elegance.user.deleted.dlq");}
    @Bean public Queue userDeletedDlq(@Value("${rabbitmq.dlq.user.deleted:elegance.user.deleted.dlq}") String dlq){ return QueueBuilder.durable(dlq).build();}
    @Bean public Binding userDeletedBinding(Queue userDeletedQueue, TopicExchange domainExchange){ return BindingBuilder.bind(userDeletedQueue).to(domainExchange).with("user.deleted");}
    @Bean public Binding userDeletedDlqBinding(Queue userDeletedDlq, TopicExchange domainExchange){ return BindingBuilder.bind(userDeletedDlq).to(domainExchange).with("elegance.user.deleted.dlq");}

    @Bean public Queue stylistCreatedQueue(@Value("${rabbitmq.queue.stylist.created:elegance.stylist.created.queue}") String q){ return durableQueueWithDlq(q,"elegance.stylist.created.dlq");}
    @Bean public Queue stylistCreatedDlq(@Value("${rabbitmq.dlq.stylist.created:elegance.stylist.created.dlq}") String dlq){ return QueueBuilder.durable(dlq).build();}
    @Bean public Binding stylistCreatedBinding(Queue stylistCreatedQueue, TopicExchange domainExchange){ return BindingBuilder.bind(stylistCreatedQueue).to(domainExchange).with("stylist.created");}
    @Bean public Binding stylistCreatedDlqBinding(Queue stylistCreatedDlq, TopicExchange domainExchange){ return BindingBuilder.bind(stylistCreatedDlq).to(domainExchange).with("elegance.stylist.created.dlq");}

    @Bean public Queue stylistUpdatedQueue(@Value("${rabbitmq.queue.stylist.updated:elegance.stylist.updated.queue}") String q){ return durableQueueWithDlq(q,"elegance.stylist.updated.dlq");}
    @Bean public Queue stylistUpdatedDlq(@Value("${rabbitmq.dlq.stylist.updated:elegance.stylist.updated.dlq}") String dlq){ return QueueBuilder.durable(dlq).build();}
    @Bean public Binding stylistUpdatedBinding(Queue stylistUpdatedQueue, TopicExchange domainExchange){ return BindingBuilder.bind(stylistUpdatedQueue).to(domainExchange).with("stylist.updated");}
    @Bean public Binding stylistUpdatedDlqBinding(Queue stylistUpdatedDlq, TopicExchange domainExchange){ return BindingBuilder.bind(stylistUpdatedDlq).to(domainExchange).with("elegance.stylist.updated.dlq");}

    @Bean public Queue stylistDeletedQueue(@Value("${rabbitmq.queue.stylist.deleted:elegance.stylist.deleted.queue}") String q){ return durableQueueWithDlq(q,"elegance.stylist.deleted.dlq");}
    @Bean public Queue stylistDeletedDlq(@Value("${rabbitmq.dlq.stylist.deleted:elegance.stylist.deleted.dlq}") String dlq){ return QueueBuilder.durable(dlq).build();}
    @Bean public Binding stylistDeletedBinding(Queue stylistDeletedQueue, TopicExchange domainExchange){ return BindingBuilder.bind(stylistDeletedQueue).to(domainExchange).with("stylist.deleted");}
    @Bean public Binding stylistDeletedDlqBinding(Queue stylistDeletedDlq, TopicExchange domainExchange){ return BindingBuilder.bind(stylistDeletedDlq).to(domainExchange).with("elegance.stylist.deleted.dlq");}

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
