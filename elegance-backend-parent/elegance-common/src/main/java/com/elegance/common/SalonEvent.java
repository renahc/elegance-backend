package com.elegance.common;

import jakarta.validation.constraints.*;
import java.time.*;

public record SalonEvent(
    @NotBlank String eventId,
    @NotBlank String eventType,
    @Min(1) @Max(1) int schemaVersion,
    @NotBlank String correlationId,
    @NotNull Instant occurredAt,
    @NotBlank String appointmentId,
    @Min(0) long appointmentVersion,
    @NotBlank String serviceId,
    @NotBlank String serviceCategory,
    @NotBlank String stylistId,
    @NotNull LocalDate date,
    @NotNull LocalTime time,
    String previousStatus,
    @NotBlank String status,
    @NotNull @PositiveOrZero Integer amount,
    @NotBlank @Pattern(regexp = "CLP") String currency,
    @NotNull @Positive Integer durationMinutes) {
  public static final String APPOINTMENTS = "elegance.appointments.events.v1";
  public static final String COMPLETED = "elegance.services.completed.v1";

  public void validate() {
    if (!java.util.Set.of("CREATED", "STATUS_UPDATED", "COMPLETED").contains(eventType))
      throw new IllegalArgumentException("Unknown event type");
    if (!java.util.Set.of("confirmada", "en_proceso", "completada", "cancelada").contains(status))
      throw new IllegalArgumentException("Unknown status");
    if ("COMPLETED".equals(eventType) && !"completada".equals(status))
      throw new IllegalArgumentException("Completion status required");
  }
}
