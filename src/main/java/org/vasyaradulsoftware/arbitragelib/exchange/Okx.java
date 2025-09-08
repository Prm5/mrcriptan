package org.vasyaradulsoftware.arbitragelib.exchange;

import java.net.URISyntaxException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.vasyaradulsoftware.arbitragelib.Ticker;

public class Okx extends WebSocketExchange
{

    protected Okx(String name) throws URISyntaxException
    {
        super("wss://ws.okx.com:8443/ws/v5/public", name);
    }

    @Override
    protected JSONObject generateSubscribeTickerRequest(Ticker ticker, String reqId)
    {
        return new JSONObject()
            .put("id", reqId)
            .put("op", "subscribe")
            .put("args", new JSONArray()
                .put(new JSONObject()
                    .put("channel", "tickers")
                    .put("instId", ticker.getBaseCurrency() + "-" + ticker.getQuoteCurrency())
                )
            );
    }

    @Override
    protected JSONObject generateUnsubscribeTickerRequest(Ticker ticker, String reqId)
    {
        return new JSONObject()
            .put("id", reqId)
            .put("op", "unsubscribe")
            .put("args", new JSONArray()
                .put(new JSONObject()
                    .put("channel", "tickers")
                    .put("instId", ticker.getBaseCurrency() + "-" + ticker.getQuoteCurrency())
                )
            );
    }

    @Override
    protected void handleMessage(JSONObject message)
    {
        if (message.has("id") && hasRequest(message.getString("id")) && message.has("event"))
        {
            handleResponse(message);
        }
        else if (message.has("arg") && message.has("data"))
        {
            handleData(message);
        }
        else
        {
            System.out.println(message);
        }
    }

    protected void handleResponse(JSONObject responce)
    {
        Ticker ticker = completeRequest(responce.getString("id")).getTicker();
        if (responce.getString("event").equals("subscribe"))
        {
            ticker.setSubscribedStatus();
            subscribtions.add(ticker);
        }
        else if (responce.getString("event").equals("unsubscribe"))
        {
            subscribtions.remove(ticker);
        }
        else if(responce.getString("event").equals("error"))
        {
            ticker.subscribingUnsuccessful();
            System.out.println(responce);
        }
    }

    protected void handleData(JSONObject data)
    {
        for (int i = 0; i < data.getJSONArray("data").length(); i++)
        {
            JSONObject d = data.getJSONArray("data").getJSONObject(i);
            if (d.has("instId") && d.has("last") && d.has("instType") && d.get("instType").equals("SPOT") && d.has("ts"))
            {
                subscribtions
                    .stream()
                    .forEach(t ->
                    {
                        if (d.getString("instId").equals(t.getBaseCurrency() + "-" + t.getQuoteCurrency())) 
                            t.update(d.getString("last"), d.getLong("ts"));
                    });
            }
        }
    }
}
