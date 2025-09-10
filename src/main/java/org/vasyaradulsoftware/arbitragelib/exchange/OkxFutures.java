package org.vasyaradulsoftware.arbitragelib.exchange;

import java.net.URISyntaxException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.vasyaradulsoftware.arbitragelib.Message;

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
    protected JSONObject generateSubscribeTickerRequest(String baseCurrency, String quoteCurrency, int reqId)
    {
        return new JSONObject()
            .put("id", reqId)
            .put("op", "subscribe")
            .put("args", new JSONArray()
                .put(new JSONObject()
                    .put("channel", "tickers")
                    //.put("instType", instType)
                    .put("instId", baseCurrency + "-" + quoteCurrency + "-SWAP")
                )
            );
    }

    @Override
    protected JSONObject generateUnsubscribeTickerRequest(String baseCurrency, String quoteCurrency, int reqId)
    {
        return new JSONObject()
            .put("id", reqId)
            .put("op", "unsubscribe")
            .put("args", new JSONArray()
                .put(new JSONObject()
                    .put("channel", "tickers")
                    .put("instId", baseCurrency + "-" + quoteCurrency + "-SWAP")
                )
            );
    }

    @Override
    protected Message parse(String message) {
        return new OkxFuturesMessage(message);
    }

    protected class OkxFuturesMessage extends OkxMessage
    {
        public OkxFuturesMessage(String message) {
            super(message);
        }

        @Override
        public boolean isUpdateChannelTickers() {
            JSONObject d = this.getJSONArray("data").getJSONObject(0);
            return d.has("instId") && d.has("last") && d.has("instType") && d.get("instType").equals("SWAP") && d.has("ts");
        }
    }
}
