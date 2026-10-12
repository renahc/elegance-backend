package com.elegance.appointment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta con información detallada de una cola en RabbitMQ")
public class QueueInfoResponse {

    @Schema(description = "Nombre de la cola", example = "appointment.created.queue")
    private String name;

    @Schema(description = "Indica si la cola existe activamente en el broker", example = "true")
    private boolean exists;

    @Schema(description = "Cantidad de mensajes listos para ser consumidos", example = "0")
    private Integer messageCount;

    @Schema(description = "Cantidad de consumidores conectados a la cola", example = "1")
    private Integer consumerCount;

    @Schema(description = "Propiedades y argumentos de la cola")
    private Map<String, Object> arguments;
}
