/*
 MIT License

 Copyright (c) 2016 Maxim Tomin

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */
package decimal;

import java.text.ParseException;

/**
 * Reference implementation of {@link AbstractDecimal} for maximum supported precision (9 dp).
 * Values from -9223372036.854775807 to 9223372036.854775807 (inclusive), which should be good enough for small numbers.
 */
public class Decimal extends AbstractDecimal<Decimal> {
    /**
     * Must not be changed!
      */
    public static final Decimal MIN_VALUE = new Decimal().setRaw(-Long.MAX_VALUE);
    /**
     * Must not be changed!
     */
    public static final Decimal MAX_VALUE = new Decimal().setRaw(Long.MAX_VALUE);

    @Override
    protected int getScale() {
        return 9; // must be constant
    }

    public static Decimal create(double value) {
        return new Decimal().fromDoubleRD(value);
    }

    public static Decimal create(String value) throws ParseException {
        return new Decimal().parse(value);
    }

    public static Decimal create(long value) {
        return new Decimal().fromLong(value);
    }

    @Override
    public String toString() {
        String[] tmp = super.toString().split("[.]");
        for (int i = 8; i >= 0; i--) {
            if (tmp[1].charAt(i) != '0') {
                return tmp[0] + "." + tmp[1].substring(0, i + 1);
            }
        }
        return tmp[0] + ".0";
    }

    private int pow(int a, int b) {
        if (b <= 0) {return 1;}
        else return a * pow(a, b - 1);
    }

    public Decimal roundDown(int scale) {
        setRaw(getRaw() - (getRaw()%(pow(10, getScale() - scale))));
        return this;
    }

    public static void main(String[] args) {
        
        Decimal d = new Decimal();
        try {d.parse("0.123456789");}
        catch (ParseException e) {e.printStackTrace();}
        System.out.println(d);
        d.roundDown(9);
        System.out.println(d);
    }
}
