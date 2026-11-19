package com.elegance.kafkaadmin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/kafka")
public class ReplayController {
  private final String servers;
  private final KafkaTemplate<String, String> producer;

  public ReplayController(
      @Value("${spring.kafka.bootstrap-servers}") String servers,
      KafkaTemplate<String, String> producer) {
    this.servers = servers;
    this.producer = producer;
  }

  public record Replay(
      @NotBlank @Pattern(regexp = "elegance\\.[a-zA-Z0-9._-]+\\.DLT") String topic,
      @Min(0) int partition,
      @Min(0) long offset,
      @Min(1) @Max(100) int limit) {}

  @PostMapping("/replay")
  public Map<String, Object> replay(@Valid @RequestBody Replay request) throws Exception {
    Properties properties = new Properties();
    properties.put("bootstrap.servers", servers);
    properties.put("enable.auto.commit", "false");
    properties.put("key.deserializer", StringDeserializer.class.getName());
    properties.put("value.deserializer", StringDeserializer.class.getName());
    properties.put("max.poll.records", request.limit());
    int count = 0;
    long next = request.offset();
    String target = request.topic().substring(0, request.topic().length() - 4);
    try (var consumer = new KafkaConsumer<String, String>(properties)) {
      var partition = new TopicPartition(request.topic(), request.partition());
      consumer.assign(List.of(partition));
      long first = consumer.beginningOffsets(List.of(partition)).get(partition),
          end = consumer.endOffsets(List.of(partition)).get(partition);
      if (request.offset() < first || request.offset() > end)
        throw new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.BAD_REQUEST, "Offset outside retained range");
      consumer.seek(partition, request.offset());
      for (var record : consumer.poll(Duration.ofSeconds(3))) {
        producer.send(target, record.key(), record.value()).get(10, TimeUnit.SECONDS);
        count++;
        next = record.offset() + 1;
      }
    }
    return Map.of("replayed", count, "nextOffset", next, "target", target);
  }
}
