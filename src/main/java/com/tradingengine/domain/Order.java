package com.tradingengine.domain;

import com.tradingengine.enums.OrderSide;

import java.time.LocalDateTime;

public abstract class Order {

    private final String orderId;
    private final String userId;
    private final String symbol;
    private final OrderSide side;
    private final int totalQuantity;
    private int remainingQuantity;
    private final LocalDateTime timestamp;

    protected Order(String orderId, String userId, String symbol, OrderSide side,
                    int totalQuantity, LocalDateTime timestamp) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID cannot be null or blank");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be null or blank");
        }
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol cannot be null or blank");
        }
        if (side == null) {
            throw new IllegalArgumentException("Order side cannot be null");
        }
        if (totalQuantity <= 0) {
            throw new IllegalArgumentException("Total quantity must be positive");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
        this.orderId = orderId;
        this.userId = userId;
        this.symbol = symbol;
        this.side = side;
        this.totalQuantity = totalQuantity;
        this.remainingQuantity = totalQuantity;
        this.timestamp = timestamp;
    }

    public abstract boolean isMarketOrder();

    public abstract double getEffectivePrice();

    public int fill(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Fill quantity must be positive");
        }
        int filled = Math.min(quantity, remainingQuantity);
        remainingQuantity -= filled;
        return filled;
    }

    public boolean isFullyFilled() {
        return remainingQuantity == 0;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getUserId() {
        return userId;
    }

    public String getSymbol() {
        return symbol;
    }

    public OrderSide getSide() {
        return side;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public int getRemainingQuantity() {
        return remainingQuantity;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{orderId='" + orderId + "', userId='" + userId
                + "', symbol='" + symbol + "', side=" + side + ", totalQuantity=" + totalQuantity
                + ", remainingQuantity=" + remainingQuantity + ", timestamp=" + timestamp + "}";
    }
}
