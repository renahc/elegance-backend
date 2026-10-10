package com.elegance.service.repository;

import com.elegance.service.model.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<ServiceEntity, String> {
    List<ServiceEntity> findByCategory(String category);
    List<ServiceEntity> findByActiveTrue();
}
