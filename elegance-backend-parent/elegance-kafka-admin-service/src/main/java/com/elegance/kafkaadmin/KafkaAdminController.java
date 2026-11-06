package com.elegance.kafkaadmin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/admin/kafka")
public class KafkaAdminController {
  private final AdminClient admin;

  public KafkaAdminController(AdminClient admin) {
    this.admin = admin;
  }

  public record TopicRequest(
      @Pattern(regexp = "elegance\\.[a-zA-Z0-9._-]+") String name,
      @Min(1) @Max(24) int partitions,
      @Min(1) @Max(3) short replicationFactor,
      Map<String, String> configs) {}

  public record PartitionsRequest(@Min(1) @Max(24) int total) {}

  public record OffsetsRequest(
      @NotBlank String topic, @Min(0) int partition, @Min(0) long offset) {}

  private void topic(String name) {
    if (name == null || !name.matches("elegance\\.[a-zA-Z0-9._-]+"))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Only elegance topics may be managed");
  }

  private Map<String, String> configs(Map<String, String> values) {
    if (values == null) return Map.of();
    for (var e : values.entrySet()) {
      if (!Set.of(
              "retention.ms",
              "retention.bytes",
              "cleanup.policy",
              "min.insync.replicas",
              "segment.ms")
          .contains(e.getKey()))
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Unsupported topic configuration");
      if (e.getKey().equals("cleanup.policy")) {
        if (!Set.of("delete", "compact", "compact,delete").contains(e.getValue()))
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cleanup policy");
      } else {
        try {
          if (Long.parseLong(e.getValue()) <= 0) throw new NumberFormatException();
        } catch (Exception ex) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST, "Configuration must be positive");
        }
      }
    }
    return values;
  }

  @GetMapping("/topics")
  public Set<String> topics() throws Exception {
    return admin.listTopics().names().get(10, TimeUnit.SECONDS);
  }

  @PostMapping("/topics")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, String> create(@Valid @RequestBody TopicRequest r) throws Exception {
    topic(r.name());
    admin
        .createTopics(
            List.of(
                new NewTopic(r.name(), r.partitions(), r.replicationFactor())
                    .configs(configs(r.configs()))))
        .all()
        .get(10, TimeUnit.SECONDS);
    return Map.of("topic", r.name());
  }

  @GetMapping("/topics/{name}")
  public Map<String, Object> describe(@PathVariable String name) throws Exception {
    topic(name);
    var t = admin.describeTopics(List.of(name)).allTopicNames().get(10, TimeUnit.SECONDS).get(name);
    var resource =
        new org.apache.kafka.common.config.ConfigResource(
            org.apache.kafka.common.config.ConfigResource.Type.TOPIC, name);
    var config =
        admin.describeConfigs(List.of(resource)).all().get(10, TimeUnit.SECONDS).get(resource);
    return Map.of(
        "name",
        name,
        "partitions",
        t.partitions().stream()
            .map(
                p ->
                    Map.of(
                        "partition",
                        p.partition(),
                        "leader",
                        p.leader().id(),
                        "replicas",
                        p.replicas().stream().map(n -> n.id()).toList(),
                        "isr",
                        p.isr().stream().map(n -> n.id()).toList()))
            .toList(),
        "configs",
        config.entries().stream()
            .filter(e -> !e.isSensitive())
            .collect(
                java.util.stream.Collectors.toMap(
                    ConfigEntry::name, e -> Objects.toString(e.value(), ""))));
  }

  @PatchMapping("/topics/{name}/partitions")
  public void partitions(@PathVariable String name, @Valid @RequestBody PartitionsRequest r)
      throws Exception {
    topic(name);
    admin
        .createPartitions(Map.of(name, NewPartitions.increaseTo(r.total())))
        .all()
        .get(10, TimeUnit.SECONDS);
  }

  @PatchMapping("/topics/{name}/configs")
  public void configure(@PathVariable String name, @RequestBody Map<String, String> values)
      throws Exception {
    topic(name);
    var resource =
        new org.apache.kafka.common.config.ConfigResource(
            org.apache.kafka.common.config.ConfigResource.Type.TOPIC, name);
    var ops =
        configs(values).entrySet().stream()
            .map(
                e ->
                    new AlterConfigOp(
                        new ConfigEntry(e.getKey(), e.getValue()), AlterConfigOp.OpType.SET))
            .toList();
    admin.incrementalAlterConfigs(Map.of(resource, ops)).all().get(10, TimeUnit.SECONDS);
  }

  @DeleteMapping("/topics/{name}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String name) throws Exception {
    topic(name);
    admin.deleteTopics(List.of(name)).all().get(10, TimeUnit.SECONDS);
  }

  @GetMapping("/groups")
  public List<Map<String, String>> groups() throws Exception {
    return admin.listConsumerGroups().all().get(10, TimeUnit.SECONDS).stream()
        .map(
            g ->
                Map.of(
                    "groupId",
                    g.groupId(),
                    "state",
                    g.state().map(Object::toString).orElse("unknown")))
        .toList();
  }

  @GetMapping("/groups/{group}/offsets")
  public List<Map<String, Object>> offsets(@PathVariable String group) throws Exception {
    group(group);
    var offsets =
        admin
            .listConsumerGroupOffsets(group)
            .partitionsToOffsetAndMetadata()
            .get(10, TimeUnit.SECONDS);
    Map<TopicPartition, OffsetSpec> queries = new HashMap<>();
    offsets.keySet().forEach(p -> queries.put(p, OffsetSpec.latest()));
    var end = admin.listOffsets(queries).all().get(10, TimeUnit.SECONDS);
    return offsets.entrySet().stream()
        .map(
            e -> {
              Map<String, Object> row = new LinkedHashMap<>();
              row.put("topic", e.getKey().topic());
              row.put("partition", e.getKey().partition());
              row.put("offset", e.getValue().offset());
              row.put("endOffset", end.get(e.getKey()).offset());
              row.put("lag", Math.max(0, end.get(e.getKey()).offset() - e.getValue().offset()));
              return row;
            })
        .toList();
  }

  private void group(String group) {
    if (!group.startsWith("elegance-"))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Only elegance groups may be managed");
  }

  @PostMapping("/groups/{group}/offsets")
  public void reset(@PathVariable String group, @Valid @RequestBody OffsetsRequest r)
      throws Exception {
    group(group);
    topic(r.topic());
    var description =
        admin.describeConsumerGroups(List.of(group)).all().get(10, TimeUnit.SECONDS).get(group);
    if (!description.members().isEmpty())
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Stop all consumers in this group before resetting offsets");
    var partition = new TopicPartition(r.topic(), r.partition());
    var first =
        admin
            .listOffsets(Map.of(partition, OffsetSpec.earliest()))
            .all()
            .get(10, TimeUnit.SECONDS)
            .get(partition)
            .offset();
    var last =
        admin
            .listOffsets(Map.of(partition, OffsetSpec.latest()))
            .all()
            .get(10, TimeUnit.SECONDS)
            .get(partition)
            .offset();
    if (r.offset() < first || r.offset() > last)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Offset outside retained range");
    admin
        .alterConsumerGroupOffsets(group, Map.of(partition, new OffsetAndMetadata(r.offset())))
        .all()
        .get(10, TimeUnit.SECONDS);
  }

  @ExceptionHandler(java.util.concurrent.ExecutionException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, String> kafkaError(java.util.concurrent.ExecutionException e) {
    return Map.of("message", e.getCause().getClass().getSimpleName());
  }
}
