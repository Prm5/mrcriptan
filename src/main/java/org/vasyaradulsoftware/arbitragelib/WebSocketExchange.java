package org.vasyaradulsoftware.arbitragelib;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import org.json.JSONException;
import org.json.JSONObject;
import org.vasyaradulsoftware.arbitragelib.Message.InvalidFieldExeption;
import org.vasyaradulsoftware.arbitragelib.Message.NotChangedExeption;
import org.vasyaradulsoftware.arbitragelib.Message.Responce;
import org.vasyaradulsoftware.arbitragelib.Message.Update;
import org.vasyaradulsoftware.arbitragelib.Ticker.Param;
import org.vasyaradulsoftware.arbitragelib.exchange.Exchange;
/*
 * Этот класс позволяет использовать биржи с поддержкой WebSocket.
 * Реализуйте предложенные абстрактные методы для отправки WebSocket запросов и обработки ответов для API нужной вам биржи. 
 */
public abstract class WebSocketExchange implements Exchange, Consumer<String> {

    private WebSocketCallbackInvoker websocket;

    private String url;
    private String name;

    protected List<Ticker> subscribtions = new ArrayList<Ticker>();
    private Map<Integer, Request> requests = new HashMap<Integer, Request>();

    private int lastReqId = 0;

    /*
     * Метод для создания запроса на подписку на обновление цены актива в реальном времени.
     * Метод должен возвращать готовый для отправки запрос на подписку на обновление цены актива.
     * Запрос на подписку для каждой биржи специфичен. По этому реалицация этого метода должна быть своя для каждой биржи.
     * Ознакомьтесь с документацией API интересующей вас биржи для того чтобы понять как вы должны реализовывать этот метод.
     */
    protected abstract JSONObject[] generateSubscribeTickerRequest(String baseCurrency, String quoteCurrency, int reqId);

    /*
     * Метод для создания запроса на отписку от обновлений цены. Всё тоже самое что и в предыдужем методе, только для отписки.
     */
    protected abstract JSONObject[] generateUnsubscribeTickerRequest(String baseCurrency, String quoteCurrency, int reqId);

    /*
     * Метод вызывается при при получении сообщения от ВебСокета и должен возвращять инстанс класса, реалезующего интерфейс Message.
     * Интерфейс Message нужен для получения информации из сообений, полученных от API биржи.
     */
    protected abstract Message parse(String message);

    private void handleMessage(Message message)
    {
        //if (message.isResponce()) System.out.println("responce received from " + url + ": " + message.toString());

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

        Request req = completeRequest(message.getResponceId());
        if (message.isSubscribedSuccessful())
        {
            if (req.isCompleted()) {
                req.ticker.setSubscribedStatus();
                subscribtions.add(req.ticker);
            }
        }
        else if (message.isUnsubscribedSuccessful())
        {
            subscribtions.remove(req.ticker);
        }
        else if(message.isSubscribeError())
        {
            req.ticker.subscribingUnsuccessful();
            System.out.println("subscribe error: " + message);
        }
    }

    private void handleUpdate(Update update)
    {
        //System.out.println(update);
        subscribtions
            .stream()
            .forEach(t ->
            {
                if (update.getBaseCurrency().equals(t.getBaseCurrency()) && update.getQuoteCurrency().equals(t.getQuoteCurrency()))
                    for (Param p : Param.values()) {
                        try {
                            t.update(p, update.getTimestamp(), update.get(p));
                        } catch (NotChangedExeption e) {
                            t.update(p, update.getTimestamp());
                        } catch (InvalidFieldExeption e) {}
                    }
            });
    }

    private class Request {
        private Ticker ticker;
        private int confirmations;
        

        public Request(Ticker ticker)
        {
            this.ticker = ticker;
            this.confirmations = 0;
        }

        public void setConfirmations(int confirmations) {
            this.confirmations = confirmations;
        }

        public boolean confirm() {
            if (confirmations-- <= 0) return true;
            return false;
        }

        public boolean isCompleted() {
            if (confirmations <= 0) return true;
            return false;
        }

    }

    protected int regRequest(Request request) {
        int reqId = lastReqId++;
        requests.put(reqId, request);
        //System.out.println("request " + reqId + " registred");
        return reqId;
    }

    protected Request completeRequest(int reqId) {
        if (requests.get(reqId).confirm()) return requests.remove(reqId);
        return requests.get(reqId);
    }

    protected boolean hasRequest(int reqId) {
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
        System.out.println("sending message to " + url + ": " + message);
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

            Request req = new Request(ticker);
            JSONObject[] messages = generateSubscribeTickerRequest(ticker.getBaseCurrency(), ticker.getQuoteCurrency(), regRequest(req));
            req.setConfirmations(messages.length);

            for (JSONObject message : messages)
                updateWebSocketAndSend(message.toString());

            return ticker;
        }
    }

    @Override
    public void unsubscribeTicker(Ticker ticker) {
        if (subscribtions.contains(ticker)) {

            Request req = new Request(ticker);
            JSONObject[] messages = generateUnsubscribeTickerRequest(ticker.getBaseCurrency(), ticker.getQuoteCurrency(), regRequest(req));
            req.setConfirmations(messages.length);
            
            for (JSONObject message : messages)
                updateWebSocketAndSend(message.toString());
        }
    }

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
