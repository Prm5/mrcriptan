package org.vasyaradulsoftware.arbitragelib.exchange;

import java.net.URISyntaxException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.vasyaradulsoftware.arbitragelib.Ticker;

public class Bybit extends WebSocketExchange  {

    protected Bybit(String url, String name) throws URISyntaxException {
        super(url, name);
    }

    @Override
    protected JSONObject generateSubscribeTickerRequest(Ticker ticker, String reqId) {
        return new JSONObject()
            .put("req_id", reqId)
            .put("op", "subscribe")
            .put("args", new JSONArray()
                .put("tickers." + ticker.getBaseCurrency() + ticker.getQuoteCurrency())
            );
    }

    @Override
    protected JSONObject generateUnsubscribeTickerRequest(Ticker ticker, String reqId) {
        return new JSONObject()
            .put("req_id", reqId)
            .put("op", "unsubscribe")
            .put("args", new JSONArray()
                .put("tickers." + ticker.getBaseCurrency() + ticker.getQuoteCurrency())
            );
    }

	@Override
	protected void handleMessage(JSONObject message) {
        
        if (message.has("req_id") && hasRequest(message.getString("req_id")) && message.has("op"))
        {
            Ticker ticker = completeRequest(message.getString("req_id")).getTicker();

            if (message.getString("op").equals("subscribe"))
            {
                if (message.has("success") && message.getBoolean("success"))
                {
                    ticker.setSubscribedStatus();
                    subscribtions.add(ticker);
                }
                else
                {
                    ticker.subscribingUnsuccessful();
                    System.out.println(message);
                }
            }
            else if (message.getString("op").equals("unsubscribe"))
            {
                if (message.has("success") && message.getBoolean("success"))
                {
                    subscribtions.remove(ticker);
                }
                else
                {
                    System.out.println(message);
                }
            }
        }
        else if (message.has("topic"))
        {
            String[] topic = message.getString("topic").split("[.]");
            if (
                topic[0].equals("tickers") &&
                message.has("data") &&
                message.getJSONObject("data").has("lastPrice") &&
                message.has("ts"))
            {
                subscribtions
                    .stream()
                    .forEach(t ->
                    {
                        if (topic[1].equals(t.getBaseCurrency() + t.getQuoteCurrency()))
                        {
                            t.update(message.getJSONObject("data").getString("lastPrice"), message.getLong("ts"));
                        }
                    });
            }
        }
	}
}
