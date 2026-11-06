package com.elegance.notification.service;

import com.elegance.notification.model.NotificationEntity;
import com.elegance.notification.repository.NotificationRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationStoreService {
  private final NotificationRepository repo;

  public NotificationStoreService(NotificationRepository repo) {
    this.repo = repo;
  }

  @Transactional
  public void addNotification(
      String eventId, String title, String message, String type, String appointmentId) {
    if (eventId == null || eventId.isBlank())
      throw new IllegalArgumentException("eventId required");
    if (repo.existsById(eventId)) return;
    var row = new NotificationEntity();
    row.id = eventId;
    row.title = title;
    row.message = message;
    row.type = type;
    row.appointmentId = appointmentId;
    row.timestamp = java.time.Instant.now();
    repo.saveAndFlush(row);
  }

  public List<Map<String, Object>> getAllNotifications() {
    return repo.findTop100ByOrderByTimestampDesc().stream()
        .map(
            n -> {
              Map<String, Object> m = new LinkedHashMap<>();
              m.put("id", n.id);
              m.put("title", n.title);
              m.put("message", n.message);
              m.put("type", "appointment");
              m.put("appointmentId", n.appointmentId);
              m.put("timestamp", n.timestamp);
              m.put("time", n.timestamp.toString());
              m.put("read", false);
              return m;
            })
        .toList();
  }
}
