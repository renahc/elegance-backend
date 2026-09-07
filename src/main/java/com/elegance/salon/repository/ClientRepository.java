package com.elegance.salon.repository;

import com.elegance.salon.model.ClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientRepository extends JpaRepository<ClientEntity, String> {
    List<ClientEntity> findByNameContainingIgnoreCase(String name);
}
