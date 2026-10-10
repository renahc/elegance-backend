package com.elegance.service.controller;

import com.elegance.service.dto.ServiceRequest;
import com.elegance.service.model.ServiceEntity;
import com.elegance.service.repository.ServiceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/services")
@Tag(name = "Catálogo de Servicios", description = "Endpoints para la gestión de los servicios ofertados")
public class ServiceController {

    private final ServiceRepository serviceRepository;

    @Autowired
    public ServiceController(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @GetMapping
    @Operation(summary = "Listar todos los servicios del catálogo")
    public ResponseEntity<List<ServiceEntity>> getAllServices(@RequestParam(required = false) String category) {
        if (category != null && !category.isBlank()) {
            return ResponseEntity.ok(serviceRepository.findByCategory(category));
        }
        return ResponseEntity.ok(serviceRepository.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalles de un servicio por ID")
    public ResponseEntity<ServiceEntity> getServiceById(@PathVariable String id) {
        return serviceRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Registrar un nuevo servicio en el catálogo")
    public ResponseEntity<ServiceEntity> createService(@Valid @RequestBody ServiceRequest request) {
        ServiceEntity entity = new ServiceEntity();
        entity.setId("srv-" + UUID.randomUUID().toString().substring(0, 8));
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setCategory(request.getCategory());
        entity.setDurationMinutes(request.getDurationMinutes());
        entity.setBasePrice(request.getBasePrice());
        entity.setImageUrl(request.getImageUrl());
        entity.setActive(true);

        ServiceEntity saved = serviceRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar información de un servicio")
    public ResponseEntity<ServiceEntity> updateService(@PathVariable String id, @Valid @RequestBody ServiceRequest request) {
        return serviceRepository.findById(id)
                .map(srv -> {
                    srv.setName(request.getName());
                    srv.setDescription(request.getDescription());
                    srv.setCategory(request.getCategory());
                    srv.setDurationMinutes(request.getDurationMinutes());
                    srv.setBasePrice(request.getBasePrice());
                    if (request.getImageUrl() != null) {
                        srv.setImageUrl(request.getImageUrl());
                    }
                    return ResponseEntity.ok(serviceRepository.save(srv));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar (desactivar) un servicio del catálogo")
    public ResponseEntity<Void> deleteService(@PathVariable String id) {
        return serviceRepository.findById(id)
                .map(srv -> {
                    srv.setActive(false);
                    serviceRepository.save(srv);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
