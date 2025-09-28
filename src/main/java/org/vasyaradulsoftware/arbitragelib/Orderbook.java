package org.vasyaradulsoftware.arbitragelib;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.vasyaradulsoftware.arbitragelib.exchange.BybitFutures;

import decimal.Decimal;

public abstract class Orderbook extends Subscribtion
{
    protected long timestamp = 0;
    protected static final int level = 5;

    protected class Row {
        public Decimal price;
        public Decimal sizeQuote;
        public Decimal sizeBase;

        public Row(Decimal price, Decimal sizeQuote, Decimal sizeBase) {
            this.price = price;
            this.sizeQuote = sizeQuote;
            this.sizeBase = sizeBase;
        }
    }

    protected List<Row> ask = new ArrayList<Row>();
    protected List<Row> bid = new ArrayList<Row>();

    public Orderbook(String baseCurrency, String quoteCurrency, WebSocketExchange exchange) {
        super(baseCurrency, quoteCurrency, Channel.ORDERBOOK, exchange);
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    public synchronized Decimal getBestAskPrice() {
        return ask.get(0).price;
    }

    public synchronized Decimal getBestBidPrice() {
        return bid.get(0).price;
    }

    public synchronized String getTable()
    {
        String t = baseCurrency + quoteCurrency + "\tPrice:\tSize(" + quoteCurrency + "):\tSize(" + baseCurrency + "):";

        t = t + "\n";

        for (int i = ask.size() - 1; i >= 0; i--) {
            Row r = ask.get(i);
            t = t + "\n";
            if (i == 0) t = t + "Sell:";
            t = t + "\t" + r.price + "\t" + r.sizeQuote + "\t" + r.sizeBase;
        }

        t = t + "\n";

        for (int i = 0; i < bid.size(); i++) {
            Row r = bid.get(i);
            t = t + "\n";
            if (i == 0) t = t + "Buy:";
            t = t + "\t" + r.price + "\t" + r.sizeQuote + "\t" + r.sizeBase;
        }

        return t;
    }

    protected synchronized void update(UpdType type, long timestamp, List<Row> ask, List<Row> bid) {
        if (timestamp < this.timestamp) return;
        this.timestamp = timestamp;
        switch (type)
        {
            case UpdType.SNAPSHOT:
                if (ask != null) {
                    ask.sort((r1, r2) -> r1.price.compareTo(r2.price));
                    this.ask = ask;
                }
                if (bid != null) {
                    bid.sort((r1, r2) -> r2.price.compareTo(r1.price));
                    this.bid = bid;
                }
                break;
            
            case UpdType.DELTA:
                if (ask != null)
                {
                    ask.forEach(upd ->
                    {
                        try {
                            Row t = this.ask
                                .stream()
                                .filter(row -> row.price.equals(upd.price))
                                .findFirst()
                                .get();
                            t.sizeQuote = upd.sizeQuote;
                            t.sizeBase = upd.sizeBase;
                        } catch (NoSuchElementException e) {
                            this.ask.add(upd);
                        }
                    });
                    this.ask.removeIf(r -> r.sizeQuote.getRaw() == 0 || r.sizeBase.getRaw() == 0);
                    this.ask.sort((r1, r2) -> r1.price.compareTo(r2.price));
                }
                if (bid != null)
                {
                    bid.forEach(upd ->
                    {
                        try {
                            Row t = this.bid
                                .stream()
                                .filter(row -> row.price.equals(upd.price))
                                .findFirst()
                                .get();
                            t.sizeQuote = upd.sizeQuote;
                            t.sizeBase = upd.sizeBase;
                        } catch (NoSuchElementException e) {
                            this.bid.add(upd);
                        }
                    });
                    this.bid.removeIf(r -> r.sizeQuote.getRaw() == 0 || r.sizeBase.getRaw() == 0);
                    this.bid.sort((r1, r2) -> r2.price.compareTo(r1.price));
                }
                break;

            default:
                break;
        }
        if (this.ask.size() > level) this.ask = this.ask.subList(0, level);
        if (this.bid.size() > level) this.bid = this.bid.subList(0, level);
    }

    public static void main(String[] args)
    {
        Orderbook ob = new BybitFutures().getOrderbook("BTC", "USDT");
        String t;
        while (true) {
            t = ob.getTable();
            System.out.println("\n" + t);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {}
        }
    }
}
