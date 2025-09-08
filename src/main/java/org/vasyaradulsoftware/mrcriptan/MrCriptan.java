package org.vasyaradulsoftware.mrcriptan;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.vasyaradulsoftware.arbitragelib.TradingPair;

public class MrCriptan {

    public static void main(String[] args)
    {    
        TradingPair.init(); //там создаются инстансы всех бирж и кладутся в List

        String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        System.out.println(rootPath);
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(rootPath + "mrcriptan.properties"));
        } catch (IOException e) {
            System.out.println(e);
            return;
        }
        String botToken = properties.getProperty("telegramBotToken"); //это всё получение токена который лежит в файлике
        new Thread(new Bot(botToken)).start(); //запуск бота

        //тест
        /**
        TradingPair p = TradingPair.follow("BTC", "USDT");

        for (int i = 0; true; i++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println(p.getPriceInfo());
        }
        //*/
    }
}