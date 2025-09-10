package org.vasyaradulsoftware.mrcriptan;

import org.vasyaradulsoftware.arbitragelib.TradingPair;
import org.vasyaradulsoftware.arbitragelib.TradingPair.NoTickersExeption;

public class MrCriptan {

    public static void main(String[] args)
    {    
        TradingPair.init();

        String botToken = System.getenv("BOT_TOKEN");
        System.out.println(botToken);
        new Thread(new Bot(botToken)).start();

        //тест
        /**
        TradingPair p = TradingPair.follow("BTC", "USDT");

        for (int i = 0; i < 20; i++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                System.out.println(p.getPriceInfo());
            } catch (NoTickersExeption e) {
                System.out.println("нема тiкерiв");
            }
        }
        //*/

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}