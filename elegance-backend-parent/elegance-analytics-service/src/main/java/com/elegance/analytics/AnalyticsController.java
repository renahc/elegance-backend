package com.elegance.analytics;

import java.time.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {
  private final EventRepository repo;

  public AnalyticsController(EventRepository repo) {
    this.repo = repo;
  }

  @GetMapping
  public Map<String, Object> summary() {
    return summarize(repo.findAll());
  }

  public static Map<String, Object> summarize(List<RecordedEvent> events) {
    Map<String, RecordedEvent> latest = new HashMap<>();
    Map<String, RecordedEvent> completed = new HashMap<>();
    for (var e : events) {
      if (e.eventType.equals("COMPLETED"))
        completed.merge(
            e.appointmentId, e, (a, b) -> a.appointmentVersion >= b.appointmentVersion ? a : b);
      else
        latest.merge(
            e.appointmentId, e, (a, b) -> a.appointmentVersion >= b.appointmentVersion ? a : b);
    }
    Map<String, Long> statuses = new TreeMap<>(),
        demand = new TreeMap<>(),
        hours = new TreeMap<>(),
        categories = new TreeMap<>(),
        daily = new TreeMap<>();
    for (var e : latest.values()) {
      statuses.merge(e.status, 1L, Long::sum);
      demand.merge(e.category, 1L, Long::sum);
      hours.merge(e.appointmentTime.substring(0, 2) + ":00", 1L, Long::sum);
    }
    long total = 0;
    for (var e : completed.values()) {
      total += e.amount;
      categories.merge(e.category, e.amount.longValue(), Long::sum);
      daily.merge(e.appointmentDate, e.amount.longValue(), Long::sum);
    }
    return Map.of(
        "appointments",
        latest.size(),
        "statuses",
        statuses,
        "demandByCategory",
        demand,
        "demandByHour",
        hours,
        "completedServices",
        completed.size(),
        "serviceValue",
        total,
        "valueByCategory",
        categories,
        "valueByDate",
        daily,
        "currency",
        "CLP",
        "updatedAt",
        Instant.now().toString());
  }
}
