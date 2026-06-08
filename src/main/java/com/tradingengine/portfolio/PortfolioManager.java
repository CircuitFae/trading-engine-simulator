package com.tradingengine.portfolio;

import com.tradingengine.domain.Order;
import com.tradingengine.domain.Portfolio;
import com.tradingengine.domain.Trade;
import com.tradingengine.enums.OrderSide;
import com.tradingengine.observer.OrderBookObserver;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class PortfolioManager implements OrderBookObserver {

    private final ConcurrentHashMap<String, Portfolio> portfolios;

    public PortfolioManager() {
        this.portfolios = new ConcurrentHashMap<>();
    }

    public Portfolio createPortfolio(String userId, double initialCash) {
        Portfolio portfolio = new Portfolio(userId, initialCash);
        portfolios.put(userId, portfolio);
        return portfolio;
    }

    public Portfolio getPortfolio(String userId) {
        Portfolio portfolio = portfolios.get(userId);
        if (portfolio == null) {
            throw new IllegalArgumentException("Portfolio not found for user: " + userId);
        }
        return portfolio;
    }

    public Collection<Portfolio> getAllPortfolios() {
        return portfolios.values();
    }

    public boolean validateOrder(Order order, double estimatedPrice) {
        Portfolio portfolio = portfolios.get(order.getUserId());
        if (portfolio == null) {
            System.out.printf("[REJECTED] No portfolio for user %s | order=%s%n",
                    order.getUserId(), order.getOrderId());
            return false;
        }

        if (order.getSide() == OrderSide.BUY) {
            double cost = order.getTotalQuantity() * estimatedPrice;
            if (!portfolio.canAfford(cost)) {
                System.out.printf("[REJECTED] Insufficient cash for user %s | need=$%.2f | order=%s%n",
                        order.getUserId(), cost, order.getOrderId());
                return false;
            }
        } else {
            if (!portfolio.hasShares(order.getSymbol(), order.getTotalQuantity())) {
                System.out.printf("[REJECTED] Insufficient shares for user %s | symbol=%s | need=%d | order=%s%n",
                        order.getUserId(), order.getSymbol(), order.getTotalQuantity(), order.getOrderId());
                return false;
            }
        }
        return true;
    }

    @Override
    public void onTradeExecuted(Trade trade) {
        Portfolio buyer = portfolios.get(trade.getBuyerUserId());
        Portfolio seller = portfolios.get(trade.getSellerUserId());

        if (buyer == null || seller == null) {
            throw new IllegalStateException("Buyer or seller portfolio missing for trade " + trade.getTradeId());
        }

        double settlement = trade.getQuantity() * trade.getExecutionPrice();
        buyer.debitCash(settlement);
        buyer.addShares(trade.getSymbol(), trade.getQuantity());
        seller.creditCash(settlement);
        seller.removeShares(trade.getSymbol(), trade.getQuantity());

        System.out.printf("[PORTFOLIO-UPDATE] trade=%s | buyer=%s debited $%.2f + %d %s | seller=%s credited $%.2f - %d %s%n",
                trade.getTradeId(), trade.getBuyerUserId(), settlement, trade.getQuantity(), trade.getSymbol(),
                trade.getSellerUserId(), settlement, trade.getQuantity(), trade.getSymbol());
    }
}
