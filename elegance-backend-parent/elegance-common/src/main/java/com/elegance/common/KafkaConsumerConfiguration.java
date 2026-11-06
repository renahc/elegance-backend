package com.elegance.common;

import org.springframework.context.annotation.*;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.*;

@Configuration
@EnableKafka
public class KafkaConsumerConfiguration {
  @Bean
  ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
      ConsumerFactory<String, String> consumers,
      KafkaTemplate<String, String> template,
      @org.springframework.beans.factory.annotation.Value("${KAFKA_CONCURRENCY:1}")
          int concurrency) {
    var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
    factory.setConsumerFactory(consumers);
    factory.setConcurrency(concurrency);
    factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
    var recoverer =
        new DeadLetterPublishingRecoverer(
            template,
            (r, e) ->
                new org.apache.kafka.common.TopicPartition(r.topic() + ".DLT", r.partition()));
    recoverer.setFailIfSendResultIsError(true);
    var backoff = new org.springframework.kafka.support.ExponentialBackOffWithMaxRetries(3);
    backoff.setInitialInterval(1000);
    backoff.setMultiplier(2);
    backoff.setMaxInterval(5000);
    var handler = new DefaultErrorHandler(recoverer, backoff);
    handler.addNotRetryableExceptions(
        IllegalArgumentException.class,
        jakarta.validation.ConstraintViolationException.class,
        com.fasterxml.jackson.core.JsonProcessingException.class);
    factory.setCommonErrorHandler(handler);
    return factory;
  }
}
