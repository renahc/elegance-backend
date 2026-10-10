package com.elegance.notification.listener;

import com.elegance.notification.config.RabbitMQConfig;
import com.elegance.notification.dto.AppointmentEvent;
import com.elegance.notification.service.NotificationStoreService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AppointmentEventListener {

    private final NotificationStoreService notificationStoreService;

    @Autowired
    public AppointmentEventListener(NotificationStoreService notificationStoreService) {
        this.notificationStoreService = notificationStoreService;
    }

    @RabbitListener(queues = RabbitMQConfig.APPOINTMENT_CREATED_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handleAppointmentCreated(AppointmentEvent event) {
        System.out.println("📥 [RabbitMQ Consumidor] Recibido evento appointment.created para cita: " + event.getAppointmentId());

        // Verificación de prueba de Dead Letter Queue (DLQ)
        if (event.isSimulateError()) {
            System.err.println("❌ [DLQ TEST] simulateError=true detectado para cita " + event.getAppointmentId() +
                    ". Lanzando excepción deliberada para activar reintentos y derivar a Dead Letter Queue (DLQ)...");
            throw new RuntimeException("Error simulado intencionalmente para demostrar funcionamiento de la Dead Letter Queue (DLQ) en la evaluación");
        }

        String title = "Nueva Cita Agendada";
        String message = String.format("Cita agendada para %s con %s el %s a las %s.",
                event.getClientName() != null ? event.getClientName() : "Cliente",
                event.getStylistName() != null ? event.getStylistName() : "Estilista",
                event.getDate() != null ? event.getDate() : "Fecha",
                event.getTime() != null ? event.getTime() : "Hora");

        notificationStoreService.addNotification(title, message, "appointment_created", event.getAppointmentId());
        System.out.println("✅ [RabbitMQ Consumidor] Notificación registrada exitosamente para: " + event.getAppointmentId());
    }

    @RabbitListener(queues = RabbitMQConfig.APPOINTMENT_STATUS_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handleAppointmentStatusUpdated(AppointmentEvent event) {
        System.out.println("📥 [RabbitMQ Consumidor] Recibido evento appointment.status para cita: " + event.getAppointmentId() +
                " -> Nuevo estado: " + event.getStatus());

        String title = "Estado de Cita Actualizado";
        String message = String.format("La cita de %s ha cambiado a estado: %s",
                event.getClientName() != null ? event.getClientName() : "Cliente",
                event.getStatus() != null ? event.getStatus() : "actualizado");

        notificationStoreService.addNotification(title, message, "appointment_status", event.getAppointmentId());
        System.out.println("✅ [RabbitMQ Consumidor] Cambio de estado notificado para: " + event.getAppointmentId());
    }
}
