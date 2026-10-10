package com.elegance.service.controller;

import com.elegance.service.dto.RateRequest;
import com.elegance.service.model.RateEntity;
import com.elegance.service.repository.RateRepository;
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
@RequestMapping("/api/v1/rates")
@Tag(name = "Gestión de Tarifas", description = "Endpoints para la administración de tarifarios y descuentos")
public class RateController {

    private final RateRepository rateRepository;

    @Autowired
    public RateController(RateRepository rateRepository) {
        this.rateRepository = rateRepository;
    }

    @GetMapping
    @Operation(summary = "Listar todas las tarifas activas")
    public ResponseEntity<List<RateEntity>> getAllRates(@RequestParam(required = false) String serviceId) {
        if (serviceId != null && !serviceId.isBlank()) {
            return ResponseEntity.ok(rateRepository.findByServiceId(serviceId));
        }
        return ResponseEntity.ok(rateRepository.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalles de una tarifa por ID")
    public ResponseEntity<RateEntity> getRateById(@PathVariable String id) {
        return rateRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Crear una nueva tarifa para un servicio")
    public ResponseEntity<RateEntity> createRate(@Valid @RequestBody RateRequest request) {
        RateEntity entity = new RateEntity();
        entity.setId("rate-" + UUID.randomUUID().toString().substring(0, 8));
        entity.setServiceId(request.getServiceId());
        entity.setName(request.getName());
        entity.setPrice(request.getPrice());
        entity.setDiscountPercentage(request.getDiscountPercentage() != null ? request.getDiscountPercentage() : 0.0);
        entity.setActive(true);

        RateEntity saved = rateRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una tarifa existente")
    public ResponseEntity<RateEntity> updateRate(@PathVariable String id, @Valid @RequestBody RateRequest request) {
        return rateRepository.findById(id)
                .map(rate -> {
                    rate.setServiceId(request.getServiceId());
                    rate.setName(request.getName());
                    rate.setPrice(request.getPrice());
                    if (request.getDiscountPercentage() != null) {
                        rate.setDiscountPercentage(request.getDiscountPercentage());
                    }
                    return ResponseEntity.ok(rateRepository.save(rate));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar (desactivar) una tarifa por ID")
    public ResponseEntity<Void> deleteRate(@PathVariable String id) {
        return rateRepository.findById(id)
                .map(rate -> {
                    rate.setActive(false);
                    rateRepository.save(rate);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
