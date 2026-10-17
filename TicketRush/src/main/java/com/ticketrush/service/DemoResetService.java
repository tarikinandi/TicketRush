package com.ticketrush.service;

import com.ticketrush.domain.TicketEvent;
import com.ticketrush.repository.OrderRepository;
import com.ticketrush.repository.TicketEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

/** Public demo only: periodically puts every event back to its seeded stock so it never stays sold out. */
@Service
@Profile("demo")
@RequiredArgsConstructor
public class DemoResetService {

    private final TicketEventRepository ticketEventRepository;
    private final OrderRepository orderRepository;
    private final TicketRedisService ticketRedisService;
    private final InMemoryTicketQueue queue;
    private final SimpMessagingTemplate messagingTemplate;
    private final TransactionTemplate transactionTemplate;

    @Value("${ticketrush.demo.seed-stock:10}")
    private int seedStock;

    @Scheduled(initialDelayString = "${ticketrush.demo.reset-minutes:30}",
            fixedDelayString = "${ticketrush.demo.reset-minutes:30}",
            timeUnit = TimeUnit.MINUTES)
    public void reset() {
        queue.runExclusively(() -> {
            List<Long> eventIds = transactionTemplate.execute(status -> {
                // Orders reference events, so they go first.
                orderRepository.deleteAllInBatch();
                List<TicketEvent> events = ticketEventRepository.findAll();
                events.forEach(e -> e.setStock(seedStock));
                return events.stream().map(TicketEvent::getId).toList();
            });
            // Redis is touched only after the DB commit, and only for our own keys.
            if (eventIds != null) {
                eventIds.forEach(id -> ticketRedisService.setInitialStock(id, seedStock));
            }
        });
        messagingTemplate.convertAndSend("/topic/notifications",
                "Demo sıfırlandı: stok " + seedStock + " bilete geri döndü.");
        System.out.println("DEMO RESET: stok " + seedStock + " olarak yenilendi.");
    }
}
