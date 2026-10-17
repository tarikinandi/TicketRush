package com.ticketrush.repository;

import com.ticketrush.domain.VisitorStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface VisitorStockRepository extends JpaRepository<VisitorStock, Long> {

    Optional<VisitorStock> findByVisitorIdAndEventId(String visitorId, Long eventId);

    @Modifying
    @Query("delete from Order o where o.visitorId in (select v.visitorId from VisitorStock v where v.updatedAt < :cutoff)")
    int deleteOrdersOfStaleVisitors(@Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Query("delete from VisitorStock v where v.updatedAt < :cutoff")
    int deleteStale(@Param("cutoff") LocalDateTime cutoff);
}
