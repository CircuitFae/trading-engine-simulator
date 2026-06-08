package com.tradingengine.engine;

import java.util.concurrent.atomic.AtomicLong;

public class OrderIdGenerator {

    private final AtomicLong counter;

    public OrderIdGenerator() {
        this.counter = new AtomicLong(0);
    }

    public String nextId() {
        return "O-" + counter.incrementAndGet();
    }
}
