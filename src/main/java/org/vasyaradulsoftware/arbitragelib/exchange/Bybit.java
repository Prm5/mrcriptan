package org.vasyaradulsoftware.arbitragelib.exchange;

import java.net.URISyntaxException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.vasyaradulsoftware.arbitragelib.Message;

public class Bybit extends WebSocketExchange  {

    protected Bybit(String url, String name) throws URISyntaxException {
        super(url, name);
    }

    @Override
    protected JSONObject generateSubscribeTickerRequest(String baseCurrency, String quoteCurrency, int reqId) {
        return new JSONObject()
            .put("req_id", reqId)
            .put("op", "subscribe")
            .put("args", new JSONArray()
                .put("tickers." + baseCurrency + quoteCurrency)
            );
    }

    @Override
    protected JSONObject generateUnsubscribeTickerRequest(String baseCurrency, String quoteCurrency, int reqId) {
        return new JSONObject()
            .put("req_id", reqId)
            .put("op", "unsubscribe")
            .put("args", new JSONArray()
                .put("tickers." + baseCurrency + quoteCurrency)
            );
    }

    @Override
    protected Message parse(String message) {
        return new BybitMessage(message);
    }

    protected class BybitMessage extends JSONObject implements Message
    {
        private static final String[] quotes = {"USDT", "USDC", "USD1", "USD", "EUR", "BTC", "MNT", "SOL", "ETH"};

        public BybitMessage(String message) {
            super(message);
        }
        
        @Override
        public boolean isResponce() {
            return this.has("req_id") && this.has("op");
        }

        @Override
        public int getResponceId() {
            return this.getInt("req_id");
        }

        @Override
        public boolean isSubscribedSuccessfulResponce() {
            return this.getString("op").equals("subscribe") && this.has("success") && this.getBoolean("success");
        }

        @Override
        public boolean isUnsubscribedSuccessfulResponce() {
            return this.getString("op").equals("unsubscribe") && this.has("success") && this.getBoolean("success");
        }

        @Override
        public boolean isSubscribeErrorResponce() {
            return this.getString("op").equals("subscribe") && this.has("success") && !this.getBoolean("success");
        }

        @Override
        public boolean isUpdate() {
            return this.has("topic");
        }

        @Override
        public boolean isUpdateChannelTickers() {
            String[] topic = this.getString("topic").split("[.]");
            return
                topic[0].equals("tickers") &&
                this.has("data") &&
                this.getJSONObject("data").has("lastPrice") &&
                this.has("ts");
        }

        @Override
        public String getUpdateBaseCurrency() {
            String ticker = this.getString("topic").split("[.]")[1];
            for (String quote : quotes) {
                if (ticker.substring(ticker.length() - quote.length(), ticker.length()).equals(quote)) {
                    return ticker.substring(0, ticker.length() - quote.length());
                }
            }
            return null;
        }

        @Override
        public String getUpdateQuoteCurrency() {
            String ticker = this.getString("topic").split("[.]")[1];
            for (String quote : quotes) {
                if (ticker.substring(ticker.length() - quote.length(), ticker.length()).equals(quote)) {
                    return quote;
                }
            }
            return null;
        }

        @Override
        public String getUpdateLastPrice() {
            return this.getJSONObject("data").getString("lastPrice");
        }

        @Override
        public long getUpdateTimestamp() {
            return this.getLong("ts");
        }
    }
}
