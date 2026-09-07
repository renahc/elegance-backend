package com.elegance.salon.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "servicios")
public class ServiceEntity {

    @Id
    private String id;
    private String name;
    private String category;
    private Integer price;
    private Integer durationMinutes;
    private Boolean popular;
    private Boolean active;

    public ServiceEntity() {
    }

    public ServiceEntity(String id, String name, String category, Integer price, Integer durationMinutes, Boolean popular, Boolean active) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.durationMinutes = durationMinutes;
        this.popular = popular;
        this.active = active;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public Boolean getPopular() { return popular; }
    public void setPopular(Boolean popular) { this.popular = popular; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
