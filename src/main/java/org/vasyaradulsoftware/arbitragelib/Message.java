package org.vasyaradulsoftware.arbitragelib;

import org.json.JSONObject;
import org.vasyaradulsoftware.arbitragelib.Subscribtion.Channel;

public abstract class Message
{
    protected final JSONObject o;

    public Message(String message) {
        o = new JSONObject(message);
    }

    @Override
    public String toString() {
        return o.toString();
    }

    public abstract boolean isResponce();
    public abstract Responce getResponce();

    public abstract boolean isUpdate();
    public abstract Update getUpdate();

    public abstract class Responce
    {
        @Override
        public String toString() {
            return o.toString();
        }

        public abstract int getResponceId();

        public abstract boolean isSubscribedSuccessful();
        public abstract boolean isUnsubscribedSuccessful();
        public abstract boolean isSubscribeError();
    }

    public abstract class Update
    {
        @Override
        public String toString() {
            return o.toString();
        }

        public JSONObject getJSONObject() {
            return o;
        }

        public abstract Channel getChannel();
        public abstract String getBaseCurrency();
        public abstract String getQuoteCurrency();
    }

    public class InvalidFieldExeption extends Exception {}
    public class NotChangedExeption extends Exception {}
}
