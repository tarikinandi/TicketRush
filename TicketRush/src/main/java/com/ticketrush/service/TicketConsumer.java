package com.ticketrush.service;

import com.ticketrush.config.RabbitMQConfig;
import com.ticketrush.domain.Order;
import com.ticketrush.domain.TicketEvent;
import com.ticketrush.domain.User;
import com.ticketrush.dto.TicketRequest;
import com.ticketrush.repository.OrderRepository;
import com.ticketrush.repository.TicketEventRepository;
import com.ticketrush.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketConsumer {

    private final TicketEventRepository ticketEventRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private final TicketRedisService ticketRedisService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    @Transactional
    public void consumeTicket(TicketRequest ticketRequest) {

        try {
            Long eventId = ticketRequest.getEventId();

            TicketEvent event = ticketEventRepository.findById(eventId)
                    .orElseThrow(() -> new RuntimeException("Event not found"));

            User user = userRepository.findById(ticketRequest.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (event.getStock() > 0) {
                event.setStock(event.getStock() - 1);
                ticketEventRepository.save(event);

                Order order = Order.builder()
                        .user(user)
                        .event(event)
                        .paidAmount(event.getPrice())
                        .build();

                orderRepository.save(order);

                String message = "Kullanıcı " + ticketRequest.getUserId() + " bilet aldı. (Kalan Stok: " + event.getStock() + ")";
                System.out.println("Consumer: WebSocket bildirimi gönderildi -> " + message);

                messagingTemplate.convertAndSend("/topic/notifications", message);
            }

        } catch (Exception e) {
            System.err.println("CONSUMER HATASI: " + e.getMessage());
            e.printStackTrace();
        }
    }
}