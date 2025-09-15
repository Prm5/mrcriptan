package org.vasyaradulsoftware.arbitragelib.exchange;

import java.net.URISyntaxException;
import java.text.ParseException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.vasyaradulsoftware.arbitragelib.Message;
import org.vasyaradulsoftware.arbitragelib.Ticker.Param;

import decimal.Decimal;

public class Gate extends WebSocketExchange
{
    protected String type;

    protected Gate(String url, String name, String type) throws URISyntaxException
    {
        super(url, name);
        this.type = type;
    }

    @Override
    protected JSONObject[] generateSubscribeTickerRequest(String baseCurrency, String quoteCurrency, int reqId)
    {
        JSONObject[] req =
        {
            new JSONObject()
                .put("time", System.currentTimeMillis()/1000)
                .put("id", reqId)
                .put("channel", type + ".tickers")
                .put("event", "subscribe")
                .put("payload", new JSONArray().put(baseCurrency + "_" + quoteCurrency))
        };
        return req;
    }

    @Override
    protected JSONObject[] generateUnsubscribeTickerRequest(String baseCurrency, String quoteCurrency, int reqId)
    {
        JSONObject[] req =
        {
            new JSONObject()
                .put("time", System.currentTimeMillis()/1000)
                .put("id", reqId)
                .put("channel", type + ".tickers")
                .put("event", "unsubscribe")
                .put("payload", new JSONArray().put(baseCurrency + "_" + quoteCurrency))
        };
        return req;
    }

    @Override
    protected Message parse(String message) {
        return new GateMessage(message);
    }

    protected class GateMessage implements Message
    {
        protected JSONObject o;

        public GateMessage(String message) {
            o = new JSONObject(message);
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
                o.getString("event").equals("update");
        }

        @Override
        public Update getUpdate() {
            return new GateUpdate();
        }

        @Override
        public String toString() {
            return o.toString();
        }

        protected class GateResponce implements Responce
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

            @Override
            public String toString() {
                return o.toString();
            }
        }

        protected class GateUpdate implements Update
        {
            @Override
            public boolean isTickerUpdate() {
                return
                    o.has("event") && 
                    o.getString("event").equals("update") &&
                    o.getString("channel").contains(".tickers");
            }

            @Override
            public boolean isOrderbookUpdate() {
                return
                    o.has("event") && 
                    o.getString("event").equals("update") &&
                    o.getString("channel").contains(".book_ticker");
            }

            @Override
            public String getBaseCurrency() {
                return o.getJSONObject("result").getString("currency_pair").split("[_]")[0];
            }

            @Override
            public String getQuoteCurrency() {
                return o.getJSONObject("result").getString("currency_pair").split("[_]")[1];
            }

            @Override
            public Decimal get(Param param) throws InvalidFieldExeption, NotChangedExeption {
                if (isTickerUpdate()) {
                    try {
                        switch (param)
                        {
                            case Param.LAST_PRICE:
                                return new Decimal().parse(o.getJSONObject("result").getString("last"));
                            
                            case Param.ASK_PRICE:
                                return new Decimal().parse(o.getJSONObject("result").getString("lowest_ask"));
                            
                            case Param.BID_PRICE:
                                return new Decimal().parse(o.getJSONObject("result").getString("highest_bid"));
                        
                            default:
                                break;
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                throw new InvalidFieldExeption();
            }

            @Override
            public long getTimestamp() {
                return o.getLong("time_ms");
            }

            @Override
            public String toString() {
                return o.toString();
            }
        }
    }
}
