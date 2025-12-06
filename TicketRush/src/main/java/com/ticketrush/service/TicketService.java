package com.ticketrush.service;

import com.ticketrush.dto.TicketRequest;
import com.ticketrush.exception.TicketSoldOutException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRedisService ticketRedisService;
    private final TicketProducer ticketProducer;

    public void buyTicket(Long userId, Long eventId){

        boolean success = ticketRedisService.tryDecreaseStock(eventId);
        if (!success) {
            throw new TicketSoldOutException("Üzgünüz, bu etkinlik için tüm biletler tükendi! Stok: 0");
        }

        ticketProducer.sendTicketRequest(new TicketRequest(userId, eventId));

        System.out.println("User " + userId + " request send to queue.");
    }
}