package org.vasyaradulsoftware.arbitragelib.exchange;

import java.net.URISyntaxException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.vasyaradulsoftware.arbitragelib.Message;

public class Okx extends WebSocketExchange
{

    protected Okx(String name) throws URISyntaxException
    {
        super("wss://ws.okx.com:8443/ws/v5/public", name);
    }

    @Override
    protected JSONObject generateSubscribeTickerRequest(String baseCurrency, String quoteCurrency, String reqId)
    {
        return new JSONObject()
            .put("id", reqId)
            .put("op", "subscribe")
            .put("args", new JSONArray()
                .put(new JSONObject()
                    .put("channel", "tickers")
                    .put("instId", baseCurrency + "-" + quoteCurrency)
                )
            );
    }

    @Override
    protected JSONObject generateUnsubscribeTickerRequest(String baseCurrency, String quoteCurrency, String reqId)
    {
        return new JSONObject()
            .put("id", reqId)
            .put("op", "unsubscribe")
            .put("args", new JSONArray()
                .put(new JSONObject()
                    .put("channel", "tickers")
                    .put("instId", baseCurrency + "-" + quoteCurrency)
                )
            );
    }

    @Override
    protected Message parse(String message) {
        return new OkxMessage(message);
    }

    protected class OkxMessage extends JSONObject implements Message {

        public OkxMessage(String message) {
            super(message);
        }
        
        @Override
        public boolean isResponce() {
            return this.has("id") && this.has("event");
        }

        @Override
        public String getResponceId() {
            return this.getString("id");
        }

        @Override
        public boolean isSubscribedSuccessfulResponce() {
            return this.getString("event").equals("subscribe");
        }

        @Override
        public boolean isUnsubscribedSuccessfulResponce() {
            return this.getString("event").equals("unsubscribe");
        }

        @Override
        public boolean isSubscribeErrorResponce() {
            return this.getString("event").equals("error");
        }

        @Override
        public boolean isUpdate() {
            return this.has("arg") && this.has("data");
        }

        @Override
        public boolean isUpdateChannelTickers() {
            JSONObject d = this.getJSONArray("data").getJSONObject(0);
            return d.has("instId") && d.has("last") && d.has("instType") && d.get("instType").equals("SPOT") && d.has("ts");
        }

        @Override
        public String getUpdateBaseCurrency() {
            JSONObject d = this.getJSONArray("data").getJSONObject(0);
            return d.getString("instId").split("[-]")[0];
        }

        @Override
        public String getUpdateQuoteCurrency() {
            JSONObject d = this.getJSONArray("data").getJSONObject(0);
            return d.getString("instId").split("[-]")[1];
        }

        @Override
        public String getUpdateLastPrice() {
            JSONObject d = this.getJSONArray("data").getJSONObject(0);
            return d.getString("last");
        }

        @Override
        public long getUpdateTimestamp() {
            JSONObject d = this.getJSONArray("data").getJSONObject(0);
            return d.getLong("ts");
        }
    }
}
