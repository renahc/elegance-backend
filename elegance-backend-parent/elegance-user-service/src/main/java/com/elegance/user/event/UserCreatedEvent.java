package com.elegance.user.event;
import java.time.LocalDateTime;
public record UserCreatedEvent(String userId, String name, String email, String role, LocalDateTime timestamp) {}