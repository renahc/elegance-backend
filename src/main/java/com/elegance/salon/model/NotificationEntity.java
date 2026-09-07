package com.elegance.salon.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "notificaciones")
public class NotificationEntity {

    @Id
    private String id;
    private String title;
    private String message;
    private String time;
    private Boolean read;
    private String type;

    public NotificationEntity() {
    }

    public NotificationEntity(String id, String title, String message, String time, Boolean read, String type) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.time = time;
        this.read = read;
        this.type = type;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public Boolean getRead() { return read; }
    public void setRead(Boolean read) { this.read = read; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
