package com.ticketrush.service;

import com.ticketrush.repository.VisitorStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Public demo janitor: visitor stocks and their orders are dropped once idle for the TTL.
 * Redis visitor keys expire on their own (EXPIRE), so Redis is never scanned or flushed.
 */
@Service
@Profile("demo")
@RequiredArgsConstructor
public class DemoResetService {

    private final VisitorStockRepository visitorStockRepository;
    private final TransactionTemplate transactionTemplate;

    @Value("${ticketrush.demo.visitor-ttl-hours:24}")
    private long ttlHours;

    @Scheduled(initialDelayString = "${ticketrush.demo.reset-minutes:30}",
            fixedDelayString = "${ticketrush.demo.reset-minutes:30}",
            timeUnit = TimeUnit.MINUTES)
    public void cleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(ttlHours);
        int[] removed = transactionTemplate.execute(status -> new int[]{
                // Orders first: they are matched through the stale visitor rows.
                visitorStockRepository.deleteOrdersOfStaleVisitors(cutoff),
                visitorStockRepository.deleteStale(cutoff)});
        if (removed != null && (removed[0] > 0 || removed[1] > 0)) {
            System.out.println("DEMO CLEANUP: " + removed[0] + " siparis, " + removed[1] + " ziyaretci stoku silindi.");
        }
    }
}
