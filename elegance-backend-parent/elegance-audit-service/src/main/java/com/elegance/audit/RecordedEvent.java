package com.elegance.audit;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(indexes = {@Index(columnList = "appointmentId"), @Index(columnList = "occurredAt")})
public class RecordedEvent {
  @Id public String eventId;
  public String topic;
  public int partitionNumber;
  public long eventOffset;
  public String appointmentId;
  public long appointmentVersion;
  public String eventType;
  public String status;
  public String category;
  public String stylistId;
  public String serviceId;
  public String appointmentDate;
  public String appointmentTime;
  public Integer amount;
  public Instant occurredAt;

  @Lob
  @Column(columnDefinition = "LONGTEXT")
  public String payload;

  public RecordedEvent() {}
}
