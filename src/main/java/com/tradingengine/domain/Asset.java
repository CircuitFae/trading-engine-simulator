package com.tradingengine.domain;

public abstract class Asset {

    private final String symbol;
    private final String companyName;
    private double currentPrice;

    protected Asset(String symbol, String companyName, double currentPrice) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol cannot be null or blank");
        }
        if (companyName == null || companyName.isBlank()) {
            throw new IllegalArgumentException("Company name cannot be null or blank");
        }
        if (currentPrice < 0) {
            throw new IllegalArgumentException("Current price cannot be negative");
        }
        this.symbol = symbol;
        this.companyName = companyName;
        this.currentPrice = currentPrice;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getCompanyName() {
        return companyName;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void updatePrice(double price) {
        if (price < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
        this.currentPrice = price;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{symbol='" + symbol + "', companyName='" + companyName
                + "', currentPrice=" + currentPrice + "}";
    }
}
