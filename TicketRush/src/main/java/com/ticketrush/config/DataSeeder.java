package com.ticketrush.config;

import com.ticketrush.domain.TicketEvent;
import com.ticketrush.domain.User;
import com.ticketrush.repository.TicketEventRepository;
import com.ticketrush.repository.UserRepository;
import com.ticketrush.service.TicketRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final TicketEventRepository ticketEventRepository;
    private final UserRepository userRepository;
    private final TicketRedisService ticketRedisService;

    @Value("${ticketrush.demo.seed-stock:10}")
    private int seedStock;

    @Override
    public void run(String... args) throws Exception {
        if (ticketEventRepository.count() == 0) {
            TicketEvent event = TicketEvent.builder()
                    .name("Tarkan Konseri")
                    .eventDate(LocalDateTime.now().plusDays(10))
                    .price(new BigDecimal("500.00"))
                    .stock(seedStock)
                    .build();
            ticketEventRepository.save(event);
            ticketRedisService.setInitialStock(event.getId(), seedStock);
            System.out.println(" Etkinlik Oluşturuldu.");
        } else {
            // Redis must mirror what is really left in the DB, otherwise a restart would resell tickets.
            System.out.println("--- DB Dolu, Redis Senkronize Ediliyor ---");
            ticketEventRepository.findAll().forEach(e ->
                    ticketRedisService.setInitialStock(e.getId(), e.getStock()));
        }

        if (userRepository.count() == 0) {
            List<User> users = new ArrayList<>();
            for (int i = 1; i <= 2000; i++) {
                users.add(User.builder()
                        .name("User " + i)
                        .email("user" + i + "@test.com")
                        .build());
            }
            userRepository.saveAll(users);
            System.out.println(" Kullanıcılar Oluşturuldu.");
        }

        System.out.println("--- SİSTEM HAZIR ---");
    }
}