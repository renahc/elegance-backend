package com.elegance.appointment.repository;

import com.elegance.appointment.model.OutboxMessage;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.*;

public interface OutboxRepository extends JpaRepository<OutboxMessage, Long> {
  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select m from OutboxMessage m where m.delivered=false and m.nextAttempt<=:now and not exists"
          + " (select older from OutboxMessage older where older.messageKey=m.messageKey and"
          + " older.destination=m.destination and older.delivered=false and older.id<m.id) order by"
          + " m.id")
  List<OutboxMessage> pending(Instant now, org.springframework.data.domain.Pageable page);
}
