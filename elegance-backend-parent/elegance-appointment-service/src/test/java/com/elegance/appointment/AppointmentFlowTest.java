package com.elegance.appointment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.elegance.appointment.dto.AppointmentRequest;
import com.elegance.appointment.repository.*;
import com.elegance.appointment.service.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AppointmentFlowTest {
  @Autowired AppointmentService service;
  @Autowired AppointmentRepository repo;
  @Autowired OutboxRepository outbox;
  @MockBean CatalogClient catalog;

  JwtAuthenticationToken user(String id, boolean admin) {
    return new JwtAuthenticationToken(
        Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .subject(id)
            .claim("oid", id)
            .claim("name", id)
            .build(),
        admin ? List.of(new SimpleGrantedAuthority("ROLE_Admin")) : List.of());
  }

  @BeforeEach
  void setup() {
    when(catalog.service("s1"))
        .thenReturn(
            Map.of(
                "name",
                "Corte",
                "category",
                "cabello",
                "basePrice",
                25000,
                "durationMinutes",
                60,
                "active",
                true));
    when(catalog.stylist("st1")).thenReturn(Map.of("name", "Estilista", "isAvailable", true));
  }

  AppointmentRequest request() {
    var r = new AppointmentRequest();
    r.setServiceId("s1");
    r.setStylistId("st1");
    var day = LocalDate.now(ZoneId.of("America/Santiago")).plusDays(2);
    if (day.getDayOfWeek() == DayOfWeek.SUNDAY) day = day.plusDays(1);
    r.setDate(day.toString());
    r.setTime("12:00");
    r.setClientId("forged-client");
    r.setPrice(1);
    return r;
  }

  @Test
  void usesServerPriceAndAuthenticatedOwner() {
    var a = service.create(request(), user("client1", false));
    assertEquals(25000, a.getPrice());
    assertEquals("cli-client1", a.getClientId());
    assertEquals("client1", a.getOwnerId());
    assertEquals(2, outbox.count());
  }

  @Test
  void rejectsOverlappingReservation() {
    service.create(request(), user("client1", false));
    assertThrows(
        org.springframework.web.server.ResponseStatusException.class,
        () -> service.create(request(), user("client2", false)));
    assertEquals(1, repo.count());
    assertEquals(2, outbox.count());
  }

  @Test
  void clientCannotReadAnotherClientsAppointment() {
    var a = service.create(request(), user("client1", false));
    assertThrows(
        org.springframework.web.server.ResponseStatusException.class,
        () -> service.get(a.getId(), user("client2", false)));
    assertTrue(service.list(null, null, user("client2", false)).isEmpty());
  }

  @Test
  void completionEmitsBothBusinessEventsOnce() {
    var admin = user("admin", true);
    var a = service.create(request(), admin);
    service.status(a.getId(), "en_proceso", admin);
    service.status(a.getId(), "completada", admin);
    assertEquals(7, outbox.count());
    service.status(a.getId(), "completada", admin);
    assertEquals(7, outbox.count());
  }

  @Test
  void cannotCompleteWithoutStarting() {
    var admin = user("admin", true);
    var a = service.create(request(), admin);
    assertThrows(
        IllegalArgumentException.class, () -> service.status(a.getId(), "completada", admin));
    assertEquals(2, outbox.count());
  }

  @Test
  void filtersDateAndStatusTogether() {
    var a = service.create(request(), user("client1", false));
    assertTrue(service.list(a.getDate(), "cancelada", user("client1", false)).isEmpty());
  }
}
