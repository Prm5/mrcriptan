package org.vasyaradulsoftware.arbitragelib.exchange;

public class BybitSpot extends Bybit {

    public BybitSpot() {
        super("wss://stream.bybit.com/v5/public/spot", "BybitSpot", ExType.FUTURES);
    }
}