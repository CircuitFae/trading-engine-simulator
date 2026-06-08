package com.tradingengine.domain;

import java.time.LocalDate;

public class Option extends Asset {

    private final double strikePrice;
    private final LocalDate expirationDate;
    private final String optionType;

    public Option(String symbol, String companyName, double currentPrice,
                  double strikePrice, LocalDate expirationDate, String optionType) {
        super(symbol, companyName, currentPrice);
        if (strikePrice < 0) {
            throw new IllegalArgumentException("Strike price cannot be negative");
        }
        if (expirationDate == null) {
            throw new IllegalArgumentException("Expiration date cannot be null");
        }
        if (optionType == null || (!optionType.equals("CALL") && !optionType.equals("PUT"))) {
            throw new IllegalArgumentException("Option type must be CALL or PUT");
        }
        this.strikePrice = strikePrice;
        this.expirationDate = expirationDate;
        this.optionType = optionType;
    }

    public double getStrikePrice() {
        return strikePrice;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public String getOptionType() {
        return optionType;
    }

    @Override
    public String toString() {
        return "Option{symbol='" + getSymbol() + "', strikePrice=" + strikePrice
                + ", expirationDate=" + expirationDate + ", optionType='" + optionType
                + "', currentPrice=" + getCurrentPrice() + "}";
    }
}
