package org.vasyaradulsoftware.arbitragelib.exchange;

import java.net.URISyntaxException;

public class GateSpot extends Gate
{
    private GateSpot() throws URISyntaxException
    {
        super("wss://api.gateio.ws/ws/v4/", "Gate(spot)", "spot");
    }

    public static GateSpot create() {
        try {
            return new GateSpot();
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
