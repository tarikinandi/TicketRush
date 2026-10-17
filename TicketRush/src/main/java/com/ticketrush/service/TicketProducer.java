package com.ticketrush.service;

import com.ticketrush.config.RabbitMQConfig;
import com.ticketrush.dto.TicketRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!demo")
@RequiredArgsConstructor
public class TicketProducer implements TicketQueue {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void send(TicketRequest ticketRequest){
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY,
                ticketRequest
        );
        System.out.println("Message sent to queue : User " + ticketRequest.getUserId());
    }
}
