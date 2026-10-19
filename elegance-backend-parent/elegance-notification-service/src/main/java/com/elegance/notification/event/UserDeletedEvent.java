package com.elegance.notification.event;
import java.time.LocalDateTime;
public record UserDeletedEvent(String userId, LocalDateTime timestamp) {}