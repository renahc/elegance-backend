package com.elegance.notification.repository;

import com.elegance.notification.model.NotificationEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationEntity, String> {
  List<NotificationEntity> findTop100ByOrderByTimestampDesc();
}
