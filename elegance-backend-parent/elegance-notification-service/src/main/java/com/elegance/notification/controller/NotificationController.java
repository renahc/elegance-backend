package com.elegance.notification.controller;

import com.elegance.notification.config.RabbitMQConfig;
import com.elegance.notification.dto.AppointmentEvent;
import com.elegance.notification.dto.EmailRequest;
import com.elegance.notification.service.EmailService;
import com.elegance.notification.service.NotificationStoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notificaciones", description = "Endpoints para el envío de notificaciones y gestión de eventos RabbitMQ")
public class NotificationController {

    private final EmailService emailService;
    private final NotificationStoreService notificationStoreService;
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public NotificationController(EmailService emailService,
                                  NotificationStoreService notificationStoreService,
                                  RabbitTemplate rabbitTemplate) {
        this.emailService = emailService;
        this.notificationStoreService = notificationStoreService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @GetMapping
    @Operation(summary = "Obtener todas las notificaciones recibidas desde RabbitMQ")
    public ResponseEntity<List<Map<String, Object>>> getAllNotifications() {
        return ResponseEntity.ok(notificationStoreService.getAllNotifications());
    }

    @PostMapping("/email")
    @Operation(summary = "Enviar un correo electrónico")
    public ResponseEntity<Map<String, String>> sendEmail(@RequestBody EmailRequest request) {
        Map<String, String> response = new HashMap<>();

        if (request == null || request.getTo() == null || request.getTo().isBlank()) {
            response.put("error", "El campo 'to' es obligatorio.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            emailService.sendEmail(request);
            response.put("message", "Correo enviado exitosamente a " + request.getTo());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Error al enviar correo electrónico: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/test-dlq")
    @Operation(summary = "Disparar mensaje de prueba con error deliberado para evidenciar Dead Letter Queue (DLQ)")
    public ResponseEntity<Map<String, Object>> triggerDlqTest() {
        AppointmentEvent testEvent = new AppointmentEvent();
        testEvent.setEventId(UUID.randomUUID().toString());
        testEvent.setEventType("CREATED");
        testEvent.setAppointmentId("dlq-test-" + System.currentTimeMillis());
        testEvent.setClientId("cli-test");
        testEvent.setClientName("Prueba Evaluación Duoc");
        testEvent.setServiceName("Corte & Tratamiento DLQ");
        testEvent.setStylistName("Estilista Demo");
        testEvent.setDate("2026-10-10");
        testEvent.setTime("16:00");
        testEvent.setStatus("pendiente");
        testEvent.setPrice(35000);
        testEvent.setSimulateError(true); // Activa el fallo deliberado
        testEvent.setTimestamp(Instant.now().toString());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.DIRECT_EXCHANGE,
                RabbitMQConfig.APPOINTMENT_CREATED_RK,
                testEvent
        );

        Map<String, Object> result = new HashMap<>();
        result.put("status", "Mensaje enviado a RabbitMQ con simulateError=true");
        result.put("eventId", testEvent.getEventId());
        result.put("exchange", RabbitMQConfig.DIRECT_EXCHANGE);
        result.put("routingKey", RabbitMQConfig.APPOINTMENT_CREATED_RK);
        result.put("expectedDlq", RabbitMQConfig.APPOINTMENT_CREATED_DLQ);
        result.put("description", "El consumidor fallará 3 reintentos y el mensaje será transferido automáticamente a appointment.created.dlq.");

        return ResponseEntity.ok(result);
    }
}