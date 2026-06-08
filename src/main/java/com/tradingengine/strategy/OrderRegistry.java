package com.tradingengine.strategy;

import com.tradingengine.domain.Order;

import java.util.concurrent.ConcurrentHashMap;

public class OrderRegistry {

    private final ConcurrentHashMap<String, Order> orders;

    public OrderRegistry() {
        this.orders = new ConcurrentHashMap<>();
    }

    public void register(Order order) {
        orders.put(order.getOrderId(), order);
    }

    public Order get(String orderId) {
        return orders.get(orderId);
    }

    public void unregister(String orderId) {
        orders.remove(orderId);
    }
}
