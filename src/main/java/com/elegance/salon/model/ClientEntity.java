package com.elegance.salon.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "clientes")
public class ClientEntity {

    @Id
    private String id;
    private String name;
    private String email;
    private String phone;
    private String avatar;
    private Integer totalVisits;
    private Integer totalSpent;
    private String lastVisit;
    private String tier;
    
    @Column(length = 1000)
    private String notes;

    public ClientEntity() {
    }

    public ClientEntity(String id, String name, String email, String phone, String avatar, Integer totalVisits, Integer totalSpent, String lastVisit, String tier, String notes) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.avatar = avatar;
        this.totalVisits = totalVisits;
        this.totalSpent = totalSpent;
        this.lastVisit = lastVisit;
        this.tier = tier;
        this.notes = notes;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public Integer getTotalVisits() { return totalVisits; }
    public void setTotalVisits(Integer totalVisits) { this.totalVisits = totalVisits; }

    public Integer getTotalSpent() { return totalSpent; }
    public void setTotalSpent(Integer totalSpent) { this.totalSpent = totalSpent; }

    public String getLastVisit() { return lastVisit; }
    public void setLastVisit(String lastVisit) { this.lastVisit = lastVisit; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
