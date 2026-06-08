package com.tradingengine.orderbook;

import com.tradingengine.domain.MarketOrder;
import com.tradingengine.domain.Order;
import com.tradingengine.domain.Trade;
import com.tradingengine.enums.OrderSide;
import com.tradingengine.observer.OrderBookObserver;
import com.tradingengine.strategy.MatchingStrategy;
import com.tradingengine.strategy.OrderRegistry;
import com.tradingengine.strategy.TradeIdGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class OrderBook {

    private static final Comparator<Order> BID_COMPARATOR = (a, b) -> {
        int priceCmp = Double.compare(b.getEffectivePrice(), a.getEffectivePrice());
        return priceCmp != 0 ? priceCmp : a.getTimestamp().compareTo(b.getTimestamp());
    };

    private static final Comparator<Order> ASK_COMPARATOR = Comparator
            .comparing(Order::getEffectivePrice)
            .thenComparing(Order::getTimestamp);

    private final String symbol;
    private final PriorityQueue<Order> bids;
    private final PriorityQueue<Order> asks;
    private final ReentrantLock lock;
    private final List<OrderBookObserver> observers;
    private final OrderRegistry registry;
    private final TradeIdGenerator tradeIdGenerator;
    private MatchingStrategy matchingStrategy;

    public OrderBook(String symbol, MatchingStrategy matchingStrategy,
                     OrderRegistry registry, TradeIdGenerator tradeIdGenerator) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol cannot be null or blank");
        }
        this.symbol = symbol;
        this.bids = new PriorityQueue<>(BID_COMPARATOR);
        this.asks = new PriorityQueue<>(ASK_COMPARATOR);
        this.lock = new ReentrantLock();
        this.observers = new CopyOnWriteArrayList<>();
        this.registry = registry;
        this.tradeIdGenerator = tradeIdGenerator;
        this.matchingStrategy = matchingStrategy;
    }

    public String getSymbol() {
        return symbol;
    }

    public void registerObserver(OrderBookObserver observer) {
        observers.add(observer);
    }

    public void setMatchingStrategy(MatchingStrategy matchingStrategy) {
        lock.lock();
        try {
            this.matchingStrategy = matchingStrategy;
        } finally {
            lock.unlock();
        }
    }

    public List<Trade> addOrder(Order order) {
        if (!symbol.equals(order.getSymbol())) {
            throw new IllegalArgumentException("Order symbol " + order.getSymbol()
                    + " does not match order book symbol " + symbol);
        }

        lock.lock();
        try {
            registry.register(order);
            List<Trade> trades;

            if (order.isMarketOrder()) {
                trades = executeMarketOrder((MarketOrder) order);
                if (order.getRemainingQuantity() > 0) {
                    System.out.printf("[CANCELLED] Unfilled market order remainder | order=%s | remaining=%d%n",
                            order.getOrderId(), order.getRemainingQuantity());
                    registry.unregister(order.getOrderId());
                }
            } else {
                if (order.getSide() == OrderSide.BUY) {
                    bids.offer(order);
                } else {
                    asks.offer(order);
                }
                trades = new ArrayList<>(matchingStrategy.match(bids, asks, symbol, tradeIdGenerator, registry));
            }

            notifyObservers(trades);
            return trades;
        } finally {
            lock.unlock();
        }
    }

    private List<Trade> executeMarketOrder(MarketOrder order) {
        List<Trade> trades = new ArrayList<>();

        if (order.getSide() == OrderSide.BUY) {
            while (order.getRemainingQuantity() > 0 && !asks.isEmpty()) {
                Order bestAsk = asks.peek();
                int quantity = Math.min(order.getRemainingQuantity(), bestAsk.getRemainingQuantity());
                double executionPrice = bestAsk.getEffectivePrice();

                Trade trade = new Trade(
                        tradeIdGenerator.nextId(),
                        order.getOrderId(),
                        bestAsk.getOrderId(),
                        order.getUserId(),
                        bestAsk.getUserId(),
                        symbol,
                        quantity,
                        executionPrice,
                        LocalDateTime.now()
                );
                trades.add(trade);

                order.fill(quantity);
                bestAsk.fill(quantity);

                if (bestAsk.isFullyFilled()) {
                    asks.poll();
                    registry.unregister(bestAsk.getOrderId());
                }
            }
        } else {
            while (order.getRemainingQuantity() > 0 && !bids.isEmpty()) {
                Order bestBid = bids.peek();
                int quantity = Math.min(order.getRemainingQuantity(), bestBid.getRemainingQuantity());
                double executionPrice = bestBid.getEffectivePrice();

                Trade trade = new Trade(
                        tradeIdGenerator.nextId(),
                        bestBid.getOrderId(),
                        order.getOrderId(),
                        bestBid.getUserId(),
                        order.getUserId(),
                        symbol,
                        quantity,
                        executionPrice,
                        LocalDateTime.now()
                );
                trades.add(trade);

                order.fill(quantity);
                bestBid.fill(quantity);

                if (bestBid.isFullyFilled()) {
                    bids.poll();
                    registry.unregister(bestBid.getOrderId());
                }
            }
        }

        return trades;
    }

    private void notifyObservers(List<Trade> trades) {
        for (Trade trade : trades) {
            for (OrderBookObserver observer : observers) {
                observer.onTradeExecuted(trade);
            }
        }
    }

    public int getBidDepth() {
        lock.lock();
        try {
            return bids.size();
        } finally {
            lock.unlock();
        }
    }

    public int getAskDepth() {
        lock.lock();
        try {
            return asks.size();
        } finally {
            lock.unlock();
        }
    }

    public Double getBestBidPrice() {
        lock.lock();
        try {
            return bids.isEmpty() ? null : bids.peek().getEffectivePrice();
        } finally {
            lock.unlock();
        }
    }

    public Double getBestAskPrice() {
        lock.lock();
        try {
            return asks.isEmpty() ? null : asks.peek().getEffectivePrice();
        } finally {
            lock.unlock();
        }
    }
}
