package com.elegance.user.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "estilistas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    public String getSpecialty() {
        return specialty;
    }

    public String getAvatar() {
        return avatar;
    }

    public Double getRating() {
        return rating;
    }

    public Integer getReviewsCount() {
        return reviewsCount;
    }

    public Boolean getIsAvailable() {
        return isAvailable;
    }

    public String getShift() {
        return shift;
    }

    public Integer getCompletedTodayCount() {
        return completedTodayCount;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public void setReviewsCount(Integer reviewsCount) {
        this.reviewsCount = reviewsCount;
    }

    public void setIsAvailable(Boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    public void setShift(String shift) {
        this.shift = shift;
    }

    public void setCompletedTodayCount(Integer completedTodayCount) {
        this.completedTodayCount = completedTodayCount;
    }


}