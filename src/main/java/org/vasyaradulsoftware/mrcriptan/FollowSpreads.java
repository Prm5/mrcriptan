package org.vasyaradulsoftware.mrcriptan;

import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.vasyaradulsoftware.arbitragelib.TradingPair;
import org.vasyaradulsoftware.arbitragelib.TradingPair.NoSubscribtionsExeption;

public class FollowSpreads extends Follow {

    private TradingPair tradingPair;

    public FollowSpreads(String baseCurrency, String quoteCurrency, long chatId, TelegramClient telegramClient) {
        super(chatId, telegramClient);
        tradingPair = TradingPair.follow(baseCurrency, quoteCurrency);
    }

    @Override
    protected void loop() {
        String text;
        try {
            text = tradingPair.getSpreadInfo();
        } catch (NoSubscribtionsExeption e) {
            text = "Ошибка: тикер не найден";
            if (isRunning()) close();
        }

        send(text);
    }

    @Override
    protected void onClose() {
        tradingPair.unfollow();
    }
}
