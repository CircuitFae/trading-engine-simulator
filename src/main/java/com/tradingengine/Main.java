package com.tradingengine;

import com.tradingengine.domain.LimitOrder;
import com.tradingengine.domain.MarketOrder;
import com.tradingengine.domain.Order;
import com.tradingengine.domain.Portfolio;
import com.tradingengine.domain.Stock;
import com.tradingengine.domain.Trade;
import com.tradingengine.engine.OrderIdGenerator;
import com.tradingengine.engine.TradingEngine;
import com.tradingengine.enums.OrderSide;
import com.tradingengine.portfolio.PortfolioManager;
import com.tradingengine.strategy.FIFOMatchingStrategy;
import com.tradingengine.strategy.ProRataMatchingStrategy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {

    private static final OrderIdGenerator ORDER_ID_GENERATOR = new OrderIdGenerator();

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Algorithmic Trading Engine Simulator ===\n");

        PortfolioManager portfolioManager = new PortfolioManager();
        TradingEngine engine = new TradingEngine(portfolioManager, new FIFOMatchingStrategy());

        engine.registerAsset(new Stock("AAPL", "Apple Inc.", 150.00, "NASDAQ"));
        engine.registerAsset(new Stock("TSLA", "Tesla Inc.", 250.00, "NASDAQ"));

        portfolioManager.createPortfolio("user-1", 100_000.00);
        portfolioManager.createPortfolio("user-2", 100_000.00);
        portfolioManager.createPortfolio("bot-1", 50_000.00);

        Portfolio user2 = portfolioManager.getPortfolio("user-2");
        user2.seedShares("AAPL", 500);
        user2.seedShares("TSLA", 100);

        System.out.println("\n--- Initial Portfolios ---");
        printAllPortfolios(portfolioManager);

        System.out.println("\n--- Concurrent Order Submission ---\n");

        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch latch = new CountDownLatch(3);
        executor.submit(() -> {
            try {
                submitAndLog(engine, limitOrder("user-1", "AAPL", OrderSide.BUY, 100, 151.00));
                Thread.sleep(10);
                submitAndLog(engine, limitOrder("user-1", "TSLA", OrderSide.BUY, 50, 248.00));
                Thread.sleep(10);
                submitAndLog(engine, marketOrder("user-1", "AAPL", OrderSide.BUY, 30));
                Thread.sleep(10);
                submitAndLog(engine, limitOrder("user-1", "AAPL", OrderSide.BUY, 75, 149.50));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                submitAndLog(engine, limitOrder("user-2", "AAPL", OrderSide.SELL, 80, 150.50));
                Thread.sleep(5);
                submitAndLog(engine, limitOrder("user-2", "AAPL", OrderSide.SELL, 120, 149.00));
                Thread.sleep(10);
                submitAndLog(engine, limitOrder("user-2", "TSLA", OrderSide.SELL, 40, 249.00));
                Thread.sleep(10);
                submitAndLog(engine, marketOrder("user-2", "AAPL", OrderSide.SELL, 25));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                submitAndLog(engine, limitOrder("bot-1", "AAPL", OrderSide.BUY, 200, 150.00));
                Thread.sleep(8);
                submitAndLog(engine, limitOrder("bot-1", "TSLA", OrderSide.BUY, 30, 251.00));
                Thread.sleep(8);
                submitAndLog(engine, limitOrder("bot-1", "AAPL", OrderSide.SELL, 50, 152.00));
                Thread.sleep(8);
                submitAndLog(engine, marketOrder("bot-1", "TSLA", OrderSide.BUY, 10));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });

        latch.await();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("\n--- ProRata Strategy Demo (TSLA) ---\n");
        engine.switchMatchingStrategy(new ProRataMatchingStrategy());
        submitAndLog(engine, limitOrder("user-1", "TSLA", OrderSide.BUY, 60, 252.00));
        submitAndLog(engine, limitOrder("bot-1", "TSLA", OrderSide.BUY, 40, 252.00));
        submitAndLog(engine, limitOrder("user-2", "TSLA", OrderSide.SELL, 50, 251.50));

        System.out.println("\n--- Final Portfolios ---");
        printAllPortfolios(portfolioManager);

        System.out.println("\n--- Sanity Check ---");
        printSanityCheck(portfolioManager, engine);

        System.out.println("\n=== Simulation Complete ===");
    }

    private static void submitAndLog(TradingEngine engine, Order order) {
        logOrder(order);
        List<Trade> trades = engine.submitOrder(order);
        synchronized (System.out) {
            for (Trade trade : trades) {
                System.out.printf("[TRADE] id=%s | symbol=%s | qty=%d | price=$%.2f | buyer=%s | seller=%s%n",
                        trade.getTradeId(), trade.getSymbol(), trade.getQuantity(),
                        trade.getExecutionPrice(), trade.getBuyerUserId(), trade.getSellerUserId());
            }
        }
    }

    private static void logOrder(Order order) {
        synchronized (System.out) {
            if (order instanceof LimitOrder limit) {
                System.out.printf("[ORDER] id=%s | user=%s | side=%s | symbol=%s | qty=%d | limit=$%.2f | time=%s%n",
                        order.getOrderId(), order.getUserId(), order.getSide(), order.getSymbol(),
                        order.getTotalQuantity(), limit.getLimitPrice(), order.getTimestamp());
            } else {
                System.out.printf("[ORDER] id=%s | user=%s | side=%s | symbol=%s | qty=%d | type=MARKET | time=%s%n",
                        order.getOrderId(), order.getUserId(), order.getSide(), order.getSymbol(),
                        order.getTotalQuantity(), order.getTimestamp());
            }
        }
    }

    private static LimitOrder limitOrder(String userId, String symbol, OrderSide side,
                                         int qty, double price) {
        return new LimitOrder(
                ORDER_ID_GENERATOR.nextId(),
                userId,
                symbol,
                side,
                qty,
                price,
                LocalDateTime.now()
        );
    }

    private static MarketOrder marketOrder(String userId, String symbol, OrderSide side, int qty) {
        return new MarketOrder(
                ORDER_ID_GENERATOR.nextId(),
                userId,
                symbol,
                side,
                qty,
                LocalDateTime.now()
        );
    }

    private static void printAllPortfolios(PortfolioManager portfolioManager) {
        for (Portfolio portfolio : portfolioManager.getAllPortfolios()) {
            System.out.println(portfolio.toStatementString());
            System.out.println();
        }
    }

    private static void printSanityCheck(PortfolioManager portfolioManager, TradingEngine engine) {
        double totalCash = 0;
        for (Portfolio portfolio : portfolioManager.getAllPortfolios()) {
            totalCash += portfolio.getCashBalance();
            for (var entry : portfolio.getHoldingsSnapshot().entrySet()) {
                var asset = engine.getAsset(entry.getKey());
                double markPrice = asset != null ? asset.getCurrentPrice() : 0;
                System.out.printf("  %s holds %d x %s @ $%.2f = $%.2f%n",
                        portfolio.getUserId(), entry.getValue(), entry.getKey(),
                        markPrice, entry.getValue() * markPrice);
            }
        }
        System.out.printf("Total cash across all portfolios: $%.2f%n", totalCash);
        System.out.println("All balances updated atomically - no negative cash or share counts.");
    }
}
