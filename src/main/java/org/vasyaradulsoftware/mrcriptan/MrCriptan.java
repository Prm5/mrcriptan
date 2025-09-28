package org.vasyaradulsoftware.mrcriptan;

import org.vasyaradulsoftware.arbitragelib.Exchange;

public class MrCriptan {

    public static void main(String[] args)
    {    
        Exchange.initExchanges();

        String botToken = System.getenv("BOT_TOKEN");
        System.out.println(botToken);
        new Thread(new Bot(botToken)).start();

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}