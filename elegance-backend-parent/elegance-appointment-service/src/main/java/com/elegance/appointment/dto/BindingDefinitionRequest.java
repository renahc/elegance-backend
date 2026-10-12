package com.elegance.appointment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para crear o eliminar un binding entre cola y exchange en RabbitMQ")
public class BindingDefinitionRequest {

    @NotBlank(message = "El nombre de la cola es obligatorio.")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El nombre de la cola contiene caracteres inválidos.")
    @Schema(description = "Nombre de la cola de destino", example = "billing.invoice.queue")
    private String queueName;

    @NotBlank(message = "El nombre del exchange es obligatorio.")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El nombre del exchange contiene caracteres inválidos.")
    @Schema(description = "Nombre del exchange de origen", example = "elegance.events.exchange")
    private String exchangeName;

    @NotBlank(message = "La routing key es obligatoria.")
    @Schema(description = "Clave de enrutamiento (Routing Key)", example = "billing.invoice.created")
    private String routingKey;

    @Schema(description = "Argumentos opcionales para el binding (útil para headers exchange)")
    private Map<String, Object> arguments;
}
