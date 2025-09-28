package org.vasyaradulsoftware.arbitragelib.exchange;

import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.vasyaradulsoftware.arbitragelib.Orderbook;

import decimal.Decimal;

public class OkxSpot extends Okx
{
    protected OkxSpot() throws URISyntaxException
    {
        super("Okx(spot)", ExType.SPOT);
    }

    public static OkxSpot create()
    {
        try {
            return new OkxSpot();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @Override
    protected Orderbook createOrderbook(String baseCurrency, String quoteCurrency) {
        return new OkxSpotOrderbook(baseCurrency, quoteCurrency);
    }

    protected class OkxSpotOrderbook extends OkxOrderbook {
        protected OkxSpotOrderbook(String baseCurrency, String quoteCurrency) {
            super(baseCurrency, quoteCurrency);
        }

        @Override
        public void update(JSONObject msg) {
            List<Row> ask = new ArrayList<Row>();
            JSONArray askJSON = msg.getJSONArray("data").getJSONObject(0).getJSONArray("asks");

            for (int i = 0; i < askJSON.length(); i++) {
                try {
                    Decimal price = new Decimal().parse(askJSON.getJSONArray(i).getString(0));
                    Decimal sizeBase = new Decimal().parse(askJSON.getJSONArray(i).getString(1));
                    Decimal sizeQuote = sizeBase.clone().mulRD(price);
                    ask.add(new Row(price, sizeQuote, sizeBase));
                }
                catch (ParseException e) {}
            }

            List<Row> bid = new ArrayList<Row>();
            JSONArray bidJSON = msg.getJSONArray("data").getJSONObject(0).getJSONArray("bids");

            for (int i = 0; i < bidJSON.length(); i++) {
                try {
                    Decimal price = new Decimal().parse(bidJSON.getJSONArray(i).getString(0));
                    Decimal sizeBase = new Decimal().parse(bidJSON.getJSONArray(i).getString(1));
                    Decimal sizeQuote = sizeBase.clone().mulRD(price);
                    bid.add(new Row(price, sizeQuote, sizeBase));
                }
                catch (ParseException e) {}
            }

            update(UpdType.SNAPSHOT, msg.getJSONArray("data").getJSONObject(0).getLong("ts"), ask, bid);
        }
    }
}
