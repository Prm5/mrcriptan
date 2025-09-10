package org.vasyaradulsoftware.arbitragelib.exchange;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import org.json.JSONException;
import org.json.JSONObject;
import org.vasyaradulsoftware.arbitragelib.Ticker;
import org.vasyaradulsoftware.arbitragelib.WebSocketCallbackInvoker;
import org.vasyaradulsoftware.arbitragelib.Message;
import org.vasyaradulsoftware.arbitragelib.Request;
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

    protected int regRequest(Request request) {
        int reqId = lastReqId++;
        requests.put(reqId, request);
        System.out.println("request " + reqId + " registred");
        return reqId;
    }

    /*
     * Метод для создания запроса на подписку на обновление цены актива в реальном времени.
     * Метод должен возвращать готовый для отправки запрос на подписку на обновление цены актива.
     * Запрос на подписку для каждой биржи специфичен. По этому реалицация этого метода должна быть своя для каждой биржи.
     * Ознакомьтесь с документацией API интересующей вас биржи для того чтобы понять как вы должны реализовывать этот метод.
     */
    protected abstract JSONObject generateSubscribeTickerRequest(String baseCurrency, String quoteCurrency, int reqId);

    /*
     * Метод для создания запроса на отписку от обновлений цены. Всё тоже самое что и в предыдужем методе, только для отписки.
     */
    protected abstract JSONObject generateUnsubscribeTickerRequest(String baseCurrency, String quoteCurrency, int reqId);

    /*
     * Метод вызывается при при получении сообщения от ВебСокета и должен возвращять инстанс класса, реалезующего интерфейс Message.
     * Интерфейс Message нужен для получения информации из сообений, полученных от API биржи.
     */
    protected abstract Message parse(String message);

    private void handleMessage(Message message)
    {
        if (message.isResponce()) System.out.println("responce received from " + url + ": " + message.toString());

        if (message.isResponce() && hasRequest(message.getResponceId()))
        {
            handleResponse(message);
        }
        else if (message.isUpdate())
        {
            handleUpdate(message);
        }
        else {
            System.out.println("message not recognised");
        }
    }

    private void handleResponse(Message message)
    {
        Ticker ticker = completeRequest(message.getResponceId()).getTicker();
        if (message.isSubscribedSuccessfulResponce())
        {
            ticker.setSubscribedStatus();
            subscribtions.add(ticker);
        }
        else if (message.isUnsubscribedSuccessfulResponce())
        {
            subscribtions.remove(ticker);
        }
        else if(message.isSubscribeErrorResponce())
        {
            ticker.subscribingUnsuccessful();
            System.out.println(message);
        }
    }

    private void handleUpdate(Message message)
    {
        if (message.isUpdateChannelTickers())
        {
            subscribtions
                .stream()
                .forEach(t ->
                {
                    if (message.getUpdateBaseCurrency().equals(t.getBaseCurrency()) && message.getUpdateQuoteCurrency().equals(t.getQuoteCurrency()))
                        t.update(message.getUpdateLastPrice(), message.getUpdateTimestamp());
                });
        }
    }

    protected Request completeRequest(int reqId) {
        return requests.remove(reqId);
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
            int reqId = regRequest(new Request(ticker));
            updateWebSocketAndSend(generateSubscribeTickerRequest(ticker.getBaseCurrency(), ticker.getQuoteCurrency(), reqId).toString());

            return ticker;
        }
    }

    @Override
    public void unsubscribeTicker(Ticker ticker) {
        if (subscribtions.contains(ticker)) {

            int reqId = regRequest(new Request(ticker));
            
            updateWebSocketAndSend(generateUnsubscribeTickerRequest(ticker.getBaseCurrency(), ticker.getQuoteCurrency(), reqId).toString());
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
