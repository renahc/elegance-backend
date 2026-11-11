package com.elegance.analytics;

import com.elegance.common.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Import(KafkaConsumerConfiguration.class)
public class EventRecorder {
  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(EventRecorder.class);
  private final EventRepository repo;
  private final ObjectMapper json;
  private final Validator validator;

  public EventRecorder(EventRepository repo, ObjectMapper json, Validator validator) {
    this.repo = repo;
    this.json = json;
    this.validator = validator;
  }

  @KafkaListener(
      topics = {"elegance.analytics.facts.v1"},
      groupId = "elegance-analytics-v1")
  @Transactional
  public void record(ConsumerRecord<String, String> message) throws Exception {
    SalonEvent e =
        json.readerFor(SalonEvent.class)
            .with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .readValue(message.value());
    var violations = validator.validate(e);
    if (!violations.isEmpty())
      throw new jakarta.validation.ConstraintViolationException(violations);
    e.validate();
    if (!e.appointmentId().equals(message.key()))
      throw new IllegalArgumentException("Event key differs from appointmentId");
    if (repo.existsById(e.eventId())) return;
    RecordedEvent row = new RecordedEvent();
    row.eventId = e.eventId();
    row.topic = message.topic();
    row.partitionNumber = message.partition();
    row.eventOffset = message.offset();
    row.appointmentId = e.appointmentId();
    row.appointmentVersion = e.appointmentVersion();
    row.eventType = e.eventType();
    row.status = e.status();
    row.category = e.serviceCategory();
    row.stylistId = e.stylistId();
    row.serviceId = e.serviceId();
    row.appointmentDate = e.date().toString();
    row.appointmentTime = e.time().toString();
    row.amount = e.amount();
    row.occurredAt = e.occurredAt();
    row.payload = message.value();
    repo.saveAndFlush(row);
    log.info(
        "eventId={} correlationId={} topic={} partition={} offset={}",
        e.eventId(),
        e.correlationId(),
        message.topic(),
        message.partition(),
        message.offset());
  }
}
