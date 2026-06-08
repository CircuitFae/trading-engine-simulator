package com.tradingengine.domain;

public class Stock extends Asset {

    private final String exchange;

    public Stock(String symbol, String companyName, double currentPrice, String exchange) {
        super(symbol, companyName, currentPrice);
        if (exchange == null || exchange.isBlank()) {
            throw new IllegalArgumentException("Exchange cannot be null or blank");
        }
        this.exchange = exchange;
    }

    public String getExchange() {
        return exchange;
    }

    @Override
    public String toString() {
        return "Stock{symbol='" + getSymbol() + "', companyName='" + getCompanyName()
                + "', exchange='" + exchange + "', currentPrice=" + getCurrentPrice() + "}";
    }
}
