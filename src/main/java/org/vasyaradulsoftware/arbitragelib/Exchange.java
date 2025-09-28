package org.vasyaradulsoftware.arbitragelib;

public interface Exchange {

    public enum ExType {SPOT, FUTURES}

    public Ticker getTicker(String baseCurrency, String quoteCurrency);
    public Orderbook getOrderbook(String baseCurrency, String quoteCurrency);

    public String getName();
}
