package org.vasyaradulsoftware.arbitragelib;

import java.io.Closeable;
import java.text.ParseException;

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

    private Decimal price;
    private long timestamp;

    public Ticker(String baseCurrency, String quoteCurrency, Exchange exchange)
    {
        this.baseCurrency = baseCurrency;
        this.quoteCurrency = quoteCurrency;
        this.exchange = exchange;
        succsess = true;
        subscribed = false;
        subscribing = false;

        price = new Decimal();
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

    public void update(String price, long ts)
    {
        if (ts >= timestamp) {
            try {
                this.price.parse(price);
                timestamp = ts;
            } catch (ParseException e) {
                System.err.println(e);
            }
        }
    }

    public Decimal getPrice() { return price; }

    public long getTimestamp() { return timestamp; }

    public Exchange getExchange() { return exchange; }
}
