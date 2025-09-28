package org.vasyaradulsoftware.arbitragelib;

import java.io.Closeable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TimeZone;

import org.vasyaradulsoftware.arbitragelib.Ticker.Param;
import org.vasyaradulsoftware.arbitragelib.exchange.GateFutures;
import org.vasyaradulsoftware.arbitragelib.exchange.GateSpot;


import decimal.Decimal;

public class TradingPair
{
    private static List<Exchange> exchanges = new ArrayList<Exchange>();

    private static List<TradingPair> traidingPairs = new ArrayList<TradingPair>();
    private int followCounter = 1;

    private List<SinglePair> subscribtions = new ArrayList<SinglePair>();
    private List<Bunch> bunches = new ArrayList<Bunch>();

    private String baseCurrency;
    private String quoteCurrency;

    private long timestamp;
    private DateFormat dateFormat;


    public static void init()
    {
        //exchanges.add(BybitSpot.create());
        //exchanges.add(BybitFutures.create());
        //exchanges.add(OkxSpot.create());
        //exchanges.add(OkxFutures.create());
        exchanges.add(GateSpot.create());
        exchanges.add(GateFutures.create());
    }

    public static TradingPair follow(String baseCurrency, String quoteCurrency) {
        try {
            TradingPair pair = traidingPairs
                .stream()
                .filter(
                    p -> p.baseCurrency.equals(baseCurrency) && p.quoteCurrency.equals(quoteCurrency)
                )
                .iterator()
                .next();
            System.out.println("traiding pair " + baseCurrency + "_" + quoteCurrency + " follow");
            pair.followCounter++;
            return pair;
        } catch (NoSuchElementException e) {
            System.out.println("traiding pair " + baseCurrency + "_" + quoteCurrency + " created");
            return new TradingPair(baseCurrency, quoteCurrency);
        }
    }

    public void unfollow() {
        System.out.println("traiding pair " + baseCurrency + "_" + quoteCurrency + " unfollow");
        followCounter--;
        if (followCounter <= 0) {
            System.out.println("traiding pair " + baseCurrency + "_" + quoteCurrency + " closed");
            traidingPairs.remove(this);
            subscribtions.forEach(s -> s.close());
        }
    }

    private TradingPair(String baseCurrency, String quoteCurrency) {
        this.baseCurrency = baseCurrency;
        this.quoteCurrency = quoteCurrency;

        for (Exchange e : exchanges) {
            subscribtions.add(new SinglePair(e));
        }

        for (SinglePair s1 : subscribtions) {
            for (SinglePair s2 : subscribtions) {
                if (s1 != s2) {
                    bunches.add(new Bunch(s1, s2));
                }
            }
        }

        dateFormat = new SimpleDateFormat("HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        traidingPairs.add(this);
    }

    private void updateTimestamp() {
        try {
            timestamp = subscribtions
                .stream()
                .min((SinglePair s1, SinglePair s2) -> Long.compare(s2.getTimestamp(), s1.getTimestamp()))
                .get()
                .getTimestamp();
        } catch (NoSuchElementException e) {}
    }

    private void checkSubscribtionsStatus()
    {
        {
            Iterator<SinglePair> i = subscribtions.iterator();
            while (i.hasNext())
            {
                if (i.next().isClosed()) i.remove();
            }
        }
        {
            Iterator<Bunch> i = bunches.iterator();
            while (i.hasNext())
            {
                if (i.next().isClosed()) i.remove();
            }
        }
    }

    public class NoSubscribtionsExeption extends Exception {}

    public String getPriceInfo() throws NoSubscribtionsExeption
    {
        checkSubscribtionsStatus();
        updateTimestamp();
        if (subscribtions.isEmpty())
        {
            throw new NoSubscribtionsExeption();
        }
        String info = "(" + dateFormat.format(timestamp) + " UTC) " + baseCurrency + "/" + quoteCurrency + " Price:";
        for (SinglePair s : subscribtions)
        {
            if (!s.isClosed())
            info = info + "\n" + s.exchange.getName() + 
            "\tLast: " + s.ticker.get(Param.LAST_PRICE).toString() +
            "\tAsk: " + s.orderbook.getBestAskPrice() +
            "\tBid: " + s.orderbook.getBestBidPrice();
        }
        return info;
    }

    public String getSpreadInfo() throws NoSubscribtionsExeption
    {
        checkSubscribtionsStatus();
        updateTimestamp();
        if (bunches.isEmpty()) {
            throw new NoSubscribtionsExeption();
        }
        sortBunchesBySpread();
        String info = "(" + dateFormat.format(timestamp) + " UTC) " + baseCurrency + "/" + quoteCurrency + " - " + subscribtions.get(0).ticker.get(Param.LAST_PRICE).toString() + "\nСпреды:";

        int i = 1;
        for (Bunch b : bunches)
        {
            if (i >= 6) break;
            if (!b.isClosed())
                info = info + "\n" + Integer.toString(i) + ". " + b.getInfo();
            i++;
        }

        return info;
    }

    protected class SinglePair implements Closeable
    {
        public final Exchange exchange;
        public final Ticker ticker;
        public final Orderbook orderbook;

        public SinglePair(Exchange e) {
            exchange = e;
            ticker = e.getTicker(baseCurrency, quoteCurrency);
            orderbook = e.getOrderbook(baseCurrency, quoteCurrency);
        }

        @Override
        public void close() {
            ticker.unfollow();
            orderbook.unfollow();
        }

        public boolean isClosed() { return ticker.isClosed() || orderbook.isClosed(); }

        public long getTimestamp() { return Long.min(ticker.getTimestamp(), orderbook.getTimestamp()); }
    }

    protected class Bunch
    {
        private SinglePair longEntry;
        private SinglePair shortEntry;

        public Bunch(SinglePair longEntry, SinglePair shortEntry)
        {
            this.longEntry = longEntry;
            this.shortEntry = shortEntry;
        }

        public Decimal calculateSpread() {
            return shortEntry.ticker.get(Param.LAST_PRICE).clone().divRD(longEntry.ticker.get(Param.LAST_PRICE)).add(-1);
        }

        public Decimal calculateEntrySpread() {
            return shortEntry.orderbook.getBestAskPrice().clone().divRD(longEntry.orderbook.getBestBidPrice()).add(-1);
        }

        public Decimal calculateExitSpread() {
            return longEntry.orderbook.getBestAskPrice().clone().divRD(shortEntry.orderbook.getBestBidPrice()).add(-1);
        }

        public SinglePair getLongEntry() {
            return longEntry;
        }

        public SinglePair getShortEntry() {
            return shortEntry;
        }
        
        public String getBunchName() {
            return longEntry.exchange.getName() + " -> " + shortEntry.exchange.getName();
        }

        public String getInfo() {
            return "[" + getBunchName() + "]: E:" +
            calculateEntrySpread().mul(100).roundDown(2) + "%, X:" +
            calculateExitSpread().mul(100).roundDown(2) + "%";
        }

        public boolean isClosed() {
            return longEntry.isClosed() || shortEntry.isClosed();
        }
    }

    private void sortBunchesBySpread()
    {
        bunches.sort((Bunch s1, Bunch s2) -> s2.calculateSpread().compareTo(s1.calculateSpread()));
    }

    public static void main(String[] args) {
        TradingPair.init();

        TradingPair p = TradingPair.follow("BTC", "USDT");

        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                System.out.println(p.getPriceInfo());
            } catch (NoSubscribtionsExeption e) {
                System.out.println("нема тiкерiв");
            }
        }
    }
}
