package com.elegance.user.controller;

import com.elegance.user.model.ClientEntity;
import com.elegance.user.repository.ClientRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clients")
@Tag(name = "Clientes (CRM)", description = "Endpoints para consultar y administrar la base de clientes del salón")
public class ClientController {

    private final ClientRepository clientRepository;

    @Autowired
    public ClientController(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @GetMapping
    @Operation(summary = "Obtener listado de clientes o buscar por nombre")
    public ResponseEntity<List<ClientEntity>> getClients(@RequestParam(required = false) String search) {
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(clientRepository.findByNameContainingIgnoreCase(search));
        }
        return ResponseEntity.ok(clientRepository.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener cliente por ID")
    public ResponseEntity<ClientEntity> getClientById(@PathVariable String id) {
        return clientRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Registrar nuevo cliente CRM")
    public ResponseEntity<ClientEntity> createClient(@RequestBody ClientEntity client) {
        if (client.getId() == null) {
            client.setId("cli-" + UUID.randomUUID().toString().substring(0, 8));
        }
        if (client.getTier() == null) {
            client.setTier("Nuevo");
        }
        ClientEntity saved = clientRepository.save(client);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar cliente existente")
    public ResponseEntity<ClientEntity> updateClient(@PathVariable String id, @RequestBody ClientEntity clientDetails) {
        return clientRepository.findById(id)
                .map(client -> {
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
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar cliente permanentemente")
    public ResponseEntity<Void> deleteClient(@PathVariable String id) {
        if (clientRepository.existsById(id)) {
            clientRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}