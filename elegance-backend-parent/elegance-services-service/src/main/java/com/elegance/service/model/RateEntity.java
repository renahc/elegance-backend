package com.elegance.service.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "rates")
public class RateEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "service_id", nullable = false, length = 64)
    private String serviceId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "discount_percentage")
    private Double discountPercentage;

    @Column(nullable = false)
    private Boolean active = true;

    public RateEntity() {
    }

    public RateEntity(String id, String serviceId, String name, BigDecimal price, Double discountPercentage, Boolean active) {
        this.id = id;
        this.serviceId = serviceId;
        this.name = name;
        this.price = price;
        this.discountPercentage = discountPercentage;
        this.active = active;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Double getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(Double discountPercentage) { this.discountPercentage = discountPercentage; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
