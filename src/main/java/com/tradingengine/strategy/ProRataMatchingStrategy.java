package com.tradingengine.strategy;

import com.tradingengine.domain.Order;
import com.tradingengine.domain.Trade;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Pro-rata allocation at the top crossing price level.
 * When multiple orders exist at the same price, fills are distributed
 * proportionally to remaining quantity before advancing to the next level.
 */
public class ProRataMatchingStrategy implements MatchingStrategy {

    @Override
    public List<Trade> match(PriorityQueue<Order> bids,
                             PriorityQueue<Order> asks,
                             String symbol,
                             TradeIdGenerator idGenerator,
                             OrderRegistry registry) {
        List<Trade> trades = new ArrayList<>();

        while (!bids.isEmpty() && !asks.isEmpty()) {
            Order topBid = bids.peek();
            Order topAsk = asks.peek();

            if (topBid.getEffectivePrice() < topAsk.getEffectivePrice()) {
                break;
            }

            double bidPrice = topBid.getEffectivePrice();
            double askPrice = topAsk.getEffectivePrice();

            List<Order> bidLevel = extractAtPrice(bids, bidPrice);
            List<Order> askLevel = extractAtPrice(asks, askPrice);

            int totalBidQty = bidLevel.stream().mapToInt(Order::getRemainingQuantity).sum();
            int totalAskQty = askLevel.stream().mapToInt(Order::getRemainingQuantity).sum();
            int matchableQty = Math.min(totalBidQty, totalAskQty);

            if (matchableQty == 0) {
                reinsertAll(bids, bidLevel);
                reinsertAll(asks, askLevel);
                break;
            }

            int bidIndex = 0;
            int askIndex = 0;
            int remaining = matchableQty;

            while (remaining > 0 && bidIndex < bidLevel.size() && askIndex < askLevel.size()) {
                Order bid = bidLevel.get(bidIndex);
                Order ask = askLevel.get(askIndex);

                int bidShare = proportionalShare(bid.getRemainingQuantity(), totalBidQty, remaining);
                int askShare = proportionalShare(ask.getRemainingQuantity(), totalAskQty, remaining);
                int quantity = Math.min(bidShare, askShare);
                quantity = Math.min(quantity, Math.min(bid.getRemainingQuantity(), ask.getRemainingQuantity()));

                if (quantity <= 0) {
                    if (bid.getRemainingQuantity() == 0) {
                        bidIndex++;
                    }
                    if (ask.getRemainingQuantity() == 0) {
                        askIndex++;
                    }
                    if (quantity == 0) {
                        break;
                    }
                    continue;
                }

                double executionPrice = bid.getTimestamp().isBefore(ask.getTimestamp())
                        ? bid.getEffectivePrice()
                        : ask.getEffectivePrice();

                Trade trade = new Trade(
                        idGenerator.nextId(),
                        bid.getOrderId(),
                        ask.getOrderId(),
                        bid.getUserId(),
                        ask.getUserId(),
                        symbol,
                        quantity,
                        executionPrice,
                        LocalDateTime.now()
                );
                trades.add(trade);

                bid.fill(quantity);
                ask.fill(quantity);
                remaining -= quantity;

                if (bid.isFullyFilled()) {
                    bidIndex++;
                }
                if (ask.isFullyFilled()) {
                    askIndex++;
                }
            }

            finalizeLevel(bids, bidLevel, registry);
            finalizeLevel(asks, askLevel, registry);
        }

        return trades;
    }

    private int proportionalShare(int orderQty, int totalQty, int remainingMatch) {
        if (totalQty == 0) {
            return 0;
        }
        double proportion = (double) orderQty / totalQty;
        return Math.max(1, (int) Math.ceil(proportion * remainingMatch * 0.5));
    }

    private List<Order> extractAtPrice(PriorityQueue<Order> queue, double price) {
        List<Order> level = new ArrayList<>();
        while (!queue.isEmpty() && queue.peek().getEffectivePrice() == price) {
            level.add(queue.poll());
        }
        return level;
    }

    private void reinsertAll(PriorityQueue<Order> queue, List<Order> orders) {
        for (Order order : orders) {
            queue.offer(order);
        }
    }

    private void finalizeLevel(PriorityQueue<Order> queue, List<Order> orders, OrderRegistry registry) {
        for (Order order : orders) {
            if (order.isFullyFilled()) {
                registry.unregister(order.getOrderId());
            } else {
                queue.offer(order);
            }
        }
    }
}
