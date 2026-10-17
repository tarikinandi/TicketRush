package com.ticketrush.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/** Flipped by DataSeeder once the seed data exists; until then health reports 503 and buying is refused. */
@Component
public class ReadinessState {
    private final AtomicBoolean ready = new AtomicBoolean(false);

    public boolean isReady() { return ready.get(); }

    public void markReady() { ready.set(true); }
}
