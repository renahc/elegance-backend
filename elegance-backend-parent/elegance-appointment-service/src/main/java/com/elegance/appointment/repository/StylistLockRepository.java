package com.elegance.appointment.repository;

import com.elegance.appointment.model.StylistLock;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface StylistLockRepository extends JpaRepository<StylistLock, String> {
  @Modifying
  @Query(value = "INSERT IGNORE INTO stylist_lock (id) VALUES (:id)", nativeQuery = true)
  void ensure(@Param("id") String id);

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from StylistLock s where s.id=:id")
  StylistLock lock(@Param("id") String id);
}
