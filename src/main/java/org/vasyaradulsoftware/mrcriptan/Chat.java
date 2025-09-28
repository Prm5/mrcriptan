package org.vasyaradulsoftware.mrcriptan;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.vasyaradulsoftware.arbitragelib.Exchange;

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
            String[] command = message.getText().split("[_]");

            if(command[0].equals("/start")) {

                SendMessage msg = SendMessage
                    .builder()
                    .chatId(id)
                    .text(
                        "Привет я Мистер Криптан 0.2 - продвинутый исскуственный интеллект для крипто анализа."
                        +"\nЯ умею:"
                        +"\n\t- показывать спред на разных биржах (/spreads_BTC)"
                        +"\n\t- выводить биржевой стакан (/orderbook_" + Exchange.publicExchanges.stream().findAny().get().getName() + "_BTC)"
                        +"\n\t- список доступных бирж для анализа(/exchange_list)"
                        +"\n\t- остонавливать спам машину(/unfollow)"
                        +"\n\t- всё. типо всё больше ничего не умею. но скоро ряльно буду уметь ваще всё (если разраб не обленится вкрай и не дропнет проект)"
                    )
                    .build();
                try {
                    telegramClient.execute(msg);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }

            }
            else if (command[0].equals("/spreads"))
            {
                if (command.length == 2) {
                    follows.add(new FollowSpreads(command[1], "USDT", id, telegramClient).start());
                } else if (command.length == 3) {
                    follows.add(new FollowSpreads(command[1], command[2], id, telegramClient).start());
                }
            }
            else if (command[0].equals("/orderbook")) {
                if (command.length == 3) {
                    follows.add(new FollowOrderbook(command[2], "USDT", command[1], id, telegramClient).start());
                } else if (command.length == 4) {
                    follows.add(new FollowOrderbook(command[2], command[3], command[1], id, telegramClient).start());
                }
            }
            else if (command[0].equals("/exchange")) {
                if (command[1].equals("list")) {
                    String text = "Поддерживаемые биржи:";
                    for (Exchange e : Exchange.publicExchanges) {
                        text = text + "\n\t- " + e.getName();
                    }

                    SendMessage msg = SendMessage
                        .builder()
                        .chatId(id)
                        .text(
                            text
                        )
                        .build();
                    try {
                        telegramClient.execute(msg);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }
            }
            else if (command[0].equals("/unfollow"))
            {
                Iterator<Follow> i = follows.iterator();
                while (i.hasNext()) {
                    i.next().close();
                    i.remove();
                };
            }
        }
    }
}
