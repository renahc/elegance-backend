package com.elegance.appointment.repository;

import com.elegance.appointment.model.AppointmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<AppointmentEntity, String> {
    List<AppointmentEntity> findByDate(String date);
    List<AppointmentEntity> findByStatus(String status);
    List<AppointmentEntity> findByStylistId(String stylistId);
}