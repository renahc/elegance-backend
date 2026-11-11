package com.elegance.service.event;
import java.time.LocalDateTime;
public record ServiceDeletedEvent(String serviceId, LocalDateTime timestamp) {}