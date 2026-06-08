package com.tradingengine.domain;

import java.time.LocalDateTime;

public class Trade {

    private final String tradeId;
    private final String buyOrderId;
    private final String sellOrderId;
    private final String buyerUserId;
    private final String sellerUserId;
    private final String symbol;
    private final int quantity;
    private final double executionPrice;
    private final LocalDateTime timestamp;

    public Trade(String tradeId, String buyOrderId, String sellOrderId,
                 String buyerUserId, String sellerUserId, String symbol,
                 int quantity, double executionPrice, LocalDateTime timestamp) {
        if (tradeId == null || tradeId.isBlank()) {
            throw new IllegalArgumentException("Trade ID cannot be null or blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (executionPrice <= 0) {
            throw new IllegalArgumentException("Execution price must be positive");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
        this.tradeId = tradeId;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.buyerUserId = buyerUserId;
        this.sellerUserId = sellerUserId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.executionPrice = executionPrice;
        this.timestamp = timestamp;
    }

    public String getTradeId() {
        return tradeId;
    }

    public String getBuyOrderId() {
        return buyOrderId;
    }

    public String getSellOrderId() {
        return sellOrderId;
    }

    public String getBuyerUserId() {
        return buyerUserId;
    }

    public String getSellerUserId() {
        return sellerUserId;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getExecutionPrice() {
        return executionPrice;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Trade{tradeId='" + tradeId + "', symbol='" + symbol + "', quantity=" + quantity
                + ", executionPrice=" + executionPrice + ", buyer='" + buyerUserId
                + "', seller='" + sellerUserId + "', timestamp=" + timestamp + "}";
    }
}
