package com.elegance.appointment.service;

import com.elegance.appointment.config.RabbitMQConfig;
import com.elegance.appointment.dto.*;
import com.elegance.appointment.model.*;
import com.elegance.appointment.repository.*;
import com.elegance.common.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AppointmentService {
  private final AppointmentRepository repo;
  private final OutboxRepository outbox;
  private final StylistLockRepository locks;
  private final ObjectMapper json;
  private final CatalogClient catalog;

  public AppointmentService(
      AppointmentRepository repo,
      OutboxRepository outbox,
      StylistLockRepository locks,
      ObjectMapper json,
      CatalogClient catalog) {
    this.repo = repo;
    this.outbox = outbox;
    this.locks = locks;
    this.json = json;
    this.catalog = catalog;
  }

  private String owner(Authentication a) {
    Jwt j = (Jwt) a.getPrincipal();
    return j.getClaimAsString("oid") != null ? j.getClaimAsString("oid") : j.getSubject();
  }

  public boolean owns(AppointmentEntity e, Authentication a) {
    return ApiSecurity.isAdmin(a) || Objects.equals(e.getOwnerId(), owner(a));
  }

  public List<AppointmentEntity> list(String date, String status, Authentication auth) {
    return repo.findAll().stream()
        .filter(e -> owns(e, auth))
        .filter(e -> date == null || date.equals(e.getDate()))
        .filter(e -> status == null || status.equals(e.getStatus()))
        .toList();
  }

  public AppointmentEntity get(String id, Authentication a) {
    var e = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!owns(e, a)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    return e;
  }

  @Transactional
  public AppointmentEntity create(AppointmentRequest r, Authentication auth) {
    if (r.getServiceId() == null || r.getStylistId() == null)
      throw new IllegalArgumentException("Servicio y estilista obligatorios");
    var service = catalog.service(r.getServiceId());
    var stylist = catalog.stylist(r.getStylistId());
    if (Boolean.FALSE.equals(service.get("active"))
        || Boolean.FALSE.equals(stylist.get("isAvailable")))
      throw new IllegalArgumentException("Servicio o estilista no disponible");
    LocalDate day;
    LocalTime time;
    try {
      day = LocalDate.parse(r.getDate());
      time = LocalTime.parse(r.getTime());
    } catch (Exception e) {
      throw new IllegalArgumentException("Fecha u hora inválida");
    }
    int duration = ((Number) service.get("durationMinutes")).intValue();
    validateTime(day, time, duration, Clock.system(ZoneId.of("America/Santiago")));
    Object shift = stylist.get("shift");
    if (shift instanceof String text && !text.isBlank()) {
      try {
        String[] hours = text.split("\\s*-\\s*");
        if (hours.length != 2
            || time.isBefore(LocalTime.parse(hours[0]))
            || time.plusMinutes(duration).isAfter(LocalTime.parse(hours[1])))
          throw new IllegalArgumentException("Fuera del turno del estilista");
      } catch (java.time.format.DateTimeParseException ex) {
        throw new IllegalArgumentException("El estilista no tiene un turno válido");
      }
    }
    locks.ensure(r.getStylistId());
    locks.lock(r.getStylistId());
    for (var other : repo.findByStylistId(r.getStylistId())) {
      if (!other.getDate().equals(r.getDate()) || other.getStatus().equals("cancelada")) continue;
      LocalTime start = LocalTime.parse(other.getTime());
      if (time.isBefore(start.plusMinutes(other.getDurationMinutes()))
          && start.isBefore(time.plusMinutes(duration)))
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Horario ocupado");
    }
    var e = new AppointmentEntity();
    e.setId("apt-" + UUID.randomUUID());
    e.setOwnerId(
        ApiSecurity.isAdmin(auth) && r.getClientId() != null && r.getClientId().startsWith("cli-")
            ? r.getClientId().substring(4)
            : owner(auth));
    e.setClientId(ApiSecurity.isAdmin(auth) ? r.getClientId() : "cli-" + owner(auth));
    e.setClientName(
        ApiSecurity.isAdmin(auth)
            ? r.getClientName()
            : ((Jwt) auth.getPrincipal()).getClaimAsString("name"));
    e.setServiceId(r.getServiceId());
    e.setServiceName((String) service.get("name"));
    e.setServiceCategory((String) service.get("category"));
    e.setStylistId(r.getStylistId());
    e.setStylistName((String) stylist.get("name"));
    e.setDate(r.getDate());
    e.setTime(r.getTime());
    e.setDurationMinutes(duration);
    e.setPrice(((Number) service.get("basePrice")).intValue());
    e.setNotes(r.getNotes());
    e.setStatus("confirmada");
    repo.saveAndFlush(e);
    emit(e, "CREATED", null, r.isSimulateError() && ApiSecurity.isAdmin(auth));
    return e;
  }

  public static void validateTime(LocalDate day, LocalTime time, int duration, Clock clock) {
    if (day.getDayOfWeek() == DayOfWeek.SUNDAY)
      throw new IllegalArgumentException("El salón está cerrado los domingos");
    if (duration <= 0 || duration > 540 || !day.atTime(time).isAfter(LocalDateTime.now(clock)))
      throw new IllegalArgumentException("La cita debe estar en el futuro y tener duración válida");
    if (time.isBefore(LocalTime.of(9, 0))
        || time.plusMinutes(duration).isAfter(LocalTime.of(20, 0))
        || time.plusMinutes(duration).isBefore(time))
      throw new IllegalArgumentException("Horario de atención: 09:00 a 20:00");
  }

  public static void validateTransition(String from, String to, boolean admin) {
    boolean valid =
        from.equals("confirmada") && (to.equals("cancelada") || (admin && to.equals("en_proceso")))
            || admin && from.equals("en_proceso") && to.equals("completada");
    if (!valid) throw new IllegalArgumentException("Cambio de estado no permitido");
  }

  @Transactional
  public AppointmentEntity status(String id, String status, Authentication auth) {
    var e = get(id, auth);
    if (e.getStatus().equals(status)) return e;
    validateTransition(e.getStatus(), status, ApiSecurity.isAdmin(auth));
    String previous = e.getStatus();
    e.setStatus(status);
    repo.saveAndFlush(e);
    emit(e, "STATUS_UPDATED", previous, false);
    if (status.equals("completada")) emit(e, "COMPLETED", previous, false);
    return e;
  }

  private void emit(AppointmentEntity e, String type, String previous, boolean simulate) {
    var event =
        new SalonEvent(
            UUID.randomUUID().toString(),
            type,
            1,
            java.util.Optional.ofNullable(org.slf4j.MDC.get("correlationId"))
                .orElseGet(() -> UUID.randomUUID().toString()),
            Instant.now(),
            e.getId(),
            e.getVersion(),
            e.getServiceId(),
            e.getServiceCategory(),
            e.getStylistId(),
            LocalDate.parse(e.getDate()),
            LocalTime.parse(e.getTime()),
            previous,
            e.getStatus(),
            e.getPrice(),
            "CLP",
            e.getDurationMinutes());
    try {
      outbox.save(
          new OutboxMessage(
              "kafka",
              type.equals("COMPLETED") ? SalonEvent.COMPLETED : SalonEvent.APPOINTMENTS,
              e.getId(),
              json.writeValueAsString(event)));
      if (!type.equals("COMPLETED")) {
        var r = new AppointmentEvent();
        r.setEventId(event.eventId());
        r.setEventType(type);
        r.setAppointmentId(e.getId());
        r.setClientId(e.getClientId());
        r.setClientName(e.getClientName());
        r.setServiceName(e.getServiceName());
        r.setStylistName(e.getStylistName());
        r.setDate(e.getDate());
        r.setTime(e.getTime());
        r.setStatus(e.getStatus());
        r.setPrice(e.getPrice());
        r.setTimestamp(event.occurredAt().toString());
        r.setSimulateError(simulate);
        outbox.save(
            new OutboxMessage(
                "rabbit",
                type.equals("CREATED")
                    ? RabbitMQConfig.APPOINTMENT_CREATED_RK
                    : RabbitMQConfig.APPOINTMENT_STATUS_RK,
                e.getId(),
                json.writeValueAsString(r)));
      }
    } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
      throw new IllegalStateException("Event serialization failed", ex);
    }
  }
}
