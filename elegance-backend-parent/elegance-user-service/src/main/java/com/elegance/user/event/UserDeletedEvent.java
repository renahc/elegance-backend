package com.elegance.user.event;

import java.time.LocalDateTime;

public record UserDeletedEvent(
    String userId,
    LocalDateTime timestamp
) {}
