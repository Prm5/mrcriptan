package org.vasyaradulsoftware.arbitragelib.exchange;

import java.net.URISyntaxException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.vasyaradulsoftware.arbitragelib.Ticker;

public class OkxFutures extends Okx
{
    public static OkxFutures create()
    {
        try
        {
            return new OkxFutures();
        }
        catch (URISyntaxException e)
        {
            return null;
        }
    }

    protected OkxFutures() throws URISyntaxException
    {
        super("Okx(futures)");
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
                    //.put("instType", instType)
                    .put("instId", ticker.getBaseCurrency() + "-" + ticker.getQuoteCurrency() + "-SWAP")
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
                    //.put("instType", instType)
                    .put("instId", ticker.getBaseCurrency() + "-" + ticker.getQuoteCurrency() + "-SWAP")
                )
            );
    }

    @Override
    protected void handleData(JSONObject data)
    {
        for (int i = 0; i < data.getJSONArray("data").length(); i++)
        {
            JSONObject d = data.getJSONArray("data").getJSONObject(i);
            if (d.has("instId") && d.has("last") && d.has("instType") && d.get("instType").equals("SWAP")  && d.has("ts"))
            {
                subscribtions
                    .stream()
                    .forEach(t ->
                    {
                        if (d.getString("instId").equals(t.getBaseCurrency() + "-" + t.getQuoteCurrency() + "-SWAP")) 
                            t.update(d.getString("last"), d.getLong("ts"));
                    });
            }
        }
    }
}
