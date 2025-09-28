package org.vasyaradulsoftware.arbitragelib;

import java.util.ArrayList;
import java.util.List;

import decimal.Decimal;

public abstract class Ticker extends Subscribtion
{
    private class TickerParam extends Decimal
    {
        private long timestamp = 0;
    }

    public enum Param
    {
        LAST_PRICE;

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

    public Ticker(String baseCurrency, String quoteCurrency, WebSocketExchange exchange) {
        super(baseCurrency, quoteCurrency, Channel.TICKER, exchange);
        for (int i = 0; i < Param.values().length; i++) {
            params.add(new TickerParam());
        }
    }

    @Override
    public long getTimestamp()
    {
        return params
            .stream()
            .min((TickerParam p1, TickerParam p2) -> Long.compare(p1.timestamp, p2.timestamp))
            .get()
            .timestamp;
    }

    public Decimal get(Param param)
    {
        return params.get(param.index);
    }

    protected void update(Param param, long ts, Decimal value)
    {
        if (ts >= params.get(param.index).timestamp) {
            params.get(param.index).set(value);
            params.get(param.index).timestamp = ts;
        }
    }

    protected void update(Param param, long ts)
    {
        if (ts >= params.get(param.index).timestamp) {
            params.get(param.index).timestamp = ts;
        }
    }
}
