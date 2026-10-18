package com.elegance.user.event;

import java.time.LocalDateTime;

public record StylistDeletedEvent(
    String stylistId,
    LocalDateTime timestamp
) {}
