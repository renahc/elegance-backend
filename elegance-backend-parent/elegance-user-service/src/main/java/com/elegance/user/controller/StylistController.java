package com.elegance.user.controller;

import com.elegance.user.model.StylistEntity;
import com.elegance.user.repository.StylistRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}