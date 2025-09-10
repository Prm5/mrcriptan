package org.vasyaradulsoftware.arbitragelib.exchange;

import java.net.URISyntaxException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.vasyaradulsoftware.arbitragelib.Message;

public class Gate extends WebSocketExchange
{
    private String type;

    protected Gate(String url, String name, String type) throws URISyntaxException
    {
        super(url, name);
        this.type = type;
    }

    @Override
    protected JSONObject generateSubscribeTickerRequest(String baseCurrency, String quoteCurrency, int reqId)
    {
        return new JSONObject()
            .put("time", System.currentTimeMillis()/1000)
            .put("id", reqId)
            .put("channel", type + ".tickers")
            .put("event", "subscribe")
            .put("payload", new JSONArray().put(baseCurrency + "_" + quoteCurrency));
    }

    @Override
    protected JSONObject generateUnsubscribeTickerRequest(String baseCurrency, String quoteCurrency, int reqId)
    {
        return new JSONObject()
            .put("time", System.currentTimeMillis()/1000)
            .put("id", reqId)
            .put("channel", type + ".tickers")
            .put("event", "unsubscribe")
            .put("payload", new JSONArray().put(baseCurrency + "_" + quoteCurrency));
    }

    @Override
    protected Message parse(String message) {
        return new GateMessage(message);
    }

    protected class GateMessage extends JSONObject implements Message {

        public GateMessage(String message) {
            super(message);
        }
        
        @Override
        public boolean isResponce() {
            return
                this.has("event") && 
                (
                    this.getString("event").equals("subscribe") ||
                    this.getString("event").equals("unsubscribe")
                );
        }

        @Override
        public int getResponceId() {
            return this.getInt("id");
        }

        @Override
        public boolean isSubscribedSuccessfulResponce() {
            return 
                this.getString("event").equals("subscribe") &&
                this.getJSONObject("result").getString("status").equals("success");
        }

        @Override
        public boolean isUnsubscribedSuccessfulResponce() {
            return
                this.getString("event").equals("unsubscribe") &&
                this.getJSONObject("result").getString("status").equals("success");
        }

        @Override
        public boolean isSubscribeErrorResponce() {
            return 
                this.getString("event").equals("subscribe") &&
                this.getJSONObject("result").getString("status").equals("fail");
        }

        @Override
        public boolean isUpdate() {
            return
                this.has("event") && 
                this.getString("event").equals("update");
        }

        @Override
        public boolean isUpdateChannelTickers() {
            return
                this.has("event") && 
                this.getString("event").equals("update") &&
                this.getString("channel").contains(".tickers");
        }

        @Override
        public String getUpdateBaseCurrency() {
            return this.getJSONObject("result").getString("currency_pair").split("[_]")[0];
        }

        @Override
        public String getUpdateQuoteCurrency() {
            return this.getJSONObject("result").getString("currency_pair").split("[_]")[1];
        }

        @Override
        public String getUpdateLastPrice() {
            return this.getJSONObject("result").getString("last");
        }

        @Override
        public long getUpdateTimestamp() {
            return this.getLong("time_ms");
        }
    }
}
