package com.ticketrush.service;

import com.ticketrush.config.RabbitMQConfig;
import com.ticketrush.dto.TicketRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendTicketRequest(TicketRequest ticketRequest){
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY,
                ticketRequest
        );
        System.out.println("Message sent to queue : User " + ticketRequest.getUserId());
    }
}
