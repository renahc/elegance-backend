package com.elegance.notification.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NotificationStoreService {

    private final List<Map<String, Object>> notifications = new CopyOnWriteArrayList<>();

    public void addNotification(String title, String message, String type, String appointmentId) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", "notif-" + UUID.randomUUID().toString().substring(0, 8));
        item.put("title", title);
        item.put("message", message);
        item.put("type", type);
        item.put("appointmentId", appointmentId);
        item.put("timestamp", Instant.now().toString());
        notifications.add(0, item); // Más reciente primero
    }

    public List<Map<String, Object>> getAllNotifications() {
        return Collections.unmodifiableList(notifications);
    }
}
