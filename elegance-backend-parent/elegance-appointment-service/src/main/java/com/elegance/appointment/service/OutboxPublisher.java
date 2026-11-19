package com.elegance.appointment.service;

import com.elegance.appointment.config.RabbitMQConfig;
import com.elegance.appointment.dto.AppointmentEvent;
import com.elegance.appointment.repository.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@EnableScheduling
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "outbox.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class OutboxPublisher {
  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(OutboxPublisher.class);
  private final OutboxRepository repo;
  private final ObjectMapper json;
  private final KafkaTemplate<String, String> kafka;
  private final RabbitTemplate rabbit;

  public OutboxPublisher(
      OutboxRepository repo,
      ObjectMapper json,
      KafkaTemplate<String, String> kafka,
      RabbitTemplate rabbit) {
    this.repo = repo;
    this.json = json;
    this.kafka = kafka;
    this.rabbit = rabbit;
  }

  @Scheduled(fixedDelayString = "${outbox.delay-ms:2000}")
  @Transactional
  public void publish() {
    for (var message :
        repo.pending(Instant.now(), org.springframework.data.domain.PageRequest.of(0, 50))) {
      try {
        if (message.destination.equals("kafka"))
          kafka.send(message.route, message.messageKey, message.payload).get(10, TimeUnit.SECONDS);
        else {
          CorrelationData correlation = new CorrelationData(message.id.toString());
          rabbit.convertAndSend(
              RabbitMQConfig.DIRECT_EXCHANGE,
              message.route,
              json.readValue(message.payload, AppointmentEvent.class),
              correlation);
          var confirm = correlation.getFuture().get(10, TimeUnit.SECONDS);
          if (!confirm.isAck() || correlation.getReturned() != null)
            throw new IllegalStateException("RabbitMQ rejected or returned message");
        }
        message.delivered = true;
        message.lastError = null;
      } catch (Exception e) {
        if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        message.attempts++;
        message.lastError = e.getClass().getSimpleName();
        message.nextAttempt =
            Instant.now().plusSeconds(Math.min(300, 1L << Math.min(message.attempts, 8)));
        log.warn(
            "Outbox id={} destination={} attempt={} failure={}",
            message.id,
            message.destination,
            message.attempts,
            message.lastError);
      }
    }
  }
}
