package org.vasyaradulsoftware.arbitragelib.exchange;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.vasyaradulsoftware.arbitragelib.Message;
import org.vasyaradulsoftware.arbitragelib.Orderbook;
import org.vasyaradulsoftware.arbitragelib.Subscribtion.Channel;
import org.vasyaradulsoftware.arbitragelib.Ticker;
import org.vasyaradulsoftware.arbitragelib.WebSocketExchange;

import decimal.Decimal;

public class GateFutures extends Gate
{
    private GateFutures() throws URISyntaxException
    {
        super("wss://fx-ws.gateio.ws/v4/ws/usdt", "Gate(futures)", ExType.FUTURES);
    }

    public static GateFutures create() {
        try {
            return new GateFutures();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @Override
    protected Ticker createTicker(String baseCurrency, String quoteCurrency) {
        return new GateFuturesTicker(baseCurrency, quoteCurrency, this);
    }

    protected class GateFuturesTicker extends GateTicker
    {
        private GateFuturesTicker(String baseCurrency, String quoteCurrency, WebSocketExchange exchange) {
            super(baseCurrency, quoteCurrency);
        }

        @Override
        public void update(JSONObject msg) {
            try {
                update(Param.LAST_PRICE, msg.getLong("time_ms"), new Decimal().parse(msg.getJSONArray("result").getJSONObject(0).getString("last")));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected Orderbook createOrderbook(String baseCurrency, String quoteCurrency) {
        return new GateFuturesOrderbook(baseCurrency, quoteCurrency);
    }

    protected class GateFuturesOrderbook extends GateOrderbook
    {
        Decimal quantoMul;

        private GateFuturesOrderbook(String baseCurrency, String quoteCurrency) {
            super(baseCurrency, quoteCurrency);

            try {
                JSONObject responce = new JSONObject(HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create("https://api.gateio.ws/api/v4/futures/usdt/contracts/" + baseCurrency + "_" + quoteCurrency))
                        .build(),
                    HttpResponse.BodyHandlers.ofString()
                ).body());

                quantoMul = new Decimal().parse(responce.getString("quanto_multiplier"));

            } catch (InterruptedException e) {
                e.printStackTrace();
                this.close();
            } catch (IOException e) {
                e.printStackTrace();
                this.close();
            } catch (ParseException e) {
                e.printStackTrace();
                this.close();
            }
        }

        @Override
        public void update(JSONObject msg)
        {
            List<Row> ask = new ArrayList<Row>();
            JSONArray askJSON = msg.getJSONObject("result").getJSONArray("asks");

            for (int i = 0; i < askJSON.length(); i++) {
                try {
                    Decimal price = new Decimal().parse(askJSON.getJSONObject(i).getString("p"));
                    Decimal sizeBase = new Decimal().fromLong(askJSON.getJSONObject(i).getLong("s")).mulRD(quantoMul);
                    Decimal sizeQuote = sizeBase.clone().mulRD(price);
                    ask.add(new Row(price, sizeQuote, sizeBase));
                }
                catch (ParseException e) {}
            }

            List<Row> bid = new ArrayList<Row>();
            JSONArray bidJSON = msg.getJSONObject("result").getJSONArray("bids");

            for (int i = 0; i < bidJSON.length(); i++) {
                try {
                    Decimal price = new Decimal().parse(bidJSON.getJSONObject(i).getString("p"));
                    Decimal sizeBase = new Decimal().fromLong(bidJSON.getJSONObject(i).getLong("s")).mulRD(quantoMul);
                    Decimal sizeQuote = sizeBase.clone().mulRD(price);
                    bid.add(new Row(price, sizeQuote, sizeBase));
                }
                catch (ParseException e) {}
            }

            if (msg.getString("event").equals("all"))
                update(UpdType.SNAPSHOT, msg.getJSONObject("result").getLong("t"), ask, bid);
            else if (msg.getString("event").equals("update"))
                update(UpdType.DELTA, msg.getJSONObject("result").getLong("t"), ask, bid);
        }
    }

    @Override
    protected Message parse(String message) {
        return new GateFuturesMessage(message);
    }

    protected class GateFuturesMessage extends GateMessage
    {
        protected GateFuturesMessage(String message) {
            super(message);
        }
        
        @Override
        public Update getUpdate() {
            return new GateFuturesUpdate();
        }

        protected class GateFuturesUpdate extends GateUpdate
        {
            @Override
            public String getBaseCurrency() {
                switch (getChannel()) {
                    case Channel.TICKER:
                        return o.getJSONArray("result").getJSONObject(0).getString("contract").split("[_]")[0];

                    case Channel.ORDERBOOK:
                        return o.getJSONObject("result").getString("contract").split("[_]")[0];
                
                    default:
                        return null;
                }
            }

            @Override
            public String getQuoteCurrency() {
                switch (getChannel()) {
                    case Channel.TICKER:
                        return o.getJSONArray("result").getJSONObject(0).getString("contract").split("[_]")[1];

                    case Channel.ORDERBOOK:
                        return o.getJSONObject("result").getString("contract").split("[_]")[1];
                
                    default:
                        return null;
                }
            }
        }
    }
}
