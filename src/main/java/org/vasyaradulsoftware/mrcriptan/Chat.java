package org.vasyaradulsoftware.mrcriptan;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public class Chat {

    private long id;
    private TelegramClient telegramClient;

    private List<Follow> follows = new ArrayList<Follow>();

    public Chat(long chatId, TelegramClient telegramClient) {
        this.id = chatId;
        this.telegramClient = telegramClient;
    }
    
    public long getId() {
        return id;
    }

    public void consume(Message message) {
        if(message.hasText()) {

            System.out.println("message \"" + message.getText() + "\" received from user" + message.getChat().getUserName());
            String[] command = message.getText().split("[ ]");

            if(command[0].equals("/start")) {

                SendMessage msg = SendMessage
                    .builder()
                    .chatId(id)
                    .text(
                        "мистер Криптан v0.1 готов браться за дело!"
                        +"\nЯ умею:"
                        +"\n\t- показывать спред на разных биржах (/follow BTC)"
                        +"\n\t- больше ниче не умею, все претензии к разрабу"
                    )
                    .build();
                try {
                    telegramClient.execute(msg);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }

            } else if (command[0].equals("/follow")) {

                if (command.length == 2) {
                    follows.add(new Follow(command[1], "USDT", id, telegramClient));
                }

            } else if (command[0].equals("/unfollow")) {

                Iterator<Follow> i = follows.iterator();
                while (i.hasNext()) {
                    i.next().close();
                    i.remove();
                };
            }
        }
    }
}
