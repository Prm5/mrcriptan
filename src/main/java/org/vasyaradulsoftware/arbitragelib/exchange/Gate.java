package org.vasyaradulsoftware.arbitragelib.exchange;

import java.net.URISyntaxException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.vasyaradulsoftware.arbitragelib.Message;
import org.vasyaradulsoftware.arbitragelib.Orderbook;
import org.vasyaradulsoftware.arbitragelib.Ticker;
import org.vasyaradulsoftware.arbitragelib.WebSocketExchange;
import org.vasyaradulsoftware.arbitragelib.Subscribtion.Channel;

public abstract class Gate extends WebSocketExchange
{

    protected Gate(String url, String name, ExType type) throws URISyntaxException
    {
        super(url, name, type);
    }

    protected abstract class GateTicker extends Ticker
    {    
        protected GateTicker(String baseCurrency, String quoteCurrency) {
            super(baseCurrency, quoteCurrency, Gate.this);
        }

        @Override
        public JSONObject genWSRequest(Op op, int id) {
            String operation = "unsubscribe";
            if (op == Op.SUBSCRIBE) operation = "subscribe";
            String t = "spot";
            if (type == ExType.FUTURES) t = "futures";

            return new JSONObject()
                .put("time", System.currentTimeMillis()/1000)
                .put("id", id)
                .put("channel", t + ".tickers")
                .put("event", operation)
                .put("payload", new JSONArray().put(baseCurrency + "_" + quoteCurrency));
        }
    }

    protected abstract class GateOrderbook extends Orderbook
    {
        protected GateOrderbook(String baseCurrency, String quoteCurrency) {
            super(baseCurrency, quoteCurrency, Gate.this);
        }

        @Override
        public JSONObject genWSRequest(Op op, int id) {
            String operation = "unsubscribe";
            if (op == Op.SUBSCRIBE) operation = "subscribe";
            String t = "spot";
            String interval = "100ms";
            if (type == ExType.FUTURES) {
                t = "futures";
                interval = "0";
            }

            return new JSONObject()
                .put("time", System.currentTimeMillis()/1000)
                .put("id", id)
                .put("channel", t + ".order_book")
                .put("event", operation)
                .put("payload", new JSONArray()
                    .put(baseCurrency + "_" + quoteCurrency)
                    .put(Integer.toString(level))
                    .put(interval)
                );
        }
    }

    protected abstract class GateMessage extends Message
    {
        protected GateMessage(String message) {
            super(message);
        }
        
        @Override
        public boolean isResponce() {
            return
                o.has("event") && 
                (
                    o.getString("event").equals("subscribe") ||
                    o.getString("event").equals("unsubscribe")
                );
        }

        @Override
        public Responce getResponce() {
            return new GateResponce();
        }

        @Override
        public boolean isUpdate() {
            return
                o.has("event") &&
                (
                    o.getString("event").equals("update") ||
                    o.getString("event").equals("all")
                );
        }

        protected class GateResponce extends Responce
        {
            @Override
            public int getResponceId() {
                return o.getInt("id");
            }

            @Override
            public boolean isSubscribedSuccessful() {
                return 
                    o.getString("event").equals("subscribe") &&
                    o.getJSONObject("result").getString("status").equals("success");
            }

            @Override
            public boolean isUnsubscribedSuccessful() {
                return
                    o.getString("event").equals("unsubscribe") &&
                    o.getJSONObject("result").getString("status").equals("success");
            }

            @Override
            public boolean isSubscribeError() {
                return 
                    o.getString("event").equals("subscribe") &&
                    o.getJSONObject("result").getString("status").equals("fail");
            }
        }

        protected abstract class GateUpdate extends Update
        {
            @Override
            public Channel getChannel() {
                if (
                    o.has("channel") && 
                    o.getString("channel").contains(".tickers")
                ) return Channel.TICKER;
                if (
                    o.has("channel") && 
                    o.getString("channel").contains(".order_book")
                ) return Channel.ORDERBOOK;
                return null;
            }
        }
    }
}
