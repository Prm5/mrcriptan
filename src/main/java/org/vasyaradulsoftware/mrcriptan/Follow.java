package org.vasyaradulsoftware.mrcriptan;

import java.io.Closeable;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.vasyaradulsoftware.arbitragelib.TradingPair;
import org.vasyaradulsoftware.arbitragelib.TradingPair.NoSubscribtionsExeption;

public class Follow implements Runnable, Closeable {

    private TelegramClient telegramClient;
    private Message message;
    private long chatId;
    private TradingPair tradingPair;
    private String baseCurrency;
    private String quoteCurrency;
    private volatile boolean running;

    public Follow(String baseCurrency, String quoteCurrency, long chatId, TelegramClient telegramClient) {
        this.chatId = chatId;
        this.telegramClient = telegramClient;
        this.baseCurrency = baseCurrency;
        this.quoteCurrency = quoteCurrency;
        running = true;

        new Thread(this).start();
    }

    @Override
    public void run() {

        SendMessage send = SendMessage
            .builder()
            .chatId(chatId)
            .text("ща")
            .build();

        try {
            message = telegramClient.execute(send);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return;
        }

        tradingPair = TradingPair.follow(baseCurrency, quoteCurrency);

        boolean edited = false;

        while (running) {

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }

            String text;
            try {
                text = tradingPair.getSpreadInfo();
            } catch (NoSubscribtionsExeption e) {
                text = "Ошибка: тикер не найден";
                if (running) close();
                if (edited) break;
            }

            EditMessageText edit = EditMessageText
                .builder()
                .chatId(message.getChatId())
                .messageId(message.getMessageId())
                .text(text)
                .build();

            try {
                telegramClient.execute(edit);
                edited = true;
            } catch (TelegramApiException e) {
                //e.printStackTrace();
            }
        }
    }

    @Override
    public void close() {
        running = false;

        tradingPair.unfollow();
    }
}
