package org.vasyaradulsoftware.arbitragelib.exchange;

import java.net.URISyntaxException;
import java.text.ParseException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.vasyaradulsoftware.arbitragelib.Message;
import org.vasyaradulsoftware.arbitragelib.Ticker.Param;

import decimal.Decimal;

public class Okx extends WebSocketExchange
{

    protected Okx(String name) throws URISyntaxException
    {
        super("wss://ws.okx.com:8443/ws/v5/public", name);
    }

    @Override
    protected JSONObject[] generateSubscribeTickerRequest(String baseCurrency, String quoteCurrency, int reqId)
    {
        JSONObject[] req =
        {
            new JSONObject()
                .put("id", reqId)
                .put("op", "subscribe")
                .put("args", new JSONArray()
                    .put(new JSONObject()
                        .put("channel", "tickers")
                        .put("instId", baseCurrency + "-" + quoteCurrency)
                    )
                )
        };
        return req;
    }

    @Override
    protected JSONObject[] generateUnsubscribeTickerRequest(String baseCurrency, String quoteCurrency, int reqId)
    {
        JSONObject[] req =
        {
            new JSONObject()
                .put("id", reqId)
                .put("op", "unsubscribe")
                .put("args", new JSONArray()
                    .put(new JSONObject()
                        .put("channel", "tickers")
                        .put("instId", baseCurrency + "-" + quoteCurrency)
                    )
                )
        };
        return req;
    }

    @Override
    protected Message parse(String message) {
        return new OkxMessage(message);
    }

    protected class OkxMessage implements Message
    {
        private JSONObject o;

        public OkxMessage(String message) {
            o = new JSONObject(message);
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

        @Override
        public String toString() {
            return o.toString();
        }

        protected class OkxResponce implements Responce
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

            @Override
            public String toString() {
                return o.toString();
            }
        }

        protected class OkxUpdate implements Update
        {
            @Override
            public boolean isTickerUpdate() {
                return o.getJSONObject("arg").getString("channel").equals("tickers");
            }

            @Override
            public boolean isOrderbookUpdate() {
                return o.getJSONObject("arg").getString("channel").equals("books");
            }

            @Override
            public String getBaseCurrency() {
                JSONObject d = o.getJSONArray("data").getJSONObject(0);
                return d.getString("instId").split("[-]")[0];
            }

            @Override
            public String getQuoteCurrency() {
                JSONObject d = o.getJSONArray("data").getJSONObject(0);
                return d.getString("instId").split("[-]")[1];
            }

            @Override
            public Decimal get(Param param) throws InvalidFieldExeption, NotChangedExeption {
                JSONObject d = o.getJSONArray("data").getJSONObject(0);
                if (isTickerUpdate()) {
                    try {
                        switch (param)
                        {
                            case Param.LAST_PRICE:
                                return new Decimal().parse(d.getString("last"));
                            
                            case Param.ASK_PRICE:
                                return new Decimal().parse(d.getString("askPx"));
                            
                            case Param.BID_PRICE:
                                return new Decimal().parse(d.getString("askPx"));
                        
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
                JSONObject d = o.getJSONArray("data").getJSONObject(0);
                return d.getLong("ts");
            }

            @Override
            public String toString() {
                return o.toString();
            }
        }
    }
}
