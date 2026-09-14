package com.elegance.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class RateRequest {

    @NotBlank(message = "El ID del servicio es obligatorio")
    private String serviceId;

    @NotBlank(message = "El nombre de la tarifa es obligatorio")
    private String name;

    @NotNull(message = "El precio de la tarifa es obligatorio")
    private BigDecimal price;

    private Double discountPercentage;

    public RateRequest() {
    }

    public RateRequest(String serviceId, String name, BigDecimal price, Double discountPercentage) {
        this.serviceId = serviceId;
        this.name = name;
        this.price = price;
        this.discountPercentage = discountPercentage;
    }

    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Double getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(Double discountPercentage) { this.discountPercentage = discountPercentage; }
}
