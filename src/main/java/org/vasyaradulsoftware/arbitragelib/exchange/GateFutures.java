package org.vasyaradulsoftware.arbitragelib.exchange;

import java.net.URISyntaxException;

import org.vasyaradulsoftware.arbitragelib.Message;

public class GateFutures extends Gate
{
    private GateFutures() throws URISyntaxException
    {
        super("wss://fx-ws.gateio.ws/v4/ws/usdt", "Gate(futures)", "futures");
    }

    public static GateFutures create() {
        try {
            return new GateFutures();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @Override
    protected Message parse(String message) {
        return new GateFuturesMessage(message);
    }

    protected class GateFuturesMessage extends GateMessage {

        public GateFuturesMessage(String message) {
            super(message);
        }
        
        

        @Override
        public String getUpdateBaseCurrency() {
            return this.getJSONArray("result").getJSONObject(0).getString("contract").split("[_]")[0];
        }

        @Override
        public String getUpdateQuoteCurrency() {
            return this.getJSONArray("result").getJSONObject(0).getString("contract").split("[_]")[1];
        }

        @Override
        public String getUpdateLastPrice() {
            return this.getJSONArray("result").getJSONObject(0).getString("last");
        }
    }
}
