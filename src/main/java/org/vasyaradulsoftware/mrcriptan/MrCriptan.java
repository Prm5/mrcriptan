package org.vasyaradulsoftware.mrcriptan;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.vasyaradulsoftware.arbitragelib.TradingPair;

public class MrCriptan {

    public static void main(String[] args)
    {    
        TradingPair.init();

        String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        System.out.println(rootPath);
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(rootPath + "mrcriptan.properties"));
        } catch (IOException e) {
            System.out.println(e);
            return;
        }
        String botToken = properties.getProperty("telegramBotToken");
        new Thread(new Bot(botToken)).start();

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