package com.elegance.analytics;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class AnalyticsTest {
  RecordedEvent event(String id, String type, long version, String status, int amount) {
    var e = new RecordedEvent();
    e.eventId = id;
    e.appointmentId = "apt-1";
    e.appointmentVersion = version;
    e.eventType = type;
    e.status = status;
    e.amount = amount;
    e.category = "cabello";
    e.appointmentDate = "2026-11-07";
    e.appointmentTime = "12:00";
    return e;
  }

  @Test
  void completedServiceDoesNotCountEveryTransitionAsRevenue() {
    var rows =
        List.of(
            event("1", "CREATED", 0, "confirmada", 25000),
            event("2", "STATUS_UPDATED", 1, "en_proceso", 25000),
            event("3", "STATUS_UPDATED", 2, "completada", 25000),
            event("4", "COMPLETED", 2, "completada", 25000),
            event("5", "COMPLETED", 2, "completada", 25000));
    var stats = AnalyticsController.summarize(rows);
    assertEquals(25000L, stats.get("serviceValue"));
    assertEquals(1, stats.get("completedServices"));
    assertEquals(1, stats.get("appointments"));
  }

  @Test
  void outOfOrderReplayKeepsLatestState() {
    var stats =
        AnalyticsController.summarize(
            List.of(
                event("2", "STATUS_UPDATED", 1, "cancelada", 25000),
                event("1", "CREATED", 0, "confirmada", 25000)));
    assertEquals(Map.of("cancelada", 1L), stats.get("statuses"));
    assertEquals(0L, stats.get("serviceValue"));
  }

  @Test
  void emptyStreamHasNoInventedStatistics() {
    var stats = AnalyticsController.summarize(List.of());
    assertEquals(0, stats.get("appointments"));
    assertEquals(0L, stats.get("serviceValue"));
  }
}
