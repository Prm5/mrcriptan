package org.vasyaradulsoftware.arbitragelib;

import java.io.Closeable;

import org.json.JSONObject;

public abstract class Subscribtion implements Closeable
{

    public enum Op {SUBSCRIBE, UNSUBSCRIBE}
    public enum UpdType {SNAPSHOT, DELTA}
    public enum Channel {TICKER, ORDERBOOK}

    private boolean subscribed = false;
    private boolean closed = false;
    private int followCounter = 1;

    public final WebSocketExchange exchange;

    public final String baseCurrency;
    public final String quoteCurrency;
    public final Channel channel;
    
    public Subscribtion(String baseCurrency, String quoteCurrency, Channel channel, WebSocketExchange exchange)
    {
        this.exchange = exchange;
        this.baseCurrency = baseCurrency;
        this.quoteCurrency = quoteCurrency;
        this.channel = channel;

        subscribe();
    }

    @Override
    public void close()
    {
        if (!closed) unsubscribe();
        closed = true;
    }

    public void follow() {
        followCounter++;
    }

    public void unfollow() {
        if (--followCounter <= 0) close();
    }

    private long lastSubscribeReqTs;

    protected void checkTimestamp() {
        if (System.currentTimeMillis() - getTimestamp() > 5000 && System.currentTimeMillis() - lastSubscribeReqTs > 5000) {
            System.out.println("timeout");
            subscribe();
            lastSubscribeReqTs = System.currentTimeMillis();
        }
    }

    private void subscribe() {
        exchange.send(genWSRequest(Op.SUBSCRIBE, exchange.regRequest(this)).toString());
    }

    private void unsubscribe() {
        exchange.send(genWSRequest(Op.UNSUBSCRIBE, exchange.regRequest(this)).toString());
    }

    public abstract JSONObject genWSRequest(Op op, int id);

    public void onSubscribingSuccessful() {
        subscribed = true;
    }

    public void onSubscribingFailed() {
        closed = true;
    }

    public boolean isSubscribed() {
        return subscribed;
    }

    public boolean isClosed() {
        return closed;
    }

    public abstract long getTimestamp();

    public abstract void update(JSONObject message);
}
