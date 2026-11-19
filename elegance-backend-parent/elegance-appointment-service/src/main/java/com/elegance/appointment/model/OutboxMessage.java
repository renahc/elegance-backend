package com.elegance.appointment.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(indexes = @Index(columnList = "delivered,nextAttempt"))
public class OutboxMessage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public String destination;
  public String route;
  public String messageKey;

  @Lob
  @Column(columnDefinition = "LONGTEXT")
  public String payload;

  public boolean delivered;
  public int attempts;
  public Instant nextAttempt = Instant.EPOCH;

  @Column(length = 500)
  public String lastError;

  public OutboxMessage() {}

  public OutboxMessage(String destination, String route, String key, String payload) {
    this.destination = destination;
    this.route = route;
    messageKey = key;
    this.payload = payload;
  }
}
