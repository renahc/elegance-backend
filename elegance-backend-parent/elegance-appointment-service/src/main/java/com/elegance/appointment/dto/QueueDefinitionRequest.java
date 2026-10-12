package com.elegance.appointment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para declarar o crear una cola en RabbitMQ")
public class QueueDefinitionRequest {

    @NotBlank(message = "El nombre de la cola es obligatorio y no puede estar vacío.")
    @Size(min = 3, max = 100, message = "El nombre de la cola debe tener entre 3 y 100 caracteres.")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El nombre de la cola solo puede contener letras, números, puntos, guiones y guiones bajos.")
    @Schema(description = "Nombre único de la cola", example = "billing.invoice.queue")
    private String name;

    @Builder.Default
    @Schema(description = "Indica si la cola sobrevive a reinicios del broker", example = "true")
    private boolean durable = true;

    @Builder.Default
    @Schema(description = "Indica si la cola es exclusiva para la conexión actual", example = "false")
    private boolean exclusive = false;

    @Builder.Default
    @Schema(description = "Indica si la cola se elimina cuando no hay consumidores", example = "false")
    private boolean autoDelete = false;

    @Schema(description = "Dead Letter Exchange asociado en caso de rechazo o TTL expirado", example = "elegance.dlx.exchange")
    private String deadLetterExchange;

    @Schema(description = "Routing Key de destino en el Dead Letter Exchange", example = "billing.invoice.dlq")
    private String deadLetterRoutingKey;

    @Schema(description = "Argumentos avanzados opcionales de la cola (x-message-ttl, x-max-length, etc.)")
    private Map<String, Object> arguments;
}
