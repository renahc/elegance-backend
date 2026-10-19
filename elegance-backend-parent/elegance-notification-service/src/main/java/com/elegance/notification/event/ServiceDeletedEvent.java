package com.elegance.notification.event;
import java.time.LocalDateTime;
public record ServiceDeletedEvent(String serviceId, LocalDateTime timestamp) {}