package com.ticketrush.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ticket_events")
@Builder
public class TicketEvent extends BaseEntity{

    private String name;

    private LocalDateTime eventDate;

    private int stock;

    private BigDecimal price;
}
