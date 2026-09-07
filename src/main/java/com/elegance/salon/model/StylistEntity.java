package com.elegance.salon.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "estilistas")
public class StylistEntity {

    @Id
    private String id;
    private String name;
    private String role;
    private String specialty;
    private String avatar;
    private Double rating;
    private Integer reviewsCount;
    private Boolean isAvailable;
    private String shift;
    private Integer completedTodayCount;

    public StylistEntity() {
    }

    public StylistEntity(String id, String name, String role, String specialty, String avatar, Double rating, Integer reviewsCount, Boolean isAvailable, String shift, Integer completedTodayCount) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.specialty = specialty;
        this.avatar = avatar;
        this.rating = rating;
        this.reviewsCount = reviewsCount;
        this.isAvailable = isAvailable;
        this.shift = shift;
        this.completedTodayCount = completedTodayCount;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public Integer getReviewsCount() { return reviewsCount; }
    public void setReviewsCount(Integer reviewsCount) { this.reviewsCount = reviewsCount; }

    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }

    public String getShift() { return shift; }
    public void setShift(String shift) { this.shift = shift; }

    public Integer getCompletedTodayCount() { return completedTodayCount; }
    public void setCompletedTodayCount(Integer completedTodayCount) { this.completedTodayCount = completedTodayCount; }
}
