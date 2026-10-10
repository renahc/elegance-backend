package com.elegance.appointment.dto;

import java.io.Serializable;

public class AppointmentEvent implements Serializable {
    private String eventId;
    private String eventType; // "CREATED", "STATUS_UPDATED", "CANCELLED"
    private String appointmentId;
    private String clientId;
    private String clientName;
    private String serviceName;
    private String stylistName;
    private String date;
    private String time;
    private String status;
    private Integer price;
    private boolean simulateError;
    private String timestamp;

    public AppointmentEvent() {}

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getStylistName() { return stylistName; }
    public void setStylistName(String stylistName) { this.stylistName = stylistName; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }

    public boolean isSimulateError() { return simulateError; }
    public void setSimulateError(boolean simulateError) { this.simulateError = simulateError; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
