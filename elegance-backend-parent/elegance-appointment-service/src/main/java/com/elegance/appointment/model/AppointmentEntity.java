package com.elegance.appointment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "citas")
public class AppointmentEntity {

    @Id
    private String id;
    
    private String clientId;
    private String clientName;
    private String clientAvatar;
    private String serviceId;
    private String serviceName;
    private String serviceCategory;
    private String stylistId;
    private String stylistName;
    
    // Usamos backticks (`) para evitar conflictos con palabras reservadas de SQL
    @Column(name = "`date`")
    private String date;
    
    @Column(name = "`time`")
    private String time;
    
    private Integer durationMinutes;
    private Integer price;
    private String status;

    @Column(length = 1000)
    private String notes;

    public AppointmentEntity() {}

    // ... (mantén todos los getters y setters que ya tienes) ...
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public String getClientAvatar() { return clientAvatar; }
    public void setClientAvatar(String clientAvatar) { this.clientAvatar = clientAvatar; }
    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getServiceCategory() { return serviceCategory; }
    public void setServiceCategory(String serviceCategory) { this.serviceCategory = serviceCategory; }
    public String getStylistId() { return stylistId; }
    public void setStylistId(String stylistId) { this.stylistId = stylistId; }
    public String getStylistName() { return stylistName; }
    public void setStylistName(String stylistName) { this.stylistName = stylistName; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}