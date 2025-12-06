package com.ticketrush.controller;

import com.ticketrush.dto.TicketRequest;
import com.ticketrush.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping("/buy")
    public ResponseEntity<String> buyTicket(@RequestBody TicketRequest request) {
        ticketService.buyTicket(request.getUserId(),request.getEventId());
        return ResponseEntity.ok("Ticket has been built");
    }
}
