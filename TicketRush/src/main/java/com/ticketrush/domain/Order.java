package com.ticketrush.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orders", indexes = @jakarta.persistence.Index(columnList = "visitorId"))
@Builder
public class Order extends BaseEntity{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private TicketEvent event;

    private BigDecimal paidAmount;

    // Set only in the public demo, where every visitor has a private stock.
    private String visitorId;
}
