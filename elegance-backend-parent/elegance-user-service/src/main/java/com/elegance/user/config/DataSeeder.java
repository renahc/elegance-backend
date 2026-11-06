package com.elegance.user.config;

import com.elegance.user.model.ClientEntity;
import com.elegance.user.model.StylistEntity;
import com.elegance.user.repository.ClientRepository;
import com.elegance.user.repository.StylistRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Entirely synthetic fixtures. No real people, contact details or health notes. */
@Component
@Profile("demo")
public class DataSeeder implements CommandLineRunner {
  private final ClientRepository clients;
  private final StylistRepository stylists;

  public DataSeeder(ClientRepository clients, StylistRepository stylists) {
    this.clients = clients;
    this.stylists = stylists;
  }

  @Override
  public void run(String... args) {
    if (clients.count() == 0) {
      for (int i = 1; i <= 5; i++) {
        var client = new ClientEntity();
        client.setId("demo-client-" + i);
        client.setName("Cliente de demostración " + i);
        client.setEmail("demo-client-" + i + "@example.invalid");
        client.setPhone("");
        client.setTotalVisits(0);
        client.setTotalSpent(0);
        client.setTier("Nuevo");
        client.setNotes("Fixture sintético para pruebas de software. No corresponde a una persona real.");
        clients.save(client);
      }
    }
    if (stylists.count() == 0) {
      for (int i = 1; i <= 4; i++) {
        var stylist = new StylistEntity();
        stylist.setId("demo-stylist-" + i);
        stylist.setName("Estilista de demostración " + i);
        stylist.setRole("Estilista de demostración");
        stylist.setSpecialty("Servicios del catálogo de prueba");
        stylist.setAvatar("https://images.unsplash.com/photo-1560066984-138dadb4c035?auto=format&fit=crop&w=300");
        stylist.setRating(0.0);
        stylist.setReviewsCount(0);
        stylist.setIsAvailable(true);
        stylist.setShift("09:00 - 18:00");
        stylist.setCompletedTodayCount(0);
        stylists.save(stylist);
      }
    }
  }
}
