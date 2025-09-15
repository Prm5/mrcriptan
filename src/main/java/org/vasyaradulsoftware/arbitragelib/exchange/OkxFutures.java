package org.vasyaradulsoftware.arbitragelib.exchange;

import java.net.URISyntaxException;

import org.json.JSONArray;
import org.json.JSONObject;

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
                        //.put("instType", instType)
                        .put("instId", baseCurrency + "-" + quoteCurrency + "-SWAP")
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
                        .put("instId", baseCurrency + "-" + quoteCurrency + "-SWAP")
                    )
                )
        };
        return req;
    }
}
