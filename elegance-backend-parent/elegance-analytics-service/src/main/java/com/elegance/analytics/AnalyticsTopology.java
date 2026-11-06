package com.elegance.analytics;

import com.elegance.common.SalonEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import java.time.Duration;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.context.annotation.*;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@Configuration
@EnableKafkaStreams
public class AnalyticsTopology {
  public static final String FACTS = "elegance.analytics.facts.v1";
  public static final String WINDOWS = "elegance.analytics.windows.v1";

  @Bean
  KStream<String, String> pipeline(StreamsBuilder builder, ObjectMapper json, Validator validator) {
    var source =
        builder.stream(
            java.util.List.of(SalonEvent.APPOINTMENTS, SalonEvent.COMPLETED),
            Consumed.with(Serdes.String(), Serdes.String()));
    return build(source, json, validator);
  }

  public static KStream<String, String> build(
      KStream<String, String> source, ObjectMapper json, Validator validator) {
    java.util.function.BiPredicate<String, String> valid =
        (key, value) -> {
          try {
            var e = json.readValue(value, SalonEvent.class);
            e.validate();
            return validator.validate(e).isEmpty() && e.appointmentId().equals(key);
          } catch (Exception ex) {
            return false;
          }
        };
    source
        .filter((k, v) -> !valid.test(k, v))
        .to("elegance.streaming.invalid.v1", Produced.with(Serdes.String(), Serdes.String()));
    var accepted = source.filter((k, v) -> valid.test(k, v));
    accepted.to(FACTS, Produced.with(Serdes.String(), Serdes.String()));
    // Operational throughput, not financial totals: counts processing records per category in
    // five-minute windows.
    accepted
        .groupBy(
            (k, v) -> {
              try {
                return json.readValue(v, SalonEvent.class).serviceCategory();
              } catch (Exception ex) {
                throw new IllegalArgumentException(ex);
              }
            },
            Grouped.with(Serdes.String(), Serdes.String()))
        .windowedBy(TimeWindows.ofSizeAndGrace(Duration.ofMinutes(5), Duration.ofMinutes(1)))
        .count(Materialized.as("category-throughput"))
        .toStream()
        .selectKey((window, count) -> window.key() + "@" + window.window().start())
        .mapValues(Object::toString)
        .to(WINDOWS, Produced.with(Serdes.String(), Serdes.String()));
    return accepted;
  }
}
