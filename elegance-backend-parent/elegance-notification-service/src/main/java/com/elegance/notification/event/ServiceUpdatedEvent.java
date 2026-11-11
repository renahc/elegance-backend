package com.elegance.notification.event;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record ServiceUpdatedEvent(String serviceId, String name, String description, BigDecimal price, Integer durationMinutes, LocalDateTime timestamp) {}