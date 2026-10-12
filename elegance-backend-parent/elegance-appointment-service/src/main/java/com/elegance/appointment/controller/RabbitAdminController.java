package com.elegance.appointment.controller;

import com.elegance.appointment.dto.BindingDefinitionRequest;
import com.elegance.appointment.dto.ExchangeDefinitionRequest;
import com.elegance.appointment.dto.QueueDefinitionRequest;
import com.elegance.appointment.dto.QueueInfoResponse;
import com.elegance.appointment.service.RabbitAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@Tag(name = "RabbitMQ Admin API", description = "Endpoints de administración para crear, consultar y eliminar Colas, Exchanges y Bindings en RabbitMQ")
@CrossOrigin(origins = "*")
public class RabbitAdminController {

    private final RabbitAdminService rabbitAdminService;

    @Autowired
    public RabbitAdminController(RabbitAdminService rabbitAdminService) {
        this.rabbitAdminService = rabbitAdminService;
    }

    // =========================================================================
    // 1. GESTIÓN DE COLAS (QUEUES)
    // =========================================================================

    @PostMapping({"/api/v1/admin/queues", "/queues"})
    @Operation(summary = "Crear o declarar una nueva cola en RabbitMQ",
            description = "Permite declarar colas durables, temporales y configurar Dead Letter Exchanges (DLX)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cola creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Parámetros de entrada inválidos o nombre vacío")
    })
    public ResponseEntity<Map<String, Object>> createQueue(@Valid @RequestBody QueueDefinitionRequest request) {
        String queueName = rabbitAdminService.createQueue(request);

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("status", HttpStatus.CREATED.value());
        response.put("message", "Cola declarada exitosamente en el clúster RabbitMQ");
        response.put("queueName", queueName);
        response.put("durable", request.isDurable());
        response.put("deadLetterExchange", request.getDeadLetterExchange());
        response.put("deadLetterRoutingKey", request.getDeadLetterRoutingKey());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping({"/api/v1/admin/queues/{name}", "/queues/{name}"})
    @Operation(summary = "Consultar estadísticas y estado de una cola",
            description = "Retorna si la cola existe, número de mensajes en cola y número de consumidores activos")
    public ResponseEntity<QueueInfoResponse> getQueue(
            @Parameter(description = "Nombre de la cola", example = "appointment.created.queue")
            @PathVariable("name") String name) {
        QueueInfoResponse info = rabbitAdminService.getQueueInfo(name);
        return ResponseEntity.ok(info);
    }

    @DeleteMapping({"/api/v1/admin/queues/{name}", "/queues/{name}"})
    @Operation(summary = "Eliminar una cola existente en RabbitMQ",
            description = "Elimina la cola especificada del clúster")
    public ResponseEntity<Map<String, Object>> deleteQueue(
            @Parameter(description = "Nombre de la cola a eliminar", example = "billing.invoice.queue")
            @PathVariable("name") String name,
            @RequestParam(value = "ifUnused", defaultValue = "false") boolean ifUnused,
            @RequestParam(value = "ifEmpty", defaultValue = "false") boolean ifEmpty) {

        boolean deleted = rabbitAdminService.deleteQueue(name, ifUnused, ifEmpty);

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("status", HttpStatus.OK.value());
        response.put("message", "Cola eliminada exitosamente");
        response.put("queueName", name);
        response.put("deleted", deleted);

        return ResponseEntity.ok(response);
    }

    @PostMapping({"/api/v1/admin/queues/{name}/purge", "/queues/{name}/purge"})
    @Operation(summary = "Purgar mensajes de una cola",
            description = "Elimina todos los mensajes pendientes en la cola sin destruirla")
    public ResponseEntity<Map<String, Object>> purgeQueue(
            @Parameter(description = "Nombre de la cola", example = "appointment.created.queue")
            @PathVariable("name") String name) {

        int purgedCount = rabbitAdminService.purgeQueue(name);

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("status", HttpStatus.OK.value());
        response.put("message", "Cola purgada exitosamente");
        response.put("queueName", name);
        response.put("purgedMessages", purgedCount);

        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // 2. GESTIÓN DE EXCHANGES
    // =========================================================================

    @PostMapping({"/api/v1/admin/exchanges", "/exchanges"})
    @Operation(summary = "Crear o declarar un exchange en RabbitMQ",
            description = "Soporta tipos direct, topic, fanout y headers")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Exchange creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos o tipo no soportado")
    })
    public ResponseEntity<Map<String, Object>> createExchange(@Valid @RequestBody ExchangeDefinitionRequest request) {
        rabbitAdminService.createExchange(request);

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("status", HttpStatus.CREATED.value());
        response.put("message", "Exchange declarado exitosamente en el clúster RabbitMQ");
        response.put("exchangeName", request.getName());
        response.put("type", request.getType());
        response.put("durable", request.isDurable());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping({"/api/v1/admin/exchanges/{name}", "/exchanges/{name}"})
    @Operation(summary = "Eliminar un exchange de RabbitMQ")
    public ResponseEntity<Map<String, Object>> deleteExchange(
            @Parameter(description = "Nombre del exchange", example = "elegance.events.exchange")
            @PathVariable("name") String name) {

        boolean deleted = rabbitAdminService.deleteExchange(name);

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("status", HttpStatus.OK.value());
        response.put("message", "Exchange eliminado exitosamente");
        response.put("exchangeName", name);
        response.put("deleted", deleted);

        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // 3. GESTIÓN DE ENLACES (BINDINGS)
    // =========================================================================

    @PostMapping({"/api/v1/admin/bindings", "/bindings"})
    @Operation(summary = "Crear un enlace (Binding) entre una cola y un exchange",
            description = "Conecta la cola y el exchange mediante una Routing Key específica")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Binding creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Parámetros de enlace inválidos")
    })
    public ResponseEntity<Map<String, Object>> createBinding(@Valid @RequestBody BindingDefinitionRequest request) {
        rabbitAdminService.createBinding(request);

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("status", HttpStatus.CREATED.value());
        response.put("message", "Binding creado exitosamente");
        response.put("queueName", request.getQueueName());
        response.put("exchangeName", request.getExchangeName());
        response.put("routingKey", request.getRoutingKey());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping({"/api/v1/admin/bindings", "/bindings"})
    @Operation(summary = "Eliminar un enlace (Binding) existente",
            description = "Desvincula la cola del exchange")
    public ResponseEntity<Map<String, Object>> deleteBinding(@Valid @RequestBody BindingDefinitionRequest request) {
        rabbitAdminService.deleteBinding(request);

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("status", HttpStatus.OK.value());
        response.put("message", "Binding eliminado exitosamente");
        response.put("queueName", request.getQueueName());
        response.put("exchangeName", request.getExchangeName());
        response.put("routingKey", request.getRoutingKey());

        return ResponseEntity.ok(response);
    }
}
