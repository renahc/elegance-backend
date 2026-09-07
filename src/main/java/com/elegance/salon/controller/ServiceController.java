package com.elegance.salon.controller;

import com.elegance.salon.model.ServiceEntity;
import com.elegance.salon.repository.ServiceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/services")
@Tag(name = "Catálogo de Servicios", description = "Endpoints para consultar y administrar los servicios del salón")
public class ServiceController {

    private final ServiceRepository serviceRepository;

    @Autowired
    public ServiceController(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @GetMapping
    @Operation(summary = "Obtener catálogo completo de servicios")
    public ResponseEntity<List<ServiceEntity>> getAllServices(@RequestParam(required = false) String category) {
        if (category != null && !category.equalsIgnoreCase("all")) {
            return ResponseEntity.ok(serviceRepository.findByCategory(category));
        }
        return ResponseEntity.ok(serviceRepository.findAll());
    }

    @PostMapping
    @Operation(summary = "Registrar nuevo servicio")
    public ResponseEntity<ServiceEntity> createService(@RequestBody ServiceEntity service) {
        if (service.getId() == null) {
            service.setId("srv-" + UUID.randomUUID().toString().substring(0, 8));
        }
        ServiceEntity saved = serviceRepository.save(service);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}/toggle")
    @Operation(summary = "Activar o desactivar un servicio")
    public ResponseEntity<ServiceEntity> toggleServiceActive(@PathVariable String id) {
        return serviceRepository.findById(id)
                .map(srv -> {
                    srv.setActive(!Boolean.TRUE.equals(srv.getActive()));
                    return ResponseEntity.ok(serviceRepository.save(srv));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
