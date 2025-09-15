package org.vasyaradulsoftware.arbitragelib;

import org.vasyaradulsoftware.arbitragelib.Ticker.Param;

import decimal.Decimal;

public interface Message
{
    public boolean isResponce();
    public Responce getResponce();

    public boolean isUpdate();
    public Update getUpdate();

    public interface Responce
    {
        public int getResponceId();

        public boolean isSubscribedSuccessful();
        public boolean isUnsubscribedSuccessful();
        public boolean isSubscribeError();
    }

    public interface Update
    {
        public boolean isTickerUpdate();
        public boolean isOrderbookUpdate();

        public String getBaseCurrency();
        public String getQuoteCurrency();

        public long getTimestamp();

        public Decimal get(Param param) throws InvalidFieldExeption, NotChangedExeption;
    }

    public class InvalidFieldExeption extends Exception {}
    public class NotChangedExeption extends Exception {}
}
