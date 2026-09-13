package com.elegance.user.controller;

import com.elegance.user.model.StylistEntity;
import com.elegance.user.repository.StylistRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stylists")
@Tag(name = "Equipo & Estilistas", description = "Endpoints para consultar y gestionar el personal del salón")
public class StylistController {

    private final StylistRepository stylistRepository;

    @Autowired
    public StylistController(StylistRepository stylistRepository) {
        this.stylistRepository = stylistRepository;
    }

    @GetMapping
    @Operation(summary = "Obtener lista completa de estilistas")
    public ResponseEntity<List<StylistEntity>> getAllStylists() {
        return ResponseEntity.ok(stylistRepository.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener estilista por ID")
    public ResponseEntity<StylistEntity> getStylistById(@PathVariable String id) {
        return stylistRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Registrar nuevo estilista")
    public ResponseEntity<StylistEntity> createStylist(@RequestBody StylistEntity stylist) {
        if (stylist.getId() == null) {
            stylist.setId("sty-" + UUID.randomUUID().toString().substring(0, 8));
        }
        if (stylist.getIsAvailable() == null) {
            stylist.setIsAvailable(true);
        }
        if (stylist.getRating() == null) {
            stylist.setRating(0.0);
        }
        if (stylist.getReviewsCount() == null) {
            stylist.setReviewsCount(0);
        }
        if (stylist.getCompletedTodayCount() == null) {
            stylist.setCompletedTodayCount(0);
        }
        
        StylistEntity saved = stylistRepository.save(stylist);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar información de un estilista")
    public ResponseEntity<StylistEntity> updateStylist(@PathVariable String id, @RequestBody StylistEntity stylistDetails) {
        return stylistRepository.findById(id)
                .map(stylist -> {
                    stylist.setName(stylistDetails.getName());
                    stylist.setRole(stylistDetails.getRole());
                    stylist.setSpecialty(stylistDetails.getSpecialty());
                    stylist.setAvatar(stylistDetails.getAvatar());
                    stylist.setRating(stylistDetails.getRating());
                    stylist.setReviewsCount(stylistDetails.getReviewsCount());
                    stylist.setIsAvailable(stylistDetails.getIsAvailable());
                    stylist.setShift(stylistDetails.getShift());
                    stylist.setCompletedTodayCount(stylistDetails.getCompletedTodayCount());
                    
                    StylistEntity updated = stylistRepository.save(stylist);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/availability")
    @Operation(summary = "Cambiar disponibilidad de un estilista")
    public ResponseEntity<StylistEntity> toggleAvailability(@PathVariable String id) {
        return stylistRepository.findById(id)
                .map(st -> {
                    st.setIsAvailable(!Boolean.TRUE.equals(st.getIsAvailable()));
                    return ResponseEntity.ok(stylistRepository.save(st));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar estilista permanentemente")
    public ResponseEntity<Void> deleteStylist(@PathVariable String id) {
        if (stylistRepository.existsById(id)) {
            stylistRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}