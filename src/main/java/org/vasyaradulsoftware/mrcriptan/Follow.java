package org.vasyaradulsoftware.mrcriptan;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public abstract class Follow implements Runnable {

    private TelegramClient telegramClient;
    private long chatId;
    private Message message;
    private volatile boolean running;

    public Follow(long chatId, TelegramClient telegramClient) {
        this.chatId = chatId;
        this.telegramClient = telegramClient;
    }

    public Follow start() {
        new Thread(this).start();
        return this;
    }

    protected void send(String text) {
        if (message == null) {
            SendMessage send = SendMessage
                .builder()
                .chatId(chatId)
                .text(text)
                .build();

            try {
                message = telegramClient.execute(send);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            EditMessageText edit = EditMessageText
                .builder()
                .chatId(message.getChatId())
                .messageId(message.getMessageId())
                .text(text)
                .build();

            try {
                telegramClient.execute(edit);
            } catch (TelegramApiException e) {
                //e.printStackTrace();
            }
        }
    }

    protected abstract void loop();
    protected abstract void onClose();

    @Override
    public void run() {
        running = true;
        while (running) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                close();
                break;
            }

            loop();
        }
        onClose();
    }

    public boolean isRunning() {
        return running;
    }

    public void close() {
        running = false;
    }
}
