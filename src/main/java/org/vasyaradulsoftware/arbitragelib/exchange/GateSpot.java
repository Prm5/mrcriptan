package org.vasyaradulsoftware.arbitragelib.exchange;

import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.vasyaradulsoftware.arbitragelib.Message;
import org.vasyaradulsoftware.arbitragelib.Orderbook;
import org.vasyaradulsoftware.arbitragelib.Subscribtion.Channel;
import org.vasyaradulsoftware.arbitragelib.Ticker;

import decimal.Decimal;

public class GateSpot extends Gate
{
    private GateSpot() throws URISyntaxException
    {
        super("wss://api.gateio.ws/ws/v4/", "Gate(spot)", ExType.SPOT);
    }

    public static GateSpot create() {
        try {
            return new GateSpot();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @Override
    protected Ticker createTicker(String baseCurrency, String quoteCurrency) {
        return new GateSpotTicker(baseCurrency, quoteCurrency);
    }

    protected class GateSpotTicker extends GateTicker
    {
        private GateSpotTicker(String baseCurrency, String quoteCurrency) {
            super(baseCurrency, quoteCurrency);
        }

        @Override
        public void update(JSONObject msg) {
            try {
                update(Param.LAST_PRICE, msg.getLong("time_ms"), new Decimal().parse(msg.getJSONObject("result").getString("last")));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected Orderbook createOrderbook(String baseCurrency, String quoteCurrency) {
        return new GateSpotOrderbook(baseCurrency, quoteCurrency);
    }

    protected class GateSpotOrderbook extends GateOrderbook
    {
        private GateSpotOrderbook(String baseCurrency, String quoteCurrency) {
            super(baseCurrency, quoteCurrency);
        }

        @Override
        public void update(JSONObject msg)
        {
            List<Row> ask = new ArrayList<Row>();
            JSONArray askJSON = msg.getJSONObject("result").getJSONArray("asks");

            for (int i = 0; i < askJSON.length(); i++) {
                try {
                    Decimal price = new Decimal().parse(askJSON.getJSONArray(i).getString(0));
                    Decimal sizeBase = new Decimal().parse(askJSON.getJSONArray(i).getString(1));
                    Decimal sizeQuote = sizeBase.clone().mulRD(price);
                    ask.add(new Row(price, sizeQuote, sizeBase));
                }
                catch (ParseException e) {}
            }

            List<Row> bid = new ArrayList<Row>();
            JSONArray bidJSON = msg.getJSONObject("result").getJSONArray("bids");

            for (int i = 0; i < bidJSON.length(); i++) {
                try {
                    Decimal price = new Decimal().parse(bidJSON.getJSONArray(i).getString(0));
                    Decimal sizeBase = new Decimal().parse(bidJSON.getJSONArray(i).getString(1));
                    Decimal sizeQuote = sizeBase.clone().mulRD(price);
                    bid.add(new Row(price, sizeQuote, sizeBase));
                }
                catch (ParseException e) {}
            }

            update(UpdType.SNAPSHOT, msg.getJSONObject("result").getLong("t"), ask, bid);
        }
    }

    @Override
    protected Message parse(String message) {
        return new GateFuturesMessage(message);
    }

    protected class GateFuturesMessage extends GateMessage
    {
        protected GateFuturesMessage(String message) {
            super(message);
        }
        
        @Override
        public Update getUpdate() {
            return new GateFuturesUpdate();
        }

        protected class GateFuturesUpdate extends GateUpdate
        {
            @Override
            public String getBaseCurrency() {
                switch (getChannel()) {
                    case Channel.TICKER:
                        return o.getJSONObject("result").getString("currency_pair").split("[_]")[0];

                    case Channel.ORDERBOOK:
                        return o.getJSONObject("result").getString("s").split("[_]")[0];
                
                    default:
                        return null;
                }
            }

            @Override
            public String getQuoteCurrency() {
                switch (getChannel()) {
                    case Channel.TICKER:
                        return o.getJSONObject("result").getString("currency_pair").split("[_]")[1];

                    case Channel.ORDERBOOK:
                        return o.getJSONObject("result").getString("s").split("[_]")[1];
                
                    default:
                        return null;
                }
            }
        }
    }
}
