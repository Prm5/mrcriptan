package org.vasyaradulsoftware.arbitragelib.exchange;

import java.net.URISyntaxException;

public class BybitSpot extends Bybit {

    protected BybitSpot() throws URISyntaxException {
        super("wss://stream.bybit.com/v5/public/spot", "Bybit(spot)", ExType.FUTURES);
    }
    
    public static BybitSpot create() {
        try {
            return new BybitSpot();
        } catch (URISyntaxException e) {
            return null;
        }
    }
}