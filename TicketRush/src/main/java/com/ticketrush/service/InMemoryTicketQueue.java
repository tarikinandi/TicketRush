package com.ticketrush.service;

import com.ticketrush.dto.TicketRequest;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

/** Broker-free stand-in for RabbitMQ: same producer/consumer split, inside one JVM. */
@Service
@Profile("demo")
// One worker on purpose: consumeTicket is a non-atomic read-modify-write of the stock row, like a single Rabbit consumer.
public class InMemoryTicketQueue implements TicketQueue {

    private final BlockingQueue<TicketRequest> queue = new LinkedBlockingQueue<>();
    // Held while a worker processes a ticket, so the demo reset can wait for it and then clear the queue.
    private final ReentrantLock workLock = new ReentrantLock();
    private final List<Thread> workers = new ArrayList<>();
    private final TicketConsumer consumer; // Spring proxy, so @Transactional applies
    private final int workerCount;

    public InMemoryTicketQueue(TicketConsumer consumer,
                               @Value("${ticketrush.queue.workers:1}") int workerCount) {
        this.consumer = consumer;
        this.workerCount = workerCount;
    }

    @PostConstruct
    void start() {
        for (int i = 0; i < workerCount; i++) {
            Thread t = new Thread(this::drain, "ticket-worker-" + i);
            t.setDaemon(true);
            t.start();
            workers.add(t);
        }
    }

    @PreDestroy
    void stop() {
        workers.forEach(Thread::interrupt);
    }

    @Override
    public void send(TicketRequest request) {
        queue.add(request);
    }

    private void drain() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                TicketRequest request = queue.take();
                workLock.lock();
                try {
                    consumer.consumeTicket(request);
                } finally {
                    workLock.unlock();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (RuntimeException e) {
                System.err.println("QUEUE WORKER HATASI: " + e.getMessage());
            }
        }
    }

    /** Runs the action with workers paused and the pending queue emptied. */
    public void runExclusively(Runnable action) {
        workLock.lock();
        try {
            queue.clear();
            action.run();
        } finally {
            workLock.unlock();
        }
    }
}
