package com.elegance.salon.repository;

import com.elegance.salon.model.StylistEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StylistRepository extends JpaRepository<StylistEntity, String> {
}
