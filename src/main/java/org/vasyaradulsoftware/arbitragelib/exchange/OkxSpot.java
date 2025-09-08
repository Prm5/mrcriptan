package org.vasyaradulsoftware.arbitragelib.exchange;

import java.net.URISyntaxException;

public class OkxSpot extends Okx
{
    public static OkxSpot create()
    {
        try
        {
            return new OkxSpot();
        }
        catch (URISyntaxException e)
        {
            return null;
        }
    }

    protected OkxSpot() throws URISyntaxException
    {
        super("Okx(spot)");
    }
}
