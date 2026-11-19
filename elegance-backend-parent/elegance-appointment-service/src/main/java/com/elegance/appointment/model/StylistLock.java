package com.elegance.appointment.model;

@jakarta.persistence.Entity
public class StylistLock {
  @jakarta.persistence.Id public String id;

  public StylistLock() {}

  public StylistLock(String id) {
    this.id = id;
  }
}
