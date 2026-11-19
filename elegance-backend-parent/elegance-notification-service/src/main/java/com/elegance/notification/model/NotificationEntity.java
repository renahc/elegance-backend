package com.elegance.notification.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class NotificationEntity {
  @Id public String id;
  public String title;

  @Column(length = 2000)
  public String message;

  public String type;
  public String appointmentId;
  public Instant timestamp;

  public NotificationEntity() {}
}
