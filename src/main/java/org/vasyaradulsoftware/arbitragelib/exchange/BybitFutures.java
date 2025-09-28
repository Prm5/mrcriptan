package org.vasyaradulsoftware.arbitragelib.exchange;

import java.net.URISyntaxException;

public class BybitFutures extends Bybit {

    protected BybitFutures() throws URISyntaxException {
        super("wss://stream.bybit.com/v5/public/linear", "Bybit(futures)", ExType.FUTURES);
    }
    
    public static BybitFutures create() {
        try {
            return new BybitFutures();
        } catch (URISyntaxException e) {
            return null;
        }
    }
}