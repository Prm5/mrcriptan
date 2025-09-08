package org.vasyaradulsoftware.arbitragelib.exchange;

import org.vasyaradulsoftware.arbitragelib.Ticker;

/*
 * Интерфейс биржи.
 * Через классы, реализующие этот интерфейс происходит взаимодействие с API конкретной биржи.
 * Чтобы добавить поддержку новой биржи создайте реализацию этого интерфейса для этой биржи.
 * Абстрактный класс WebSocketExchange удобен бирж, API которых поддерживает WebSocket. Лучше используйте сразу его.
 * Пока что функционал довольно скудный, добавляйте новые методы по мере необходимости.
 */
public interface Exchange {

    public Ticker subscribeTicker(String baseCurrency, String quoteCurrency);
    public void unsubscribeTicker(Ticker ticker);

    public String getName();
}
