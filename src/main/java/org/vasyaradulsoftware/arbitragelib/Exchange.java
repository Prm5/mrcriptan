package org.vasyaradulsoftware.arbitragelib;

import java.util.ArrayList;
import java.util.List;

import org.vasyaradulsoftware.arbitragelib.exchange.BybitFutures;
import org.vasyaradulsoftware.arbitragelib.exchange.BybitSpot;
import org.vasyaradulsoftware.arbitragelib.exchange.GateFutures;
import org.vasyaradulsoftware.arbitragelib.exchange.GateSpot;
import org.vasyaradulsoftware.arbitragelib.exchange.OkxFutures;
import org.vasyaradulsoftware.arbitragelib.exchange.OkxSpot;

public interface Exchange {

    public static final List<Exchange> publicExchanges = new ArrayList<Exchange>();

    public static void initExchanges() {
        publicExchanges.add(new BybitSpot());
        publicExchanges.add(new BybitFutures());
        publicExchanges.add(new OkxSpot());
        publicExchanges.add(new OkxFutures());
        publicExchanges.add(new GateSpot());
        publicExchanges.add(new GateFutures());
    }

    public enum ExType {SPOT, FUTURES}

    public Ticker getTicker(String baseCurrency, String quoteCurrency);
    public Orderbook getOrderbook(String baseCurrency, String quoteCurrency);

    public String getName();
}
