package com.elegance.service.repository;

import com.elegance.service.model.RateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RateRepository extends JpaRepository<RateEntity, String> {
    List<RateEntity> findByServiceId(String serviceId);
    List<RateEntity> findByServiceIdAndActiveTrue(String serviceId);
}
