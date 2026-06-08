package com.tradingengine.domain;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class Portfolio {

    private final String userId;
    private double cashBalance;
    private final ConcurrentHashMap<String, Integer> assetQuantities;
    private final ReentrantLock lock;

    public Portfolio(String userId, double initialCash) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be null or blank");
        }
        if (initialCash < 0) {
            throw new IllegalArgumentException("Initial cash cannot be negative");
        }
        this.userId = userId;
        this.cashBalance = initialCash;
        this.assetQuantities = new ConcurrentHashMap<>();
        this.lock = new ReentrantLock();
    }

    public String getUserId() {
        return userId;
    }

    public double getCashBalance() {
        lock.lock();
        try {
            return cashBalance;
        } finally {
            lock.unlock();
        }
    }

    public int getQuantity(String symbol) {
        return assetQuantities.getOrDefault(symbol, 0);
    }

    public boolean canAfford(double cost) {
        lock.lock();
        try {
            return cashBalance >= cost;
        } finally {
            lock.unlock();
        }
    }

    public boolean hasShares(String symbol, int quantity) {
        return getQuantity(symbol) >= quantity;
    }

    public void creditCash(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Credit amount cannot be negative");
        }
        lock.lock();
        try {
            cashBalance += amount;
        } finally {
            lock.unlock();
        }
    }

    public void debitCash(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Debit amount cannot be negative");
        }
        lock.lock();
        try {
            if (cashBalance < amount) {
                throw new IllegalStateException("Insufficient cash for user " + userId);
            }
            cashBalance -= amount;
        } finally {
            lock.unlock();
        }
    }

    public void addShares(String symbol, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        lock.lock();
        try {
            assetQuantities.merge(symbol, quantity, Integer::sum);
        } finally {
            lock.unlock();
        }
    }

    public void removeShares(String symbol, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        lock.lock();
        try {
            int current = assetQuantities.getOrDefault(symbol, 0);
            if (current < quantity) {
                throw new IllegalStateException("Insufficient shares of " + symbol + " for user " + userId);
            }
            int remaining = current - quantity;
            if (remaining == 0) {
                assetQuantities.remove(symbol);
            } else {
                assetQuantities.put(symbol, remaining);
            }
        } finally {
            lock.unlock();
        }
    }

    public void seedShares(String symbol, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        lock.lock();
        try {
            assetQuantities.put(symbol, quantity);
        } finally {
            lock.unlock();
        }
    }

    public Map<String, Integer> getHoldingsSnapshot() {
        lock.lock();
        try {
            return Collections.unmodifiableMap(new HashMap<>(assetQuantities));
        } finally {
            lock.unlock();
        }
    }

    public String toStatementString() {
        lock.lock();
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("[PORTFOLIO] user=%s | cash=$%.2f%n", userId, cashBalance));
            if (assetQuantities.isEmpty()) {
                sb.append("  holdings: (none)");
            } else {
                sb.append("  holdings:");
                assetQuantities.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(e -> sb.append(String.format("%n    %s: %d shares", e.getKey(), e.getValue())));
            }
            return sb.toString();
        } finally {
            lock.unlock();
        }
    }
}
