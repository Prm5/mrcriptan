package org.vasyaradulsoftware.arbitragelib;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TimeZone;

import org.vasyaradulsoftware.arbitragelib.Ticker.Param;
import org.vasyaradulsoftware.arbitragelib.exchange.BybitFutures;
import org.vasyaradulsoftware.arbitragelib.exchange.BybitSpot;
import org.vasyaradulsoftware.arbitragelib.exchange.Exchange;
import org.vasyaradulsoftware.arbitragelib.exchange.GateFutures;
import org.vasyaradulsoftware.arbitragelib.exchange.GateSpot;
import org.vasyaradulsoftware.arbitragelib.exchange.OkxFutures;
import org.vasyaradulsoftware.arbitragelib.exchange.OkxSpot;

import decimal.Decimal;

public class TradingPair
{
    private static List<Exchange> exchanges = new ArrayList<Exchange>();

    private static List<TradingPair> traidingPairs = new ArrayList<TradingPair>();
    private int followCounter = 1;

    private List<Ticker> tickers = new ArrayList<Ticker>();
    private List<Bunch> bunches = new ArrayList<Bunch>();

    private String baseCurrency;
    private String quoteCurrency;

    private long timestamp;
    private DateFormat dateFormat;


    public static void init()
    {
        exchanges.add(BybitSpot.create());
        exchanges.add(BybitFutures.create());
        exchanges.add(OkxSpot.create());
        exchanges.add(OkxFutures.create());
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
            tickers.forEach(t -> t.close());
        }
    }

    private TradingPair(String baseCurrency, String quoteCurrency) {
        this.baseCurrency = baseCurrency;
        this.quoteCurrency = quoteCurrency;

        for (Exchange e : exchanges) {
            tickers.add(e.subscribeTicker(baseCurrency, quoteCurrency));
        }

        for (Ticker t1 : tickers) {
            for (Ticker t2 : tickers) {
                if (t1 != t2) {
                    bunches.add(new Bunch(t1, t2));
                }
            }
        }

        dateFormat = new SimpleDateFormat("HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        traidingPairs.add(this);
    }

    public void updateTimestamp() {
        try {
            timestamp = tickers
                .stream()
                .min((Ticker t1, Ticker t2) -> Long.compare(t2.getTimestamp(), t1.getTimestamp()))
                .get()
                .getTimestamp();
        } catch (NoSuchElementException e) {}
    }

    public void checkTickersStatus()
    {
        {
            Iterator<Ticker> i = tickers.iterator();
            while (i.hasNext())
            {
                if (!i.next().isSuccsessful())
                {
                    i.remove();
                }
            }
        }
        {
            Iterator<Bunch> i = bunches.iterator();
            while (i.hasNext())
            {
                if (!i.next().isSuccsessful())
                {
                    i.remove();
                }
            }
        }
    }

    public class NoTickersExeption extends Exception {}

    public String getPriceInfo() throws NoTickersExeption
    {
        checkTickersStatus();
        updateTimestamp();
        if (tickers.isEmpty())
        {
            throw new NoTickersExeption();
        }
        String info = "(" + dateFormat.format(timestamp) + " UTC) " + baseCurrency + "/" + quoteCurrency + " Price:";
        for (Ticker t : tickers)
        {
            if (t.isSubscribed())
            info = info + "\n" + t.getExchange().getName() + 
            "\tLast: " + t.get(Param.LAST_PRICE).toString() +
            "\tAsk: " + t.get(Param.ASK_PRICE).toString() +
            "\tBid: " + t.get(Param.BID_PRICE).toString();
        }
        return info;
    }

    public String getSpreadInfo() throws NoTickersExeption
    {
        checkTickersStatus();
        updateTimestamp();
        if (bunches.isEmpty()) {
            throw new NoTickersExeption();
        }
        sortBunchesBySpread();
        String info = "(" + dateFormat.format(timestamp) + " UTC) " + baseCurrency + "/" + quoteCurrency + " - " + tickers.get(0).get(Param.LAST_PRICE).toString() + "\nСпреды:";

        int i = 1;
        for (Bunch b : bunches)
        {
            if (i >= 6) break;
            if (b.isSubscribed())
                info = info + "\n" + Integer.toString(i) + ". " + b.getInfo();
            i++;
        }

        return info;
    }

    class Bunch
    {
        private Ticker longEntry;
        private Ticker shortEntry;

        public Bunch(Ticker longEntry, Ticker shortEntry)
        {
            this.longEntry = longEntry;
            this.shortEntry = shortEntry;
        }

        public Decimal calculateSpread() {
            return shortEntry.get(Param.LAST_PRICE).clone().divRD(longEntry.get(Param.LAST_PRICE)).add(-1);
        }

        public Decimal calculateEntrySpread() {
            return shortEntry.get(Param.ASK_PRICE).clone().divRD(longEntry.get(Param.BID_PRICE)).add(-1);
        }

        public Decimal calculateExitSpread() {
            return longEntry.get(Param.ASK_PRICE).clone().divRD(shortEntry.get(Param.BID_PRICE)).add(-1);
        }

        public Ticker getLongEntry() {
            return longEntry;
        }

        public Ticker getShortEntry() {
            return shortEntry;
        }
        
        public String getExchanges() {
            return longEntry.getExchange().getName() + " -> " + shortEntry.getExchange().getName();
        }

        public String getInfo() {
            return "[" + getExchanges() + "]: E:" +
            calculateEntrySpread().mul(100).roundDown(2) + "%, X:" +
            calculateExitSpread().mul(100).roundDown(2) + "%";
        }

        public boolean isSuccsessful() {
            return longEntry.isSuccsessful() && shortEntry.isSuccsessful();
        }

        public boolean isSubscribed() {
            return longEntry.isSubscribed() && shortEntry.isSubscribed();
        }
    }

    private void sortBunchesBySpread()
    {
        bunches.sort((Bunch s1, Bunch s2) -> s2.calculateSpread().compareTo(s1.calculateSpread()));
    }
}
