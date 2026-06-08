package com.tradingengine.engine;

import com.tradingengine.domain.Asset;
import com.tradingengine.domain.LimitOrder;
import com.tradingengine.domain.Order;
import com.tradingengine.domain.Trade;
import com.tradingengine.enums.OrderSide;
import com.tradingengine.orderbook.OrderBook;
import com.tradingengine.portfolio.PortfolioManager;
import com.tradingengine.strategy.FIFOMatchingStrategy;
import com.tradingengine.strategy.MatchingStrategy;
import com.tradingengine.strategy.OrderRegistry;
import com.tradingengine.strategy.TradeIdGenerator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class TradingEngine {

    private final ConcurrentHashMap<String, OrderBook> orderBooks;
    private final ConcurrentHashMap<String, Asset> assets;
    private final PortfolioManager portfolioManager;
    private final OrderRegistry orderRegistry;
    private final TradeIdGenerator tradeIdGenerator;
    private MatchingStrategy matchingStrategy;

    public TradingEngine(PortfolioManager portfolioManager) {
        this(portfolioManager, new FIFOMatchingStrategy());
    }

    public TradingEngine(PortfolioManager portfolioManager, MatchingStrategy matchingStrategy) {
        this.orderBooks = new ConcurrentHashMap<>();
        this.assets = new ConcurrentHashMap<>();
        this.portfolioManager = portfolioManager;
        this.orderRegistry = new OrderRegistry();
        this.tradeIdGenerator = new TradeIdGenerator();
        this.matchingStrategy = matchingStrategy;
    }

    public PortfolioManager getPortfolioManager() {
        return portfolioManager;
    }

    public void registerAsset(Asset asset) {
        assets.put(asset.getSymbol(), asset);
        OrderBook orderBook = new OrderBook(
                asset.getSymbol(),
                matchingStrategy,
                orderRegistry,
                tradeIdGenerator
        );
        orderBook.registerObserver(portfolioManager);
        orderBooks.put(asset.getSymbol(), orderBook);
        System.out.printf("[ENGINE] Registered asset %s (%s) @ $%.2f%n",
                asset.getSymbol(), asset.getCompanyName(), asset.getCurrentPrice());
    }

    public List<Trade> submitOrder(Order order) {
        OrderBook orderBook = orderBooks.get(order.getSymbol());
        if (orderBook == null) {
            throw new IllegalArgumentException("No order book for symbol: " + order.getSymbol());
        }

        double estimatedPrice = estimateExecutionPrice(order, orderBook);
        if (!portfolioManager.validateOrder(order, estimatedPrice)) {
            return List.of();
        }

        List<Trade> trades = orderBook.addOrder(order);

        for (Trade trade : trades) {
            Asset asset = assets.get(trade.getSymbol());
            if (asset != null) {
                asset.updatePrice(trade.getExecutionPrice());
            }
        }

        return trades;
    }

    private double estimateExecutionPrice(Order order, OrderBook orderBook) {
        if (order instanceof LimitOrder limitOrder) {
            return limitOrder.getLimitPrice();
        }

        if (order.getSide() == OrderSide.BUY) {
            Double bestAsk = orderBook.getBestAskPrice();
            if (bestAsk != null) {
                return bestAsk;
            }
        } else {
            Double bestBid = orderBook.getBestBidPrice();
            if (bestBid != null) {
                return bestBid;
            }
        }

        Asset asset = assets.get(order.getSymbol());
        if (asset != null) {
            return asset.getCurrentPrice();
        }
        throw new IllegalStateException("Cannot estimate price for market order on " + order.getSymbol());
    }

    public void switchMatchingStrategy(MatchingStrategy strategy) {
        this.matchingStrategy = strategy;
        for (OrderBook orderBook : orderBooks.values()) {
            orderBook.setMatchingStrategy(strategy);
        }
        System.out.println("[ENGINE] Matching strategy switched to " + strategy.getClass().getSimpleName());
    }

    public Asset getAsset(String symbol) {
        return assets.get(symbol);
    }

    public Collection<Asset> getAllAssets() {
        return assets.values();
    }

    public OrderBook getOrderBook(String symbol) {
        return orderBooks.get(symbol);
    }
}
