package com.elegance.user.controller;

import com.elegance.user.event.UserCreatedEvent;
import com.elegance.user.event.UserUpdatedEvent;
import com.elegance.user.event.UserDeletedEvent;
import com.elegance.user.model.ClientEntity;
import com.elegance.user.repository.ClientRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/clients")
@Tag(
    name = "Clientes (CRM)",
    description = "Endpoints para consultar y administrar la base de clientes del salón")
public class ClientController {

  private final ClientRepository clientRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Autowired
  public ClientController(
      ClientRepository clientRepository, ApplicationEventPublisher eventPublisher) {
    this.clientRepository = clientRepository;
    this.eventPublisher = eventPublisher;
  }

  @GetMapping("/me")
  public ClientEntity me(org.springframework.security.core.Authentication auth) {
    var jwt = (org.springframework.security.oauth2.jwt.Jwt) auth.getPrincipal();
    String oid =
        jwt.getClaimAsString("oid") != null ? jwt.getClaimAsString("oid") : jwt.getSubject();
    return clientRepository
        .findById("cli-" + oid)
        .orElseGet(
            () -> {
              var client = new ClientEntity();
              client.setId("cli-" + oid);
              client.setName(jwt.getClaimAsString("name"));
              client.setEmail(
                  jwt.getClaimAsString("preferred_username") != null
                      ? jwt.getClaimAsString("preferred_username")
                      : jwt.getClaimAsString("upn"));
              client.setTier("Nuevo");
              client.setTotalVisits(0);
              client.setTotalSpent(0);
              return clientRepository.save(client);
            });
  }

  @GetMapping
  @Operation(summary = "Obtener listado de clientes o buscar por nombre")
  public ResponseEntity<List<ClientEntity>> getClients(
      @RequestParam(required = false) String search) {
    if (search != null && !search.isBlank()) {
      return ResponseEntity.ok(clientRepository.findByNameContainingIgnoreCase(search));
    }
    return ResponseEntity.ok(clientRepository.findAll());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Obtener cliente por ID")
  public ResponseEntity<ClientEntity> getClientById(@PathVariable String id) {
    return clientRepository
        .findById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  @Transactional
  @Operation(summary = "Registrar nuevo cliente CRM")
  public ResponseEntity<ClientEntity> createClient(
      @jakarta.validation.Valid @RequestBody ClientEntity client) {
    if (client.getId() == null) {
      client.setId("cli-" + UUID.randomUUID().toString().substring(0, 8));
    }
    if (client.getTier() == null) {
      client.setTier("Nuevo");
    }
    ClientEntity saved = clientRepository.save(client);
    eventPublisher.publishEvent(
        new UserCreatedEvent(
            saved.getId(),
            saved.getName(),
            saved.getEmail(),
            "CLIENT",
            LocalDateTime.now()));
    return ResponseEntity.status(HttpStatus.CREATED).body(saved);
  }

  @PutMapping("/{id}")
  @Transactional
  @Operation(summary = "Actualizar cliente existente")
  public ResponseEntity<ClientEntity> updateClient(
      @PathVariable String id, @jakarta.validation.Valid @RequestBody ClientEntity clientDetails) {
    return clientRepository
        .findById(id)
        .map(
            client -> {
              client.setName(clientDetails.getName());
              client.setEmail(clientDetails.getEmail());
              client.setPhone(clientDetails.getPhone());
              client.setAvatar(clientDetails.getAvatar());
              client.setTotalVisits(clientDetails.getTotalVisits());
              client.setTotalSpent(clientDetails.getTotalSpent());
              client.setLastVisit(clientDetails.getLastVisit());
              client.setTier(clientDetails.getTier());
              client.setNotes(clientDetails.getNotes());

              ClientEntity updated = clientRepository.save(client);
              eventPublisher.publishEvent(
                  new UserUpdatedEvent(
                      updated.getId(),
                      updated.getName(),
                      updated.getEmail(),
                      "CLIENT",
                      LocalDateTime.now()));
              return ResponseEntity.ok(updated);
            })
        .orElse(ResponseEntity.notFound().build());
  }

  @DeleteMapping("/{id}")
  @Transactional
  @Operation(summary = "Eliminar cliente permanentemente")
  public ResponseEntity<Void> deleteClient(@PathVariable String id) {
    if (clientRepository.existsById(id)) {
      clientRepository.deleteById(id);
      eventPublisher.publishEvent(
          new UserDeletedEvent(
              id,
              LocalDateTime.now()));
      return ResponseEntity.noContent().build();
    }
    return ResponseEntity.notFound().build();
  }
}
