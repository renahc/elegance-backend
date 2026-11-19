package com.elegance.audit;

import static org.junit.jupiter.api.Assertions.*;

import com.elegance.common.SalonEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AuditPersistenceTest {
  @Autowired EventRecorder recorder;
  @Autowired EventRepository repo;
  @Autowired ObjectMapper json;

  @Test
  void duplicateDeliveryOnlyPersistsOnce() throws Exception {
    var e =
        new SalonEvent(
            "duplicate-test",
            "CREATED",
            1,
            "correlation",
            Instant.now(),
            "apt-1",
            0,
            "srv-1",
            "cabello",
            "sty-1",
            LocalDate.now(),
            LocalTime.NOON,
            null,
            "confirmada",
            25000,
            "CLP",
            60);
    var record =
        new ConsumerRecord<String, String>(
            SalonEvent.APPOINTMENTS, 0, 1, "apt-1", json.writeValueAsString(e));
    recorder.record(record);
    recorder.record(record);
    assertEquals(1, repo.count());
  }
}
