package com.ticketrush.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Demo only: one visitor's own remaining stock for an event. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "visitor_stocks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"visitorId", "eventId"}))
public class VisitorStock extends BaseEntity {

    private String visitorId;

    private Long eventId;

    private int stock;
}
