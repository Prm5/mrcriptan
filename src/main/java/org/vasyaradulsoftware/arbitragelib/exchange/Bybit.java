package org.vasyaradulsoftware.arbitragelib.exchange;

import java.net.URISyntaxException;
import java.text.ParseException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.vasyaradulsoftware.arbitragelib.Message;
import org.vasyaradulsoftware.arbitragelib.WebSocketExchange;
import org.vasyaradulsoftware.arbitragelib.Ticker.Param;

import decimal.Decimal;

public class Bybit extends WebSocketExchange  {

    protected Bybit(String url, String name) throws URISyntaxException {
        super(url, name);
    }

    @Override
    protected JSONObject[] generateSubscribeTickerRequest(String baseCurrency, String quoteCurrency, int reqId) {
        JSONObject[] req =
        {
            new JSONObject()
                .put("req_id", reqId)
                .put("op", "subscribe")
                .put("args", new JSONArray()
                    .put("tickers." + baseCurrency + quoteCurrency)
                ),
            
            new JSONObject()
                .put("req_id", reqId)
                .put("op", "subscribe")
                .put("args", new JSONArray()
                    .put("orderbook.50." + baseCurrency + quoteCurrency)
                ),
        };
        return req;
    }

    @Override
    protected JSONObject[] generateUnsubscribeTickerRequest(String baseCurrency, String quoteCurrency, int reqId) {
        JSONObject[] req =
        {
            new JSONObject()
                .put("req_id", reqId)
                .put("op", "unsubscribe")
                .put("args", new JSONArray()
                    .put("tickers." + baseCurrency + quoteCurrency)
                ),
            
            new JSONObject()
                .put("req_id", reqId)
                .put("op", "unsubscribe")
                .put("args", new JSONArray()
                    .put("orderbook.50." + baseCurrency + quoteCurrency)
                )
        };
        return req;
    }

    @Override
    protected Message parse(String message) {
        return new BybitMessage(message);
    }

    protected class BybitMessage implements Message
    {
        private static final String[] quotes = {"USDT", "USDC", "USD1", "USD", "EUR", "BTC", "MNT", "SOL", "ETH"};

        private JSONObject o;

        public BybitMessage(String message) {
            o = new JSONObject(message);
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

        @Override
        public String toString() {
            return o.toString();
        }

        protected class BybitResponce implements Responce
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

            @Override
            public String toString() {
                return o.toString();
            }
        }

        protected class BybitUpdate implements Update
        {
            @Override
            public boolean isTickerUpdate() {
                String[] topic = o.getString("topic").split("[.]");
                return topic[0].equals("tickers") && o.has("data");
            }

            @Override
            public boolean isOrderbookUpdate() {
                String[] topic = o.getString("topic").split("[.]");
                return topic[0].equals("orderbook") && o.has("data");
            }

            @Override
            public String getBaseCurrency() {
                String[] topic = o.getString("topic").split("[.]");
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
                String[] topic = o.getString("topic").split("[.]");
                String ticker = topic[topic.length - 1];
                for (String quote : quotes) {
                    if (ticker.substring(ticker.length() - quote.length(), ticker.length()).equals(quote)) {
                        return quote;
                    }
                }
                return null;
            }

            protected boolean isDelta() {
                return o.getString("type").equals("delta");
            }

            protected boolean isSnapshot() {
                return o.getString("type").equals("snapshot");
            }

            @Override
            public Decimal get(Param param) throws InvalidFieldExeption, NotChangedExeption {
                try {
                    if (isTickerUpdate()) {
                        switch (param)
                        {
                            case Param.LAST_PRICE:
                                if (o.getJSONObject("data").has("lastPrice")) {
                                    return new Decimal().parse(o.getJSONObject("data").getString("lastPrice"));
                                } else {
                                    throw new NotChangedExeption();
                                }
                        
                            default:
                                break;
                        }
                    } else if (isOrderbookUpdate()) {
                        switch (param)
                        {   
                            case Param.ASK_PRICE:
                                JSONArray a = o.getJSONObject("data").getJSONArray("a");
                                if (isSnapshot()) {
                                    return new Decimal().parse(a.getJSONArray(a.length() - 1).getString(0));
                                } else if (isDelta()) {
                                    int i = 0;
                                    while (i++ < a.length()) if (!a.getJSONArray(a.length() - i).getString(1).equals("0")) break;
                                    if (i == 1) {
                                        throw new NotChangedExeption();
                                    } else {
                                        return new Decimal().parse(a.getJSONArray(a.length() - i).getString(0));
                                    }
                                }
                                break;
                            
                            case Param.BID_PRICE:
                                JSONArray b = o.getJSONObject("data").getJSONArray("b");
                                if (isSnapshot()) {
                                    return new Decimal().parse(b.getJSONArray(b.length() - 1).getString(0));
                                } else if (isDelta()) {
                                    int i = 0;
                                    while (i++ < b.length()) if (!b.getJSONArray(b.length() - i).getString(1).equals("0")) break;
                                    if (i == 1) {
                                        throw new NotChangedExeption();
                                    } else {
                                        return new Decimal().parse(b.getJSONArray(b.length() - i).getString(0));
                                    }
                                }
                                break;
                        
                            default:
                                break;
                        }
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                throw new InvalidFieldExeption();
            }

            @Override
            public long getTimestamp() {
                return o.getLong("ts");
            }

            @Override
            public String toString() {
                return o.toString();
            }
        }
    }
}
