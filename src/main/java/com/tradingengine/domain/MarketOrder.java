package com.tradingengine.domain;

import com.tradingengine.enums.OrderSide;

import java.time.LocalDateTime;

public class MarketOrder extends Order {

    public MarketOrder(String orderId, String userId, String symbol, OrderSide side,
                       int totalQuantity, LocalDateTime timestamp) {
        super(orderId, userId, symbol, side, totalQuantity, timestamp);
    }

    @Override
    public boolean isMarketOrder() {
        return true;
    }

    /**
     * Sentinel price used only for comparator tie-breaking.
     * Market orders never rest on the order book.
     */
    @Override
    public double getEffectivePrice() {
        return getSide() == OrderSide.BUY ? Double.MAX_VALUE : 0.0;
    }

    @Override
    public String toString() {
        return "MarketOrder{orderId='" + getOrderId() + "', userId='" + getUserId()
                + "', symbol='" + getSymbol() + "', side=" + getSide()
                + ", quantity=" + getTotalQuantity()
                + ", remaining=" + getRemainingQuantity() + "}";
    }
}
