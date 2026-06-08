package com.tradingengine.domain;

import com.tradingengine.enums.OrderSide;

import java.time.LocalDateTime;

public class LimitOrder extends Order {

    private final double limitPrice;

    public LimitOrder(String orderId, String userId, String symbol, OrderSide side,
                      int totalQuantity, double limitPrice, LocalDateTime timestamp) {
        super(orderId, userId, symbol, side, totalQuantity, timestamp);
        if (limitPrice <= 0) {
            throw new IllegalArgumentException("Limit price must be positive");
        }
        this.limitPrice = limitPrice;
    }

    public double getLimitPrice() {
        return limitPrice;
    }

    @Override
    public boolean isMarketOrder() {
        return false;
    }

    @Override
    public double getEffectivePrice() {
        return limitPrice;
    }

    @Override
    public String toString() {
        return "LimitOrder{orderId='" + getOrderId() + "', userId='" + getUserId()
                + "', symbol='" + getSymbol() + "', side=" + getSide()
                + ", quantity=" + getTotalQuantity() + ", limitPrice=" + limitPrice
                + ", remaining=" + getRemainingQuantity() + "}";
    }
}
