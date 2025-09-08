package org.vasyaradulsoftware.mrcriptan;

import java.util.ArrayList;
import java.util.List;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public class Bot implements LongPollingSingleThreadUpdateConsumer, Runnable {

    private TelegramClient telegramClient;
    private String botToken;
    private List<Chat> chats = new ArrayList<Chat>();

    public Bot(String botToken) {
        this.botToken = botToken;
    }

    @Override
    public void run() {
        this.telegramClient = new OkHttpTelegramClient(botToken);

        try (TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication()) {
            botsApplication.registerBot(botToken, this);
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void consume(Update update) {
    
        if (update.hasMessage()) {

            if (!chats.stream().anyMatch(chat -> chat.getId() == (update.getMessage().getChatId()))) {
                chats.add(new Chat(update.getMessage().getChatId(), telegramClient));
            }

            chats
                .stream()
                .filter(chat -> chat.getId() == (update.getMessage().getChatId()))
                .iterator()
                .next()
                .consume(update.getMessage())
            ;

        }
    }
}