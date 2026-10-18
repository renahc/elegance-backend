package com.elegance.user.event;
import java.time.LocalDateTime;
public record StylistUpdatedEvent(String stylistId, String name, String specialty, boolean isAvailable, LocalDateTime timestamp) {}