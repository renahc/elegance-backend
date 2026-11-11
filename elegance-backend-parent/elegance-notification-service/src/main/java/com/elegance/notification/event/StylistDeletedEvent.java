package com.elegance.notification.event;
import java.time.LocalDateTime;
public record StylistDeletedEvent(String stylistId, LocalDateTime timestamp) {}