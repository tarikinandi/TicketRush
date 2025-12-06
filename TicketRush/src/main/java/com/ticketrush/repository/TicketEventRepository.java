package com.ticketrush.repository;

import com.ticketrush.domain.TicketEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketEventRepository extends JpaRepository<TicketEvent,Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("Select t from TicketEvent t Where t.id = :id")
    Optional<TicketEvent> findByIdWithLock(@Param("id") Long id);
}
