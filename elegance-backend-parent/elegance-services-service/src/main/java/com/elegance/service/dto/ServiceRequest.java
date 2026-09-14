package com.elegance.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class ServiceRequest {

    @NotBlank(message = "El nombre del servicio es obligatorio")
    private String name;

    private String description;

    @NotBlank(message = "La categoría es obligatoria")
    private String category;

    @NotNull(message = "La duración en minutos es obligatoria")
    private Integer durationMinutes;

    @NotNull(message = "El precio base es obligatorio")
    private BigDecimal basePrice;

    private String imageUrl;

    public ServiceRequest() {
    }

    public ServiceRequest(String name, String description, String category, Integer durationMinutes, BigDecimal basePrice, String imageUrl) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.durationMinutes = durationMinutes;
        this.basePrice = basePrice;
        this.imageUrl = imageUrl;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
