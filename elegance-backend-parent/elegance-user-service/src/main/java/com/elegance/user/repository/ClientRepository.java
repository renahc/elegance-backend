package com.elegance.user.repository;

import com.elegance.user.model.ClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ClientRepository extends JpaRepository<ClientEntity, String> {
    List<ClientEntity> findByNameContainingIgnoreCase(String name);
}