package org.vasyaradulsoftware.arbitragelib.exchange;

import java.text.ParseException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.vasyaradulsoftware.arbitragelib.Message;
import org.vasyaradulsoftware.arbitragelib.Orderbook;
import org.vasyaradulsoftware.arbitragelib.Ticker;
import org.vasyaradulsoftware.arbitragelib.WebSocketExchange;
import org.vasyaradulsoftware.arbitragelib.Subscribtion.Channel;

import decimal.Decimal;

public abstract class Okx extends WebSocketExchange
{
    public Okx(String name, ExType type)
    {
        super("wss://ws.okx.com:8443/ws/v5/public", name, type);
    }

    @Override
    protected Ticker createTicker(String baseCurrency, String quoteCurrency) {
        return new OkxTicker(baseCurrency, quoteCurrency);
    }

    protected class OkxTicker extends Ticker
    {
        protected OkxTicker(String baseCurrency, String quoteCurrency) {
            super(baseCurrency, quoteCurrency, Okx.this);
        }

        @Override
        public JSONObject genWSRequest(Op op, int id) {
            String operation = "unsubscribe";
            if (op == Op.SUBSCRIBE) operation = "subscribe";
            String inst = baseCurrency + "-" + quoteCurrency;
            if (type == ExType.FUTURES) inst = inst + "-SWAP";

            return new JSONObject()
                .put("id", id)
                .put("op", operation)
                .put("args", new JSONArray()
                    .put(new JSONObject()
                        .put("channel", "tickers")
                        .put("instId", inst)
                    )
                );
        }

        @Override
        public void update(JSONObject msg) {
            JSONObject d = msg.getJSONArray("data").getJSONObject(0);
            try {
                update(Param.LAST_PRICE, d.getLong("ts"), new Decimal().parse(d.getString("last")));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    protected abstract class OkxOrderbook extends Orderbook
    {
        protected OkxOrderbook(String baseCurrency, String quoteCurrency) {
            super(baseCurrency, quoteCurrency, Okx.this);
        }
        
        @Override
        public JSONObject genWSRequest(Op op, int id) {
            String operation = "unsubscribe";
            if (op == Op.SUBSCRIBE) operation = "subscribe";
            String inst = baseCurrency + "-" + quoteCurrency;
            if (type == ExType.FUTURES) inst = inst + "-SWAP";

            return new JSONObject()
                .put("id", id)
                .put("op", operation)
                .put("args", new JSONArray()
                    .put(new JSONObject()
                        .put("channel", "books5")
                        .put("instId", inst)
                    )
                );
        }
    }

    @Override
    protected Message parse(String message) {
        return new OkxMessage(message);
    }

    protected class OkxMessage extends Message
    {
        public OkxMessage(String message) {
            super(message);
        }
        
        @Override
        public boolean isResponce() {
            return o.has("id") && o.has("event");
        }

        @Override
        public Responce getResponce() {
            return new OkxResponce();
        }

        @Override
        public boolean isUpdate() {
            return o.has("arg") && o.has("data");
        }

        @Override
        public Update getUpdate() {
            return new OkxUpdate();
        }

        protected class OkxResponce extends Responce
        {
            @Override
            public int getResponceId() {
                return o.getInt("id");
            }

            @Override
            public boolean isSubscribedSuccessful() {
                return o.getString("event").equals("subscribe");
            }

            @Override
            public boolean isUnsubscribedSuccessful() {
                return o.getString("event").equals("unsubscribe");
            }

            @Override
            public boolean isSubscribeError() {
                return o.getString("event").equals("error");
            }

        }

        protected class OkxUpdate extends Update
        {
            JSONObject d = o.getJSONArray("data").getJSONObject(0);

            @Override
            public Channel getChannel() {
                if (o.getJSONObject("arg").getString("channel").equals("tickers")) return Channel.TICKER;
                if (o.getJSONObject("arg").getString("channel").equals("books5")) return Channel.ORDERBOOK;
                return null;
            }

            @Override
            public String getBaseCurrency() {
                return d.getString("instId").split("[-]")[0];
            }

            @Override
            public String getQuoteCurrency() {
                return d.getString("instId").split("[-]")[1];
            }
        }
    }
}
