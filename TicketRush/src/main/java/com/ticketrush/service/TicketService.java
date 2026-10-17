package com.ticketrush.service;

import com.ticketrush.dto.TicketRequest;
import com.ticketrush.exception.TicketSoldOutException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRedisService ticketRedisService;
    private final TicketQueue ticketQueue;

    /** visitorId is null outside the demo, which keeps the original shared-stock behaviour. */
    public void buyTicket(Long userId, Long eventId, String visitorId){

        boolean success = visitorId == null
                ? ticketRedisService.tryDecreaseStock(eventId)
                : ticketRedisService.tryDecreaseVisitorStock(eventId, visitorId);
        if (!success) {
            throw new TicketSoldOutException("Üzgünüz, bu etkinlik için tüm biletler tükendi! Stok: 0");
        }

        ticketQueue.send(new TicketRequest(userId, eventId, visitorId));

        System.out.println("User " + userId + " request send to queue.");
    }
}
