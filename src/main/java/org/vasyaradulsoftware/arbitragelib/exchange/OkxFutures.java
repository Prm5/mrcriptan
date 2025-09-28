package org.vasyaradulsoftware.arbitragelib.exchange;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.vasyaradulsoftware.arbitragelib.Orderbook;

import decimal.Decimal;

public class OkxFutures extends Okx
{
    protected OkxFutures() throws URISyntaxException
    {
        super("Okx(futures)", ExType.FUTURES);
    }

    public static OkxFutures create()
    {
        try {
            return new OkxFutures();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @Override
    protected Orderbook createOrderbook(String baseCurrency, String quoteCurrency) {
        return new OkxFuturesOrderbook(baseCurrency, quoteCurrency);
    }

    protected class OkxFuturesOrderbook extends OkxOrderbook
    {
        private Decimal contractValue;
        private boolean ctValCcyIsQuote;

        protected OkxFuturesOrderbook(String baseCurrency, String quoteCurrency) {
            super(baseCurrency, quoteCurrency);

            try {
                JSONObject responce = new JSONObject(HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create("https://www.okx.com/api/v5/public/instruments?instType=SWAP&instId=" + baseCurrency + "-" + quoteCurrency + "-SWAP"))
                        .build(),
                    HttpResponse.BodyHandlers.ofString()
                ).body());

                contractValue = new Decimal().parse(responce
                    .getJSONArray("data")
                    .getJSONObject(0)
                    .getString("ctVal"));

                ctValCcyIsQuote = responce
                    .getJSONArray("data")
                    .getJSONObject(0)
                    .getString("ctValCcy")
                    .equals(quoteCurrency);

            } catch (InterruptedException e) {
                e.printStackTrace();
                this.close();
            } catch (IOException e) {
                e.printStackTrace();
                this.close();
            } catch (ParseException e) {
                e.printStackTrace();
                this.close();
            }
        }

        @Override
        public void update(JSONObject msg) {
            List<Row> ask = new ArrayList<Row>();
            JSONArray askJSON = msg.getJSONArray("data").getJSONObject(0).getJSONArray("asks");

            for (int i = 0; i < askJSON.length(); i++) {
                try {
                    Decimal price = new Decimal().parse(askJSON.getJSONArray(i).getString(0));
                    Decimal sizeContracts = new Decimal().parse(askJSON.getJSONArray(i).getString(1));
                    Decimal sizeQuote;
                    Decimal sizeBase;
                    if (ctValCcyIsQuote) {
                        sizeQuote = sizeContracts.mulRD(contractValue);
                        sizeBase = sizeQuote.clone().divRD(price);
                    } else {
                        sizeBase = sizeContracts.mulRD(contractValue);
                        sizeQuote = sizeBase.clone().mulRD(price);
                    }
                    ask.add(new Row(price, sizeQuote, sizeBase));
                }
                catch (ParseException e) {}
            }

            List<Row> bid = new ArrayList<Row>();
            JSONArray bidJSON = msg.getJSONArray("data").getJSONObject(0).getJSONArray("bids");

            for (int i = 0; i < bidJSON.length(); i++) {
                try {
                    Decimal price = new Decimal().parse(bidJSON.getJSONArray(i).getString(0));
                    Decimal sizeContracts = new Decimal().parse(bidJSON.getJSONArray(i).getString(1));
                    Decimal sizeQuote;
                    Decimal sizeBase;
                    if (ctValCcyIsQuote) {
                        sizeQuote = sizeContracts.mulRD(contractValue);
                        sizeBase = sizeQuote.clone().divRD(price);
                    } else {
                        sizeBase = sizeContracts.mulRD(contractValue);
                        sizeQuote = sizeBase.clone().mulRD(price);
                    }
                    bid.add(new Row(price, sizeQuote, sizeBase));
                }
                catch (ParseException e) {}
            }

            update(UpdType.SNAPSHOT, msg.getJSONArray("data").getJSONObject(0).getLong("ts"), ask, bid);
        }
    }
}
