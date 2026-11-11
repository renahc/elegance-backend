package com.elegance.analytics;

import static org.junit.jupiter.api.Assertions.*;

import com.elegance.common.SalonEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import java.time.*;
import java.util.Properties;
import org.apache.kafka.common.serialization.*;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;
import org.junit.jupiter.api.Test;

class TopologyTest {
  @Test
  void routesBothBusinessTopicsAndQuarantinesInvalidJson() throws Exception {
    var json = new ObjectMapper().findAndRegisterModules();
    var factory = Validation.buildDefaultValidatorFactory();
    var builder = new StreamsBuilder();
    AnalyticsTopology.build(
        builder.stream(
            java.util.List.of(SalonEvent.APPOINTMENTS, SalonEvent.COMPLETED),
            Consumed.with(Serdes.String(), Serdes.String())),
        json,
        factory.getValidator());
    Properties props = new Properties();
    props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test-topology");
    props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");
    props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
    props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
    try (var driver = new TopologyTestDriver(builder.build(), props)) {
      var input =
          driver.createInputTopic(
              SalonEvent.APPOINTMENTS, new StringSerializer(), new StringSerializer());
      var completed =
          driver.createInputTopic(
              SalonEvent.COMPLETED, new StringSerializer(), new StringSerializer());
      var out =
          driver.createOutputTopic(
              AnalyticsTopology.FACTS, new StringDeserializer(), new StringDeserializer());
      var dead =
          driver.createOutputTopic(
              "elegance.streaming.invalid.v1", new StringDeserializer(), new StringDeserializer());
      var e =
          new SalonEvent(
              "e1",
              "CREATED",
              1,
              "c1",
              Instant.now(),
              "apt-1",
              0,
              "service-1",
              "cabello",
              "stylist-1",
              LocalDate.of(2026, 11, 7),
              LocalTime.NOON,
              null,
              "confirmada",
              25000,
              "CLP",
              60);
      input.pipeInput("apt-1", json.writeValueAsString(e));
      assertEquals("apt-1", out.readKeyValue().key);
      var done =
          new SalonEvent(
              "e2",
              "COMPLETED",
              1,
              "c2",
              Instant.now(),
              "apt-1",
              2,
              "service-1",
              "cabello",
              "stylist-1",
              LocalDate.of(2026, 11, 7),
              LocalTime.NOON,
              "en_proceso",
              "completada",
              25000,
              "CLP",
              60);
      completed.pipeInput("apt-1", json.writeValueAsString(done));
      assertEquals("apt-1", out.readKeyValue().key);
      input.pipeInput("apt-1", "{bad json");
      assertEquals("{bad json", dead.readValue());
      assertTrue(out.isEmpty());
    } finally {
      factory.close();
    }
  }
}
