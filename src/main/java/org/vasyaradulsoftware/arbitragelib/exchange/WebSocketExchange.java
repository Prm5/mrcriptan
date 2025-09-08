package org.vasyaradulsoftware.arbitragelib.exchange;

import java.math.BigInteger;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;
import org.vasyaradulsoftware.arbitragelib.Ticker;
import org.vasyaradulsoftware.arbitragelib.WebSocketCallbackInvoker;

import org.vasyaradulsoftware.arbitragelib.Request;

public abstract class WebSocketExchange implements Exchange {

    private WebSocketCallbackInvoker websocket;

    private String url;
    private String name;

    protected List<Ticker> subscribtions = new ArrayList<Ticker>();
    private Map<String, Request> requests = new HashMap<String, Request>();

    protected String regRequest(Request request) {
        String reqId = new BigInteger(64, new Random()).toString();
        requests.put(reqId, request);
        System.out.println("request " + reqId + " registred");
        return reqId;
    }

    protected Request completeRequest(String reqId) {
        return requests.remove(reqId);
    }

    protected boolean hasRequest(String reqId) {
        return requests.containsKey(reqId);
    }
    
    protected WebSocketExchange(String url, String name)
    {
        this.url = url;
        this.name = name;
        websocket = null;
    }

    private void updateWebSocketAndSend(String message) {
        if (websocket == null || websocket.isClosed() || websocket.isClosing())
        {
            websocket = null;

            try {
                websocket = new WebSocketCallbackInvoker(url, this);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        websocket.send(message);
    }

    @Override
    public Ticker subscribeTicker(String baseCurrency, String quoteCurrency)
    {
        try
        {
            return subscribtions
                .stream()
                .filter(t ->
                    t.getBaseCurrency().equals(baseCurrency) &&
                    t.getQuoteCurrency().equals(quoteCurrency)
                )
                .iterator()
                .next();
        } 
        catch (NoSuchElementException e)
        {
            Ticker ticker = new Ticker(baseCurrency, quoteCurrency, this);

            ticker.setSubscribingStatus();
            String reqId = regRequest(new Request(ticker));
            updateWebSocketAndSend(generateSubscribeTickerRequest(ticker, reqId).toString());

            return ticker;
        }
    }

    protected abstract JSONObject generateSubscribeTickerRequest(Ticker ticker, String reqId);

    @Override
    public void unsubscribeTicker(Ticker ticker) {
        if (subscribtions.contains(ticker)) {

            String reqId = regRequest(new Request(ticker));
            
            updateWebSocketAndSend(generateUnsubscribeTickerRequest(ticker, reqId).toString());
        }
    }

    protected abstract JSONObject generateUnsubscribeTickerRequest(Ticker ticker, String reqId);

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void accept(String message) {
        try {
            handleMessage(new JSONObject(message));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected abstract void handleMessage(JSONObject message);
}
