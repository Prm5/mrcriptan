package org.vasyaradulsoftware.mrcriptan;

import java.io.Closeable;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.vasyaradulsoftware.arbitragelib.TradingPair;

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

        while (running) {

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }

            EditMessageText edit = EditMessageText
                .builder()
                .chatId(message.getChatId())
                .messageId(message.getMessageId())
                .text(tradingPair.getSpreadInfo())
                .build();

            try {
                telegramClient.execute(edit);
            } catch (TelegramApiException e) {
                //e.printStackTrace();
            }
        }
    }

    public void close() {
        running = false;

        tradingPair.unfollow();
    }
}
