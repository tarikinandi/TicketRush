package com.ticketrush.service;

import com.ticketrush.dto.TicketRequest;

/** Hands a ticket request to the asynchronous consumer, whatever the transport is. */
public interface TicketQueue {
    void send(TicketRequest request);
}
