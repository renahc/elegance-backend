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
@Schema(description = "Solicitud para declarar o crear un exchange en RabbitMQ")
public class ExchangeDefinitionRequest {

    @NotBlank(message = "El nombre del exchange es obligatorio y no puede estar vacío.")
    @Size(min = 3, max = 100, message = "El nombre del exchange debe tener entre 3 y 100 caracteres.")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El nombre del exchange solo puede contener letras, números, puntos, guiones y guiones bajos.")
    @Schema(description = "Nombre único del exchange", example = "elegance.events.exchange")
    private String name;

    @NotBlank(message = "El tipo de exchange es obligatorio.")
    @Pattern(regexp = "^(direct|topic|fanout|headers)$", message = "El tipo de exchange debe ser: direct, topic, fanout o headers.")
    @Builder.Default
    @Schema(description = "Tipo de enrutamiento del exchange (direct, topic, fanout, headers)", example = "direct")
    private String type = "direct";

    @Builder.Default
    @Schema(description = "Indica si el exchange sobrevive a reinicios del broker", example = "true")
    private boolean durable = true;

    @Builder.Default
    @Schema(description = "Indica si el exchange se auto-elimina cuando se desenlazan todas las colas", example = "false")
    private boolean autoDelete = false;

    @Schema(description = "Argumentos avanzados opcionales para el exchange")
    private Map<String, Object> arguments;
}
