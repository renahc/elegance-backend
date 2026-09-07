package com.elegance.salon.controller;

import com.elegance.salon.dto.AppointmentRequest;
import com.elegance.salon.model.AppointmentEntity;
import com.elegance.salon.repository.AppointmentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
@Tag(name = "Citas & Agenda", description = "Endpoints para la gestión de citas y reservaciones")
public class AppointmentController {

    private final AppointmentRepository appointmentRepository;

    @Autowired
    public AppointmentController(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    @GetMapping
    @Operation(summary = "Obtener todas las citas de la agenda")
    public ResponseEntity<List<AppointmentEntity>> getAllAppointments(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String status) {

        if (date != null) {
            return ResponseEntity.ok(appointmentRepository.findByDate(date));
        }
        if (status != null) {
            return ResponseEntity.ok(appointmentRepository.findByStatus(status));
        }
        return ResponseEntity.ok(appointmentRepository.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener cita por ID")
    public ResponseEntity<AppointmentEntity> getAppointmentById(@PathVariable String id) {
        return appointmentRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Agendar nueva cita")
    public ResponseEntity<AppointmentEntity> createAppointment(@RequestBody AppointmentRequest request) {
        AppointmentEntity entity = new AppointmentEntity(
                "apt-" + UUID.randomUUID().toString().substring(0, 8),
                request.getClientId() != null ? request.getClientId() : "cli-guest",
                request.getClientName() != null ? request.getClientName() : "Cliente Invitado",
                request.getClientAvatar() != null ? request.getClientAvatar() : "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=300",
                request.getServiceId(),
                request.getServiceName(),
                request.getServiceCategory(),
                request.getStylistId(),
                request.getStylistName(),
                request.getDate(),
                request.getTime(),
                request.getDurationMinutes(),
                request.getPrice(),
                "confirmada",
                request.getNotes()
        );

        AppointmentEntity saved = appointmentRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Actualizar estado de una cita")
    public ResponseEntity<AppointmentEntity> updateStatus(
            @PathVariable String id,
            @RequestParam String status) {

        return appointmentRepository.findById(id)
                .map(apt -> {
                    apt.setStatus(status);
                    return ResponseEntity.ok(appointmentRepository.save(apt));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancelar una cita por ID")
    public ResponseEntity<Void> cancelAppointment(@PathVariable String id) {
        if (appointmentRepository.existsById(id)) {
            appointmentRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
