package org.vasyaradulsoftware.arbitragelib.exchange;

import java.net.URISyntaxException;
import java.text.ParseException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.vasyaradulsoftware.arbitragelib.Message;
import org.vasyaradulsoftware.arbitragelib.Ticker.Param;

import decimal.Decimal;

public class GateFutures extends Gate
{
    private GateFutures() throws URISyntaxException
    {
        super("wss://fx-ws.gateio.ws/v4/ws/usdt", "Gate(futures)", "futures");
    }

    public static GateFutures create() {
        try {
            return new GateFutures();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @Override
    protected JSONObject[] generateSubscribeTickerRequest(String baseCurrency, String quoteCurrency, int reqId) {
        JSONObject[] req = {
            super.generateSubscribeTickerRequest(baseCurrency, quoteCurrency, reqId)[0],
            new JSONObject()
                .put("time", System.currentTimeMillis()/1000)
                .put("id", reqId)
                .put("channel", type + ".book_ticker")
                .put("event", "subscribe")
                .put("payload", new JSONArray().put(baseCurrency + "_" + quoteCurrency))
        };
        return req;
    }

    @Override
    protected JSONObject[] generateUnsubscribeTickerRequest(String baseCurrency, String quoteCurrency, int reqId) {
        JSONObject[] req = {
            super.generateUnsubscribeTickerRequest(baseCurrency, quoteCurrency, reqId)[0],
            new JSONObject()
                .put("time", System.currentTimeMillis()/1000)
                .put("id", reqId)
                .put("channel", type + ".book_ticker")
                .put("event", "unsubscribe")
                .put("payload", new JSONArray().put(baseCurrency + "_" + quoteCurrency))
        };
        return req;
    }

    @Override
    protected Message parse(String message) {
        return new GateFuturesMessage(message);
    }

    protected class GateFuturesMessage extends GateMessage {

        public GateFuturesMessage(String message) {
            super(message);
        }

        @Override
        public Update getUpdate() {
            return new GateFuturesUpdate();
        }

        public class GateFuturesUpdate extends GateUpdate
        {
            @Override
            public String getBaseCurrency() {
                if (isOrderbookUpdate()) return o.getJSONObject("result").getString("s").split("[_]")[0];
                return o.getJSONArray("result").getJSONObject(0).getString("contract").split("[_]")[0];
            }

            @Override
            public String getQuoteCurrency() {
                if (isOrderbookUpdate()) return o.getJSONObject("result").getString("s").split("[_]")[1];
                return o.getJSONArray("result").getJSONObject(0).getString("contract").split("[_]")[1];
            }

            @Override
            public Decimal get(Param param) throws InvalidFieldExeption, NotChangedExeption {
                if (isTickerUpdate()) {
                    try {
                        switch (param)
                        {
                            case Param.LAST_PRICE:
                                return new Decimal().parse(o.getJSONArray("result").getJSONObject(0).getString("last"));
                        
                            default:
                                break;
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                } else if (isOrderbookUpdate()) {
                    try {
                        switch (param)
                        {   
                            case Param.ASK_PRICE:
                                return new Decimal().parse(o.getJSONObject("result").getString("a"));
                            
                            case Param.BID_PRICE:
                                return new Decimal().parse(o.getJSONObject("result").getString("b"));
                        
                            default:
                                break;
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                throw new InvalidFieldExeption();
            }
        }
    }
}
