package org.vasyaradulsoftware.arbitragelib.exchange;

public class BybitFutures extends Bybit {

    public BybitFutures() {
        super("wss://stream.bybit.com/v5/public/linear", "BybitFutures", ExType.FUTURES);
    }
}