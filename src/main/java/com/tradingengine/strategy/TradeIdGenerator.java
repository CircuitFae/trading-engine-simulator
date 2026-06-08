package com.tradingengine.strategy;

import java.util.concurrent.atomic.AtomicLong;

public class TradeIdGenerator {

    private final AtomicLong counter;

    public TradeIdGenerator() {
        this.counter = new AtomicLong(0);
    }

    public String nextId() {
        return "T-" + counter.incrementAndGet();
    }
}
