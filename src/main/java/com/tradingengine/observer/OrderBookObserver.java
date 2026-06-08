package com.tradingengine.observer;

import com.tradingengine.domain.Trade;

public interface OrderBookObserver {

    void onTradeExecuted(Trade trade);
}
