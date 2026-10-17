package com.ticketrush.service;

import com.ticketrush.domain.Order;
import com.ticketrush.domain.TicketEvent;
import com.ticketrush.domain.User;
import com.ticketrush.domain.VisitorStock;
import com.ticketrush.dto.TicketRequest;
import com.ticketrush.repository.OrderRepository;
import com.ticketrush.repository.TicketEventRepository;
import com.ticketrush.repository.UserRepository;
import com.ticketrush.repository.VisitorStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketConsumer {

    private final TicketEventRepository ticketEventRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final VisitorStockRepository visitorStockRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private final TicketRedisService ticketRedisService;

    @Value("${ticketrush.demo.seed-stock:10}")
    private int seedStock;

    @Transactional
    public void consumeTicket(TicketRequest ticketRequest) {

        try {
            Long eventId = ticketRequest.getEventId();
            String visitorId = ticketRequest.getVisitorId();

            TicketEvent event = ticketEventRepository.findById(eventId)
                    .orElseThrow(() -> new RuntimeException("Event not found"));

            User user = userRepository.findById(ticketRequest.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Demo visitors decrement their own counter; everyone else shares the event row.
            VisitorStock visitorStock = null;
            int remaining;
            if (visitorId == null) {
                remaining = event.getStock();
            } else {
                visitorStock = visitorStockRepository.findByVisitorIdAndEventId(visitorId, eventId)
                        .orElseGet(() -> visitorStockRepository.save(VisitorStock.builder()
                                .visitorId(visitorId).eventId(eventId).stock(seedStock).build()));
                remaining = visitorStock.getStock();
            }

            if (remaining > 0) {
                if (visitorStock == null) {
                    event.setStock(remaining - 1);
                    ticketEventRepository.save(event);
                } else {
                    visitorStock.setStock(remaining - 1);
                    visitorStockRepository.save(visitorStock);
                }

                Order order = Order.builder()
                        .user(user)
                        .event(event)
                        .paidAmount(event.getPrice())
                        .visitorId(visitorId)
                        .build();

                orderRepository.save(order);

                String message = "Kullanıcı " + ticketRequest.getUserId() + " bilet aldı. (Kalan Stok: " + (remaining - 1) + ")";
                System.out.println("Consumer: WebSocket bildirimi gönderildi -> " + message);

                String topic = visitorId == null ? "/topic/notifications" : "/topic/notifications/" + visitorId;
                messagingTemplate.convertAndSend(topic, message);
            }

        } catch (Exception e) {
            System.err.println("CONSUMER HATASI: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
