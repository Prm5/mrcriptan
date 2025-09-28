package org.vasyaradulsoftware.mrcriptan;

import java.util.NoSuchElementException;

import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.vasyaradulsoftware.arbitragelib.Exchange;
import org.vasyaradulsoftware.arbitragelib.Orderbook;

public class FollowOrderbook extends Follow
{
    Orderbook ob;

    public FollowOrderbook(String baseCurrency, String quoteCurrency, String exchange, long chatId, TelegramClient telegramClient) {
        super(chatId, telegramClient);

        try {
            ob = Exchange.publicExchanges
                .stream()
                .filter(e -> e.getName()
                .equals(exchange))
                .findFirst()
                .get()
                .getOrderbook(baseCurrency, quoteCurrency);
        } catch (NoSuchElementException e) {
            ob = null;
        }
    }

    @Override
    protected void loop() {
        if (ob == null) {
            send("Ошибка: биржа не найдена.");
            close();
        }

        send(ob.getTable());
    }

    @Override
    protected void onClose() {
        if (ob != null) ob.unfollow();
    }
}
