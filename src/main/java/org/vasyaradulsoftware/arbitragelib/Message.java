package org.vasyaradulsoftware.arbitragelib;

public interface Message {

    public boolean isResponce();
    public int getResponceId();
    public boolean isSubscribedSuccessfulResponce();
    public boolean isUnsubscribedSuccessfulResponce();
    public boolean isSubscribeErrorResponce();
    public boolean isUpdate();
    public boolean isUpdateChannelTickers();
    public String getUpdateBaseCurrency();
    public String getUpdateQuoteCurrency();
    public String getUpdateLastPrice();
    public long getUpdateTimestamp();
}
