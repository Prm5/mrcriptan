package org.vasyaradulsoftware.arbitragelib;

public class Request {
    private Ticker ticker;

    public Request(Ticker ticker)
    {
        this.ticker = ticker;
    }

    public Ticker getTicker() {
        return ticker;
    }
}
