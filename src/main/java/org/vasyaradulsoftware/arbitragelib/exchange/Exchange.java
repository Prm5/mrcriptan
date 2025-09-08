package org.vasyaradulsoftware.arbitragelib.exchange;

import java.util.function.Consumer;

import org.vasyaradulsoftware.arbitragelib.Ticker;

public interface Exchange extends Consumer<String> {

    public Ticker subscribeTicker(String baseCurrency, String quoteCurrency);
    public void unsubscribeTicker(Ticker ticker);

    public String getName();
}
