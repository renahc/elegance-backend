package com.elegance.appointment;

import static org.junit.jupiter.api.Assertions.*;

import com.elegance.appointment.service.AppointmentService;
import java.time.*;
import org.junit.jupiter.api.Test;

class AppointmentRulesTest {
  private final Clock clock =
      Clock.fixed(Instant.parse("2026-11-06T12:00:00Z"), ZoneId.of("America/Santiago"));

  @Test
  void rejectsPastAppointments() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            AppointmentService.validateTime(LocalDate.of(2026, 11, 5), LocalTime.NOON, 60, clock));
  }

  @Test
  void rejectsClosingTimeOverflow() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            AppointmentService.validateTime(
                LocalDate.of(2026, 11, 7), LocalTime.of(19, 30), 60, clock));
  }

  @Test
  void acceptsBoundaryEndingAtClosing() {
    assertDoesNotThrow(
        () ->
            AppointmentService.validateTime(
                LocalDate.of(2026, 11, 7), LocalTime.of(19, 0), 60, clock));
  }

  @Test
  void rejectsZeroDuration() {
    assertThrows(
        IllegalArgumentException.class,
        () -> AppointmentService.validateTime(LocalDate.of(2026, 11, 7), LocalTime.NOON, 0, clock));
  }

  @Test
  void clientCannotCompleteService() {
    assertThrows(
        IllegalArgumentException.class,
        () -> AppointmentService.validateTransition("en_proceso", "completada", false));
  }

  @Test
  void completedCannotBeReopened() {
    assertThrows(
        IllegalArgumentException.class,
        () -> AppointmentService.validateTransition("completada", "confirmada", true));
  }

  @Test
  void clientCanCancelConfirmed() {
    assertDoesNotThrow(
        () -> AppointmentService.validateTransition("confirmada", "cancelada", false));
  }

  @Test
  void adminCanStartAndComplete() {
    assertDoesNotThrow(
        () -> {
          AppointmentService.validateTransition("confirmada", "en_proceso", true);
          AppointmentService.validateTransition("en_proceso", "completada", true);
        });
  }

  @Test
  void rejectsSunday() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            AppointmentService.validateTime(LocalDate.of(2026, 11, 8), LocalTime.NOON, 60, clock));
  }
}
