package com.elegance.salon.controller;

import com.elegance.salon.model.ClientEntity;
import com.elegance.salon.repository.ClientRepository;
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
}
