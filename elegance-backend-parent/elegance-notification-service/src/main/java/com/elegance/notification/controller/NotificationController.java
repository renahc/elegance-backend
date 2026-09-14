package com.elegance.notification.controller;

import com.elegance.notification.dto.EmailRequest;
import com.elegance.notification.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notificaciones", description = "Endpoints para el envío de correos electrónicos")
public class NotificationController {

    @Autowired
    private EmailService emailService;

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
}