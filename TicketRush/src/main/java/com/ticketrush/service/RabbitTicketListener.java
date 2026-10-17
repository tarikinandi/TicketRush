package com.ticketrush.service;

import com.ticketrush.config.RabbitMQConfig;
import com.ticketrush.dto.TicketRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!demo")
@RequiredArgsConstructor
public class RabbitTicketListener {

    private final TicketConsumer consumer;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void onMessage(TicketRequest request) {
        consumer.consumeTicket(request);
    }
}
