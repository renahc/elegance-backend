package com.elegance.appointment.controller;

import com.elegance.appointment.dto.AppointmentRequest;
import com.elegance.appointment.model.AppointmentEntity;
import com.elegance.appointment.repository.AppointmentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import com.elegance.appointment.config.RabbitMQConfig;
import com.elegance.appointment.dto.AppointmentEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/appointments")
@Tag(name = "Citas & Agenda", description = "Endpoints para la gestión de citas y reservaciones")
public class AppointmentController {

    private final AppointmentRepository appointmentRepository;
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public AppointmentController(AppointmentRepository appointmentRepository, RabbitTemplate rabbitTemplate) {
        this.appointmentRepository = appointmentRepository;
        this.rabbitTemplate = rabbitTemplate;
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
        AppointmentEntity entity = new AppointmentEntity();
        entity.setId("apt-" + UUID.randomUUID().toString().substring(0, 8));
        entity.setClientId(request.getClientId() != null ? request.getClientId() : "cli-guest");
        entity.setClientName(request.getClientName() != null ? request.getClientName() : "Cliente Invitado");
        entity.setClientAvatar(request.getClientAvatar() != null ? request.getClientAvatar() : "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=300");
        entity.setServiceId(request.getServiceId());
        entity.setServiceName(request.getServiceName());
        entity.setServiceCategory(request.getServiceCategory());
        entity.setStylistId(request.getStylistId());
        entity.setStylistName(request.getStylistName());
        entity.setDate(request.getDate());
        entity.setTime(request.getTime());
        entity.setDurationMinutes(request.getDurationMinutes());
        entity.setPrice(request.getPrice());
        entity.setStatus("confirmada");
        entity.setNotes(request.getNotes());

        AppointmentEntity saved = appointmentRepository.save(entity);

        // Publicación de evento hacia el Clúster RabbitMQ (appointment.created.queue)
        AppointmentEvent event = new AppointmentEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType("CREATED");
        event.setAppointmentId(saved.getId());
        event.setClientId(saved.getClientId());
        event.setClientName(saved.getClientName());
        event.setServiceName(saved.getServiceName());
        event.setStylistName(saved.getStylistName());
        event.setDate(saved.getDate());
        event.setTime(saved.getTime());
        event.setStatus(saved.getStatus());
        event.setPrice(saved.getPrice());
        event.setSimulateError(request.isSimulateError() || (request.getNotes() != null && request.getNotes().contains("[DLQ_TEST]")));
        event.setTimestamp(Instant.now().toString());

        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.DIRECT_EXCHANGE,
                RabbitMQConfig.APPOINTMENT_CREATED_RK,
                event
            );
            System.out.println("✅ [RabbitMQ] Evento appointment.created publicado: " + saved.getId());
        } catch (Exception e) {
            System.err.println("⚠️ [RabbitMQ] No se pudo conectar al clúster RabbitMQ: " + e.getMessage());
        }

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
                    AppointmentEntity updated = appointmentRepository.save(apt);

                    // Publicación de evento de cambio de estado a RabbitMQ (appointment.status.queue)
                    AppointmentEvent statusEvent = new AppointmentEvent();
                    statusEvent.setEventId(UUID.randomUUID().toString());
                    statusEvent.setEventType("STATUS_UPDATED");
                    statusEvent.setAppointmentId(updated.getId());
                    statusEvent.setClientId(updated.getClientId());
                    statusEvent.setClientName(updated.getClientName());
                    statusEvent.setServiceName(updated.getServiceName());
                    statusEvent.setStylistName(updated.getStylistName());
                    statusEvent.setDate(updated.getDate());
                    statusEvent.setTime(updated.getTime());
                    statusEvent.setStatus(updated.getStatus());
                    statusEvent.setPrice(updated.getPrice());
                    statusEvent.setTimestamp(Instant.now().toString());

                    try {
                        rabbitTemplate.convertAndSend(
                            RabbitMQConfig.DIRECT_EXCHANGE,
                            RabbitMQConfig.APPOINTMENT_STATUS_RK,
                            statusEvent
                        );
                        System.out.println("✅ [RabbitMQ] Evento appointment.status publicado: " + updated.getId());
                    } catch (Exception e) {
                        System.err.println("⚠️ [RabbitMQ] No se pudo conectar al clúster RabbitMQ: " + e.getMessage());
                    }

                    return ResponseEntity.ok(updated);
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