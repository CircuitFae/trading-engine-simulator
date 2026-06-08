package com.tradingengine.strategy;

import com.tradingengine.domain.Order;
import com.tradingengine.domain.Trade;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class FIFOMatchingStrategy implements MatchingStrategy {

    @Override
    public List<Trade> match(PriorityQueue<Order> bids,
                             PriorityQueue<Order> asks,
                             String symbol,
                             TradeIdGenerator idGenerator,
                             OrderRegistry registry) {
        List<Trade> trades = new ArrayList<>();

        while (!bids.isEmpty() && !asks.isEmpty()) {
            Order bestBid = bids.peek();
            Order bestAsk = asks.peek();

            if (bestBid.getEffectivePrice() < bestAsk.getEffectivePrice()) {
                break;
            }

            int quantity = Math.min(bestBid.getRemainingQuantity(), bestAsk.getRemainingQuantity());
            double executionPrice = determineExecutionPrice(bestBid, bestAsk);

            Trade trade = new Trade(
                    idGenerator.nextId(),
                    bestBid.getOrderId(),
                    bestAsk.getOrderId(),
                    bestBid.getUserId(),
                    bestAsk.getUserId(),
                    symbol,
                    quantity,
                    executionPrice,
                    LocalDateTime.now()
            );
            trades.add(trade);

            bestBid.fill(quantity);
            bestAsk.fill(quantity);

            if (bestBid.isFullyFilled()) {
                bids.poll();
                registry.unregister(bestBid.getOrderId());
            }
            if (bestAsk.isFullyFilled()) {
                asks.poll();
                registry.unregister(bestAsk.getOrderId());
            }
        }

        return trades;
    }

    private double determineExecutionPrice(Order bid, Order ask) {
        if (bid.getTimestamp().isBefore(ask.getTimestamp()) || bid.getTimestamp().equals(ask.getTimestamp())) {
            return bid.getEffectivePrice();
        }
        return ask.getEffectivePrice();
    }
}
