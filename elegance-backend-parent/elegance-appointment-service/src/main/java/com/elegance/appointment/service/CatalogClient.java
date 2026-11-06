package com.elegance.appointment.service;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CatalogClient {
  private final RestClient services;
  private final RestClient users;

  public CatalogClient(
      @Value("${SERVICES_URL:http://localhost:8084}") String services,
      @Value("${USERS_URL:http://localhost:8082}") String users) {
    this.services = RestClient.create(services);
    this.users = RestClient.create(users);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> service(String id) {
    return services.get().uri("/api/v1/services/{id}", id).retrieve().body(Map.class);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> stylist(String id) {
    return users.get().uri("/api/v1/stylists/{id}", id).retrieve().body(Map.class);
  }
}
