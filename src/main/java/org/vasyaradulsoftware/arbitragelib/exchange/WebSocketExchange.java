package org.vasyaradulsoftware.arbitragelib.exchange;

import java.math.BigInteger;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.function.Consumer;

import org.json.JSONException;
import org.json.JSONObject;
import org.vasyaradulsoftware.arbitragelib.Ticker;
import org.vasyaradulsoftware.arbitragelib.WebSocketCallbackInvoker;

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
    private Map<String, Request> requests = new HashMap<String, Request>();

    protected String regRequest(Request request) {
        String reqId = new BigInteger(64, new Random()).toString();
        requests.put(reqId, request);
        System.out.println("request " + reqId + " registred");
        return reqId;
    }

    /*
     * Метод для создания запроса на подписку на обновление цены актива в реальном времени.
     * Метод должен возвращать готовый для отправки запрос на подписку на обновление цены актива.
     * Запрос на подписку для каждой биржи специфичен. По этому реалицация этого метода должна быть своя для каждой биржи.
     * Ознакомьтесь с документацией API интересующей вас биржи для того чтобы понять как вы должны реализовывать этот метод.
     * Метод принимает ссылку на структуру данных Ticker, которая служит обёрткой для значения цены актива.
     * Этот класс впоследствии самостоятельно будет обновлять цену в переданном Ticker'е по мере поступления апдейтов от биржи, вам лишь требуется
     * сгенерировать запрос исходя исходя из названия базовой валюты (base currency) и валюты котировки (quote currency) которые хранятся в
     * структуре данных Ticker. Также метод принимает ID запроса (reqId) который должен содержаться в возвращаемом запросе.
     */
    protected abstract JSONObject generateSubscribeTickerRequest(Ticker ticker, String reqId);

    /*
     * Метод для создания запроса на отписку от обновлений цены. Всё тоже самое что и в предыдужем методе, только для отписки.
     */
    protected abstract JSONObject generateUnsubscribeTickerRequest(Ticker ticker, String reqId);

    /*
     * Этот метод вызывается всегда когда API биржи присылает сообщение по протоколу WebSocket.
     * Он должен обработать полученное сообщение, формат которого описан в API интересующей вас биржи и специфичен для каждой биржи.
     * Могут приходить как ответы на запросы, так и обновления по подпискам.
     * В случае ответа добавляйте тикер в список подписок или сообщайте тикеру что подписка не удалась.
     * В случае апдейта обновляйте цену нужного тикера.
     */
    protected abstract void handleMessage(JSONObject message);

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

    @Override
    public void unsubscribeTicker(Ticker ticker) {
        if (subscribtions.contains(ticker)) {

            String reqId = regRequest(new Request(ticker));
            
            updateWebSocketAndSend(generateUnsubscribeTickerRequest(ticker, reqId).toString());
        }
    }

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
}
