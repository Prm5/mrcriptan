package org.vasyaradulsoftware.arbitragelib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import org.json.JSONException;
import org.vasyaradulsoftware.arbitragelib.Message.Responce;
import org.vasyaradulsoftware.arbitragelib.Message.Update;

public abstract class WebSocketExchange implements Exchange, Consumer<String> {

    private WebSocketCallbackInvoker websocket;

    private String url;
    protected String name;
    protected ExType type;

    protected List<Subscribtion> subsctibtions = new ArrayList<Subscribtion>();
    protected List<Ticker> tickers = new ArrayList<Ticker>();
    protected List<Orderbook> orderbooks = new ArrayList<Orderbook>();
    
    private Map<Integer, Subscribtion> requests = new HashMap<Integer, Subscribtion>();

    private int lastReqId = 0;

    protected abstract Message parse(String message);

    private void handleMessage(Message message)
    {
        //System.out.println(message);
        if (message.isResponce()) System.out.println("responce received from " + url + ": " + message.toString());

        if (message.isResponce())
        {
            handleResponse(message.getResponce());
        }
        else if (message.isUpdate())
        {
            handleUpdate(message.getUpdate());
        }
        else {
            System.out.println("message from " + url + " not recognised: " + message.toString());
        }
    }

    private void handleResponse(Responce message)
    {
        //System.out.println(message);
        if (!hasRequest(message.getResponceId())) return;

        Subscribtion req = completeRequest(message.getResponceId());
        if (message.isSubscribedSuccessful())
        {
            req.onSubscribingSuccessful();
            subsctibtions.add(req);
        }
        else if (message.isUnsubscribedSuccessful())
        {
            subsctibtions.remove(req);
            tickers.remove(req);
            orderbooks.remove(req);
        }
        else if(message.isSubscribeError())
        {
            if (!req.isSubscribed()) {
                req.onSubscribingFailed();
                tickers.remove(req);
                orderbooks.remove(req);
            }
            System.out.println("subscribe error: " + message);
        }
    }

    private void handleUpdate(Update update)
    {
        //System.out.println(update);
        subsctibtions
            .stream()
            .forEach(s -> {
                if (
                    update.getBaseCurrency().equals(s.baseCurrency) &&
                    update.getQuoteCurrency().equals(s.quoteCurrency) &&
                    update.getChannel().equals(s.channel)
                )
                    s.update(update.getJSONObject());
            });
    }

    protected int regRequest(Subscribtion request) {
        int reqId = lastReqId++;
        requests.put(reqId, request);
        //System.out.println("request " + reqId + " registred");
        return reqId;
    }

    protected Subscribtion completeRequest(int reqId) {
        return requests.remove(reqId);
    }

    protected boolean hasRequest(int reqId) {
        return requests.containsKey(reqId);
    }
    
    protected WebSocketExchange(String url, String name, ExType type)
    {
        this.url = url;
        this.name = name;
        this.type = type;
        websocket = null;
    }

    protected void send(String message) {
        if (websocket == null || websocket.isClosed() || websocket.isClosing())
        {
            websocket = null;
            websocket = new WebSocketCallbackInvoker(url, this);
        }
        System.out.println("sending message to " + url + ": " + message);
        websocket.send(message);
    }

    @Override
    public Ticker getTicker(String baseCurrency, String quoteCurrency)
    {
        try
        {
            Ticker ticker = tickers
                .stream()
                .filter(t ->
                    t.baseCurrency.equals(baseCurrency) &&
                    t.quoteCurrency.equals(quoteCurrency) &&
                    !t.isClosed()
                )
                .iterator()
                .next();
            ticker.follow();
            return ticker;
        } 
        catch (NoSuchElementException e)
        {
            Ticker ticker = createTicker(baseCurrency, quoteCurrency);
            tickers.add(ticker);
            return ticker;
        }
    }

    protected abstract Ticker createTicker(String baseCurrency, String quoteCurrency);

    @Override
    public Orderbook getOrderbook(String baseCurrency, String quoteCurrency) {
        try
        {
            Orderbook orderbook = orderbooks
                .stream()
                .filter(t ->
                    t.baseCurrency.equals(baseCurrency) &&
                    t.quoteCurrency.equals(quoteCurrency) &&
                    !t.isClosed()
                )
                .iterator()
                .next();
            orderbook.follow();
            return orderbook;
        } 
        catch (NoSuchElementException e)
        {
            Orderbook orderbook = createOrderbook(baseCurrency, quoteCurrency);
            orderbooks.add(orderbook);
            return orderbook;
        }
    }

    protected abstract Orderbook createOrderbook(String baseCurrency, String quoteCurrency);

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void accept(String message) {
        try {
            handleMessage(parse(message));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
