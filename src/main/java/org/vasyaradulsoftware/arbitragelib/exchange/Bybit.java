package org.vasyaradulsoftware.arbitragelib.exchange;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.vasyaradulsoftware.arbitragelib.Message;
import org.vasyaradulsoftware.arbitragelib.Orderbook;
import org.vasyaradulsoftware.arbitragelib.Ticker;
import org.vasyaradulsoftware.arbitragelib.WebSocketExchange;
import org.vasyaradulsoftware.arbitragelib.Subscribtion.Channel;

import decimal.Decimal;

public abstract class Bybit extends WebSocketExchange  {

    protected Bybit(String url, String name, ExType type) {
        super(url, name, type);
    }

    @Override
    protected Ticker createTicker(String baseCurrency, String quoteCurrency) {
        return new BybitTicker(baseCurrency, quoteCurrency);
    }

    protected class BybitTicker extends Ticker
    {
        public BybitTicker(String baseCurrency, String quoteCurrency) {
            super(baseCurrency, quoteCurrency, Bybit.this);
        }
        
        @Override
        public JSONObject genWSRequest(Op op, int id) {
            String operation = "unsubscribe";
            if (op == Op.SUBSCRIBE) operation = "subscribe";

            return new JSONObject()
                .put("req_id", id)
                .put("op", operation)
                .put("args", new JSONArray()
                    .put("tickers." + baseCurrency + quoteCurrency)
                );
        }

        @Override
        public void update(JSONObject msg) {
            try {
                update(Param.LAST_PRICE, msg.getLong("ts"), new Decimal().parse(msg.getJSONObject("data").getString("lastPrice")));
            } catch (JSONException e) {
                update(Param.LAST_PRICE, msg.getLong("ts"));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected Orderbook createOrderbook(String baseCurrency, String quoteCurrency) {
        return new BybitOrderbook(baseCurrency, quoteCurrency);
    }

    protected class BybitOrderbook extends Orderbook
    {
        public BybitOrderbook(String baseCurrency, String quoteCurrency) {
            super(baseCurrency, quoteCurrency, Bybit.this);
        }

        @Override
        public JSONObject genWSRequest(Op op, int id) {
            String operation = "unsubscribe";
            if (op == Op.SUBSCRIBE) operation = "subscribe";

            return new JSONObject()
                .put("req_id", id)
                .put("op", operation)
                .put("args", new JSONArray()
                    .put("orderbook.50." + baseCurrency + quoteCurrency)
                );
        }

        @Override
        public void update(JSONObject msg) {
            List<Row> ask = new ArrayList<Row>();
            JSONArray askJSON = msg.getJSONObject("data").getJSONArray("a");

            for (int i = 0; i < askJSON.length(); i++) {
                try {
                    Decimal price = new Decimal().parse(askJSON.getJSONArray(i).getString(0));
                    Decimal sizeBase = new Decimal().parse(askJSON.getJSONArray(i).getString(1));
                    Decimal sizeQuote = sizeBase.clone().mulRD(price);
                    ask.add(new Row(price, sizeQuote, sizeBase));
                }
                catch (JSONException e) {
                    break;
                }
                catch (ParseException e) {}
            }

            List<Row> bid = new ArrayList<Row>();
            JSONArray bidJSON = msg.getJSONObject("data").getJSONArray("b");

            for (int i = 0; i < bidJSON.length(); i++) {
                try {
                    Decimal price = new Decimal().parse(bidJSON.getJSONArray(i).getString(0));
                    Decimal sizeBase = new Decimal().parse(bidJSON.getJSONArray(i).getString(1));
                    Decimal sizeQuote = sizeBase.clone().mulRD(price);
                    bid.add(new Row(price, sizeQuote, sizeBase));
                }
                catch (JSONException e) {
                    break;
                }
                catch (ParseException e) {}
            }

            
            if (msg.getString("type").equals("delta"))
                update(UpdType.DELTA, msg.getLong("ts"), ask, bid);
            else if (msg.getString("type").equals("snapshot"))
                update(UpdType.SNAPSHOT, msg.getLong("ts"), ask, bid);
        }
    }

    @Override
    protected Message parse(String message) {
        return new BybitMessage(message);
    }

    protected class BybitMessage extends Message
    {
        private static final String[] quotes = {"USDT", "USDC", "USD1", "USD", "EUR", "BTC", "MNT", "SOL", "ETH"};

        public BybitMessage(String message) {
            super(message);
        }
        
        @Override
        public boolean isResponce() {
            return o.has("req_id") && o.has("op");
        }

        @Override
        public Responce getResponce() {
            return new BybitResponce();
        }

        @Override
        public boolean isUpdate() {
            return o.has("topic") && o.has("data");
        }

        @Override
        public Update getUpdate() {
            return new BybitUpdate();
        }

        protected class BybitResponce extends Responce
        {
            @Override
            public int getResponceId() {
                return o.getInt("req_id");
            }

            @Override
            public boolean isSubscribedSuccessful() {
                return o.getString("op").equals("subscribe") && o.has("success") && o.getBoolean("success");
            }

            @Override
            public boolean isUnsubscribedSuccessful() {
                return o.getString("op").equals("unsubscribe") && o.has("success") && o.getBoolean("success");
            }

            @Override
            public boolean isSubscribeError() {
                return o.getString("op").equals("subscribe") && o.has("success") && !o.getBoolean("success");
            }
        }

        protected class BybitUpdate extends Update
        {
            String[] topic = o.getString("topic").split("[.]");

            @Override
            public Channel getChannel() {
                if (topic[0].equals("tickers")) return Channel.TICKER;
                else if (topic[0].equals("orderbook")) return Channel.ORDERBOOK;
                else return null;
            }

            @Override
            public String getBaseCurrency() {
                String ticker = topic[topic.length - 1];
                for (String quote : quotes) {
                    if (ticker.substring(ticker.length() - quote.length(), ticker.length()).equals(quote)) {
                        return ticker.substring(0, ticker.length() - quote.length());
                    }
                }
                return null;
            }

            @Override
            public String getQuoteCurrency() {
                String ticker = topic[topic.length - 1];
                for (String quote : quotes) {
                    if (ticker.substring(ticker.length() - quote.length(), ticker.length()).equals(quote)) {
                        return quote;
                    }
                }
                return null;
            }
        }
    }
}
