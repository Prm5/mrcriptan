package org.vasyaradulsoftware.arbitragelib;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

import org.vasyaradulsoftware.arbitragelib.exchange.Exchange;

import decimal.Decimal;

public class Ticker implements Closeable
{
    private volatile boolean subscribed;
    private volatile boolean subscribing;
    private volatile boolean succsess;

    private Exchange exchange;

    private String baseCurrency;
    private String quoteCurrency;

    private class TickerParam extends Decimal
    {
        private long actualityTimastamp = 0;
    }

    public enum Param
    {
        LAST_PRICE, ASK_PRICE, BID_PRICE;

        private static int valuesNumber = 0;
        public final int index;

        Param() {
            index = valuesNumberIncrement();
        }

        private int valuesNumberIncrement() {
            return valuesNumber++;
        }
    }

    private List<TickerParam> params = new ArrayList<TickerParam>();

    public Ticker(String baseCurrency, String quoteCurrency, Exchange exchange)
    {
        this.baseCurrency = baseCurrency;
        this.quoteCurrency = quoteCurrency;
        this.exchange = exchange;
        succsess = true;
        subscribed = false;
        subscribing = false;

        for (int i = 0; i < Param.values().length; i++) {
            params.add(new TickerParam());
        }
    }

    @Override
    public void close()
    {
        succsess = false;
        exchange.unsubscribeTicker(this);
    }

    public void setSubscribingStatus() { 
        subscribing = true;
    }

    public void setSubscribedStatus() {
        subscribing = false;
        subscribed = true;
    }

    public void subscribingUnsuccessful() {
        subscribing = false;
        succsess = false;
    }

    public boolean isSubscribed() { return subscribed; }

    public boolean isSubscribing() { return subscribing; }

    public boolean isSuccsessful() { return succsess; }

    public String getBaseCurrency() { return baseCurrency; }

    public String getQuoteCurrency() { return quoteCurrency; }

    public long getTimestamp()
    {
        return params
            .stream()
            .min((TickerParam p1, TickerParam p2) -> Long.compare(p1.actualityTimastamp, p2.actualityTimastamp))
            .get()
            .actualityTimastamp;
    }

    public void update(Param param, long ts, Decimal value)
    {
        if (ts >= params.get(param.index).actualityTimastamp) {
            params.get(param.index).set(value);
            params.get(param.index).actualityTimastamp = ts;
        }
    }

    public void update(Param param, long ts)
    {
        if (ts >= params.get(param.index).actualityTimastamp) {
            params.get(param.index).actualityTimastamp = ts;
        }
    }

    public Decimal get(Param param)
    {
        return params.get(param.index);
    }

    public Exchange getExchange() { return exchange; }
}
