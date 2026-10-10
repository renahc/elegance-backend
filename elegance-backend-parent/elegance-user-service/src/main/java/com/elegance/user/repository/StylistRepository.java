package com.elegance.user.repository;

import com.elegance.user.model.StylistEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StylistRepository extends JpaRepository<StylistEntity, String> {
}