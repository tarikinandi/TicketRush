package com.ticketrush.controller;

import com.ticketrush.dto.TicketRequest;
import com.ticketrush.repository.TicketEventRepository;
import com.ticketrush.service.ReadinessState;
import com.ticketrush.service.TicketRedisService;
import com.ticketrush.service.TicketService;
import com.ticketrush.service.VisitorIds;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final TicketRedisService ticketRedisService;
    private final TicketEventRepository ticketEventRepository;
    private final ReadinessState readiness;

    // In the public demo every visitor has a private stock, identified by X-Visitor-Id.
    @Value("${ticketrush.demo.enabled:false}")
    private boolean demo;

    @PostMapping("/buy")
    public ResponseEntity<String> buyTicket(@RequestBody TicketRequest request,
                                            @RequestHeader(value = "X-Visitor-Id", required = false) String visitorId) {
        requireReady();
        ticketService.buyTicket(request.getUserId(), request.getEventId(), visitorFor(visitorId));
        return ResponseEntity.ok("Ticket has been built");
    }

    @GetMapping("/events/{eventId}/stock")
    public Map<String, Integer> stock(@PathVariable Long eventId,
                                      @RequestHeader(value = "X-Visitor-Id", required = false) String visitorId) {
        requireReady();
        String visitor = visitorFor(visitorId);
        if (visitor != null) {
            return Map.of("stock", ticketRedisService.getVisitorStock(eventId, visitor));
        }
        int stock = ticketEventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"))
                .getStock();
        return Map.of("stock", stock);
    }

    private void requireReady() {
        if (!readiness.isReady()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Sunucu hazırlanıyor, birazdan tekrar deneyin.");
        }
    }

    private String visitorFor(String header) {
        if (!demo) {
            return null;
        }
        if (!VisitorIds.isValid(header)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Geçersiz veya eksik X-Visitor-Id.");
        }
        return header;
    }
}
