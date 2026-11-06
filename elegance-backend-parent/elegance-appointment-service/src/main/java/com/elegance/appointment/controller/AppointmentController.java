package com.elegance.appointment.controller;

import com.elegance.appointment.dto.AppointmentRequest;
import com.elegance.appointment.model.AppointmentEntity;
import com.elegance.appointment.service.AppointmentService;
import java.util.List;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {
  private final AppointmentService service;

  public AppointmentController(AppointmentService service) {
    this.service = service;
  }

  @GetMapping
  public List<AppointmentEntity> list(
      @RequestParam(required = false) String date,
      @RequestParam(required = false) String status,
      Authentication auth) {
    return service.list(date, status, auth);
  }

  @GetMapping("/{id}")
  public AppointmentEntity get(@PathVariable String id, Authentication auth) {
    return service.get(id, auth);
  }

  @PostMapping
  public ResponseEntity<AppointmentEntity> create(
      @jakarta.validation.Valid @RequestBody AppointmentRequest request, Authentication auth) {
    return ResponseEntity.status(201).body(service.create(request, auth));
  }

  @PutMapping("/{id}/status")
  public AppointmentEntity status(
      @PathVariable String id, @RequestParam String status, Authentication auth) {
    return service.status(id, status, auth);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> cancel(@PathVariable String id, Authentication auth) {
    service.status(id, "cancelada", auth);
    return ResponseEntity.noContent().build();
  }
}
