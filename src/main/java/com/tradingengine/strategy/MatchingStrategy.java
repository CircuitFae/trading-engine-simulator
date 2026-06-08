package com.tradingengine.strategy;

import com.tradingengine.domain.Order;
import com.tradingengine.domain.Trade;

import java.util.List;
import java.util.PriorityQueue;

public interface MatchingStrategy {

    List<Trade> match(PriorityQueue<Order> bids,
                        PriorityQueue<Order> asks,
                        String symbol,
                        TradeIdGenerator idGenerator,
                        OrderRegistry registry);
}
