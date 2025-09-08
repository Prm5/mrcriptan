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

import java.math.RoundingMode;
import java.text.ParseException;

/**
 * Fixed point decimal, represented as a long mantissa and integer implied decimal points (dp) from 0 to 9, which is constant
 * for a concrete class (instance of same class must always have the same scale). Multiple subclases with different dps
 * can be created, e.g. Quantity with 2 dp and Price with 8 dp.
 * Supports basic arithmetic operations with full control of overflow and rounding.
 * Rounding must be explicitly provided if required with the exception of "RD" methods that round DOWN (fastest).
 * Non-allocating (unless explicitly specified).
 * <p>
 * Special value {@link #NaN} is used to represent an invalid operation, including<ul>
 *     <li>Overflow</li>
 *     <li>Unexpected rounding (in particular, underflow)</li>
 *     <li>Division by zero</li>
 * </ul>
 * Any operations involving {@link #NaN} return {@link #NaN}.
 * <p>
 * All operations can involve at most 2 different scales as arguments and result, they can be divided into:<ul>
 *     <li>Unary operations, taking a single argument and modifying this object, e.g. a.add(5)</li>
 *     <li>Binary operations, taking a 2 argument of the <b>same</b> scale and putting result to this object, e.g. a.plus(3, 4)</li>
 * </ul>
 * A long value can be used instead of a Decimal argument with 0 dp.
 * <p>
 * Note: this class has a natural ordering that is inconsistent with equals, see {@link #compareTo}
 *
 * @param <T>
 */
public abstract class AbstractDecimal<T extends AbstractDecimal> extends BaseDecimal implements Comparable<T>, Cloneable {
    public static final long NaN = Long.MIN_VALUE;

    /**
     * Implied decimal points, must be constant for the class, must be between 0 and 9.
     */
    protected abstract int getScale();

    /**
     * @return type-casted this object
     */
    @SuppressWarnings("unchecked")
    public T self() {
        return (T) this;
    }

    /**
     * Raw long value without decimal points. Can be from {@link -Long#MAX_VALUE} to {@link Long#MAX_VALUE} with the
     * {@link Long#MIN_VALUE} reserved for NaN
     */
    public long getRaw() {
        return a;
    }

    /**
     * Raw long value without decimal points
     */
    public T setRaw(long raw) {
        a = raw;
        return self();
    }

    /**
     * true if the value is NaN.
     * All arithmetic operations with NaN returns NaN.
     */
    public boolean isNaN() {
        return getRaw() == NaN;
    }

    /**
     * Throws an exception (allocating) if the value is NaN
     */
    public T checkNotNaN() {
        if (isNaN()) {
            throw new ArithmeticException("Last operation was invalid (overflow or division by zero)");
        }
        return self();
    }

    /**
     * Change the sign of the number.
     */
    public T negate() {
        return !isNaN() ? setRaw(-getRaw()) : self();
    }

    /**
     * Copy the value from another decimal, rounding down if necessary
     */
    public T setRD(AbstractDecimal<?> a) {
        return set(a, RoundingMode.DOWN);
    }
    
    /**
     * Copy the value from another decimal
     * Rounding is required if the argument scale is greater than this scale.
     */
    public T set(AbstractDecimal<?> a, RoundingMode roundingMode) {
        int scale = getScale() - a.getScale();
        if (scale == 0) {
            return setRaw(a.getRaw());
        } else if (scale < 0 && !a.isNaN()) {
            long result = downScale_63_31(a.getRaw(), -scale);
            long remainder = getRaw();
            return setRaw(round(result, remainder, POW10[-scale], roundingMode));
        } else {
            return setRaw(scaleWithOverflow(a.getRaw(), scale));
        }
    }

    /**
     * Copy the value from long (considering scale)
     * No rounding required.
     */
    public T set(long a) {
        return setRaw(scaleWithOverflow(a, getScale()));
    }

    /**
     * Copy the value from another Decimal with the same scale.
     * No rounding required.
     */
    public T set(T a) {
        return set(a, RoundingMode.UNNECESSARY);
    }

    /**
     * Add 2 numbers of the same scale and puts result to this
     * Round DOWN if the arguments' scale is greater than this scale.
     */
    public <V extends AbstractDecimal> T plusRD(V a, V b) {
        return plus(a, b, RoundingMode.DOWN);
    }

    /**
     * Add 2 numbers of the scale same to this
     * No rounding required.
     */
    public T plus(T a, T b) {
        if (getScale() != a.getScale() || getScale() != b.getScale()) {
            throw new IllegalArgumentException("Scales must be the same");
        }

        return setRaw(plusWithOverflow(a.getRaw(), b.getRaw()));
    }

    /**
     * Add 2 numbers of the same scale and puts result to this
     * Rounding is required if the arguments' scale is greater than this scale.
     */
    public <V extends AbstractDecimal> T plus(V a, V b, RoundingMode roundingMode) {
        if (a.getScale() != b.getScale()) {
            throw new IllegalArgumentException("Scales must be the same");
        }

        int scale = getScale() - a.getScale();
        return plus(a.getRaw(), b.getRaw(), roundingMode, scale);
    }

    /**
     * Add 2 longs and puts result to this
     * No rounding required
     */
    public T plus(long a, long b) {
        return setRaw(plusAndScale(a, b, getScale()));
    }

    /**
     * Adds 2 longs and multiply the result by (possibly negative) power of 10
     */
    private T plus(long a, long b, RoundingMode roundingMode, int scale) {
        if (scale >= 0 || a == NaN || b == NaN) {
            return setRaw(plusAndScale(a, b, scale));
        }

        if (a >= 0 && b >= 0) { // unsigned overflow is not possible, ok with signed one
            a = unsignedDownScale_64_31(a + b, -scale);
            b = getRaw();
        } else if (a < 0 && b < 0) { // same as above, but negate everything before and after
            a = -unsignedDownScale_64_31(-a - b, -scale);
            b = -getRaw();
        } else { // no overflow is possible
            a = downScale_63_31(a + b, -scale);
            b = getRaw();
        }
        return setRaw(round(a, b, POW10[-scale], roundingMode));
    }

    /**
     * Add a number to this.
     * Round DOWN if the argument scale is greater than this scale.
     */
    public <V extends AbstractDecimal> T addRD(V a) {
        return add(a, RoundingMode.DOWN);
    }

    /**
     * Add a number to this.
     * Rounding is required if the argument scale is greater than this scale.
     */
    public <V extends AbstractDecimal> T add(V a, RoundingMode roundingMode) {
        int scale = getScale() - a.getScale();
        return add(a.getRaw(), roundingMode, scale);
    }

    /**
     * Add a number of the same scale to this.
     * No rounding required.
     */
    @SuppressWarnings("unchecked")
    public T add(T a) {
        return plus((T) this, a);
    }

    /**
     * Add a long number to this.
     * No rounding required.
     */
    public T add(long a) {
        return setRaw(scaleAndPlus(a, getScale(), getRaw()));
    }

    /**
     * Adds a value multiplied by (possibly negative)  power of 10
     */
    private T add(long a, RoundingMode roundingMode, int scale) {
        if (scale < 0 && !isNaN() && a != NaN) {
            long self = getRaw();
            long other = downScale_63_31(a, -scale);
            long remainder = getRaw();

            // have to inline plusWithOverflow here to avoid extra "if NaN then return immediately"
            long result = self + other;
            if (self == NaN || other == NaN || (result < 0) != (self < 0) && (result < 0) != (other < 0)) {
                return setRaw(NaN);
            }

            int denominator = POW10[-scale];
            if (result < 0 && remainder > 0) {
                remainder -= denominator;
                ++result;
            } else if (result > 0 && remainder < 0) {
                remainder += denominator;
                --result;
            }

            return setRaw(round(result, remainder, denominator, roundingMode));
        }

        return setRaw(scaleAndPlus(a, scale, getRaw()));
    }

    /**
     * Subtract 2 numbers of the same scale and puts result to this
     * Round DOWN if the arguments' scale is greater than this scale.
     */
    public <V extends AbstractDecimal> T minusRD(V a, V b) {
        return minus(a, b, RoundingMode.DOWN);
    }

    /**
     * Subtract 2 numbers of the scale same to this
     * No rounding required.
     */
    public T minus(T a, T b) {
        if (getScale() != a.getScale() || getScale() != b.getScale()) {
            throw new IllegalArgumentException("Scales must be the same");
        }

        return setRaw(plusWithOverflow(a.getRaw(), -b.getRaw()));
    }

    /**
     * Subtract 2 numbers of the same scale and puts result to this
     * Rounding is required if the arguments' scale is greater than this scale.
     */
    public <V extends AbstractDecimal> T minus(V a, V b, RoundingMode roundingMode) {
        if (a.getScale() != b.getScale()) {
            throw new IllegalArgumentException("Scales must be the same");
        }

        int scale = getScale() - a.getScale();
        return plus(a.getRaw(), -b.getRaw(), roundingMode, scale);
    }

    /**
     * Subtract 2 longs and puts result to this
     * No rounding required
     */
    public T minus(long a, long b) {
        return plus(a, -b);
    }

    /**
     * Subtract a number from this.
     * Round DOWN if the argument scale is greater than this scale.
     */
    public <V extends AbstractDecimal> T subtractRD(V a) {
        return subtract(a, RoundingMode.DOWN);
    }

    /**
     * Subtract a number from this.
     * Rounding is required if the argument scale is greater than this scale.
     */
    public <V extends AbstractDecimal> T subtract(V a, RoundingMode roundingMode) {
        int scale = getScale() - a.getScale();
        return add(-a.getRaw(), roundingMode, scale);
    }

    /**
     * Subtract a number of the same scale from this.
     * No rounding required.
     */
    @SuppressWarnings("unchecked")
    public T subtract(T a) {
        return minus((T) this, a);
    }

    /**
     * Subtract a long from this.
     * No rounding required.
     */
    public T subtract(long a) {
        return add(-a);
    }

    /**
     * Multiply 2 numbers of the same scale and put the result to this.
     * Round DOWN if the arguments scale combined is greater than this scale.
     */
    public <V extends AbstractDecimal> T productRD(V a, V b) {
        return product(a, b, RoundingMode.DOWN);
    }

    /**
     * Multiply 2 numbers of the same scale and put the result to this.
     * Rounding is required if the arguments scale combined is greater than this scale.
     */
    public <V extends AbstractDecimal> T product(V a, V b, RoundingMode roundingMode) {
        if (a.getScale() != b.getScale()) {
            throw new IllegalArgumentException("Scales must be the same");
        }

        int scale = a.getScale() + b.getScale() - getScale();
        if (scale >= 0) {
            return setRaw(mulScaleRound(a.getRaw(), b.getRaw(), scale, roundingMode));
        } else {
            return setRaw(scaleWithOverflow(mulWithOverflow(a.getRaw(), b.getRaw()), -scale));
        }
    }

    /**
     * Multiply 2 longs put the result to this.
     * Rounding is not required.
     */
    public T product(long a, long b) {
        return setRaw(scaleWithOverflow(mulWithOverflow(a, b), getScale()));
    }

    /**
     * Multiply this by the argument.
     * Round DOWN if argument scale is not zero.
     */
    public <V extends AbstractDecimal> T mulRD(V a) {
        return mul(a, RoundingMode.DOWN);
    }

    /**
     * Multiply this by the argument.
     * Rounding is required if argument scale is not zero.
     */
    public <V extends AbstractDecimal> T mul(V a, RoundingMode roundingMode) {
        return setRaw(mulScaleRound(getRaw(), a.getRaw(), a.getScale(), roundingMode));
    }

    /**
     * Multiply this by the argument.
     * Rounding is not required.
     */
    public T mul(long a) {
        return setRaw(mulWithOverflow(getRaw(), a));
    }

    /**
     * Divide first argument bye second and the result to this.
     * Round DOWN.
     * Return {@link #NaN} if b is zero.
     */
    public <V extends AbstractDecimal> T quotientRD(V a, V b) {
        return quotient(a, b, RoundingMode.DOWN);
    }

    /**
     * Divide first argument bye second and the result to this.
     * Rounding is always required.
     * Return {@link #NaN} if b is zero.
     */
    public <V extends AbstractDecimal> T quotient(V a, V b, RoundingMode roundingMode) {
        if (a.getScale() != b.getScale()) {
            throw new IllegalArgumentException("Scales must be the same");
        }
        return quotient(a.getRaw(), b.getRaw(), roundingMode);
    }

    /**
     * Divide first argument bye second and the result to this.
     * Round DOWN.
     * Return {@link #NaN} if b is zero.
     */
    public T quotientRD(long a, long b) {
        return quotient(a, b, RoundingMode.DOWN);
    }

    /**
     * Divide first argument bye second and the result to this.
     * Rounding is always required.
     * Return {@link #NaN} if b is zero.
     */
    public T quotient(long a, long b, RoundingMode roundingMode) {
        return setRaw(scaleDivRound(a, getScale(), b, roundingMode));
    }

    /**
     * Divide this by the argument and put result into this.
     * Round DOWN.
     * Return {@link #NaN} if a is zero.
     */
    public <V extends AbstractDecimal> T divRD(V a) {
        return div(a, RoundingMode.DOWN);
    }

    /**
     * Divide this by the argument and put result into this.
     * Rounding is always required.
     * Return {@link #NaN} if a is zero.
     */
    public <V extends AbstractDecimal> T div(V a, RoundingMode roundingMode) {
        return setRaw(scaleDivRound(getRaw(), a.getScale(), a.getRaw(), roundingMode));
    }

    /**
     * Divide this by the argument and put result into this.
     * Round DOWN.
     * Return {@link #NaN} if a is zero.
     */
    public T divRD(long a) {
        return div(a, RoundingMode.DOWN);
    }

    /**
     * Divide this by the argument and put result into this.
     * Rounding is always required.
     * Return {@link #NaN} if a is zero.
     */
    public T div(long a, RoundingMode roundingMode) {
        if (isNaN() || a == NaN || a == 0) {
            return setRaw(NaN);
        }

        // if a is negative - negate both numerator and denominator (to comply with "round" contract)
        long signA = a >> 63;
        a = negIf(a, signA);
        long raw = negIf(getRaw(), signA);

        return setRaw(round(raw / a, raw % a, a, roundingMode));
    }

    /**
     * Created a copy of the class with the same raw number.
     */
    @SuppressWarnings("unchecked")
    @Override
    public T clone() {
        try {
            return (T) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Unexpected", e);
        }
    }

    /**
     * Compares 2 values considering the scale ("0.123" < "12.0" although its not true for raw values)
     * {@link #NaN} is smaller than any other number irrespective of scale. Two {@link #NaN}s are equal to each other.
     *
     * Comparison can work across different concrete classes, and therefore can be inconsistent with {@link #equals},
     * which always returns "false" for different classes.
     */
    @Override
    public int compareTo(AbstractDecimal o) {
        if (isNaN()) {
            return o.isNaN() ? 0 : -1;
        } else if (o.isNaN()) {
            return isNaN() ? 0 : 1;
        }

        int scale = getScale() - o.getScale();
        if (scale >= 0) {
            long first = getRaw();
            long second = scaleWithOverflow(o.getRaw(), scale);
            return first < second || second == NaN && o.getRaw() > 0 ? -1 :
                    first > second ? 1 : 0;
        } else {
            long first = scaleWithOverflow(getRaw(), -scale);
            long second = o.getRaw();
            return first > second || first == NaN && getRaw() > 0 ? 1 :
                    first < second ? -1 : 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte byteValue() {
        return (byte) toLong(RoundingMode.DOWN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public short shortValue() {
        return (short) toLong(RoundingMode.DOWN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int intValue() {
        return (int) toLong(RoundingMode.DOWN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long longValue() {
        return toLong(RoundingMode.DOWN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float floatValue() {
        return (float) toDouble();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double doubleValue() {
        return toDouble();
    }

    /**
     * Returns the whole part of the value rounding DOWN, throws exception if {@link #NaN}
     */
    public long toLongRD() {
        return toLong(RoundingMode.DOWN);
    }

    /**
     * Returns the whole part of the value, throws exception if {@link #NaN}
     */
    public long toLong(RoundingMode roundingMode) {
        if (isNaN()) {
            throw new ArithmeticException("NaN");
        }


        long raw = getRaw(); // will be overridden by remainder

        int scale = getScale();
        long result = round(downScale_63_31(raw, scale), getRaw(), POW10[scale], roundingMode);

        setRaw(raw);

        return result;
    }

    /**
     * Converts long to Decimal value. Can result in {@link #NaN} (if overflow)
     */
    public T fromLong(long value) {
        return setRaw(scaleWithOverflow(value, getScale()));
    }

    /**
     * Converts scaled long to Decimal value. Can result in {@link #NaN} (if overflow)
     * The result will be equal to value * 10^scale
     *
     * In particular, if scale == {@link #getScale} then {@link #setRaw}(value)
     *
     * @param value mantissa
     * @param scale decimal exponent
     * @param roundingMode required if scale is greater than {@link #getScale}
     */
    public T fromLong(long value, int scale, RoundingMode roundingMode) {
        if (scale < -18) {
            return setRaw(value == 0 ? 0 : NaN);
        }
        if (getScale() - scale < -18) {
            // rounded zero
            return setRaw(round(0, Long.signum(value), Long.MAX_VALUE, roundingMode));
        }

        scale = getScale() - scale; // no overflow guaranteed
        if (scale >= 0) {
            return setRaw(scale > 18 ? NaN : scaleWithOverflow(value, scale));
        } else {
            scale = -scale;
            assert scale <= 18;
            long remainder = 0;
            long divisor = LONG_POW10[scale];
            if (scale > 9) {
                // double division by constant
                // the formula for final remainder derived from:
                // v / d1 / d2 = (q1 + r1 / d1) / d2 = q1 / d2 + r1 / d1d2 = q2 + r2 / d2 + r1 / d1d2 = q2 + (r2d1 + r1) / d1d2
                value = downScale_63_31(value, 9);
                remainder = a * 1000000000;
                scale -= 9;
            }
            value = downScale_63_31(value, scale);
            remainder += a;

            return setRaw(round(value, remainder, divisor, roundingMode));
        }
    }

    public T fromLongRD(long value, int scale) {
        return fromLong(value, scale, RoundingMode.DOWN);
    }

    /**
     * Converts Decimal to floating-point number, returns {@link Double#NaN} if {@link #NaN}
     */
    public double toDouble() {
        return !isNaN() ? (double) getRaw() / POW10[getScale()] : Double.NaN;
    }

    /**
     * Converts doublie to Decimal value rounding DOWN
     */
    public T fromDoubleRD(double value) {
        return fromDouble(value, RoundingMode.DOWN);
    }

    /**
     * Converts doublie to Decimal value with the following rounding modes supported:<ul>
     *     <li>DOWN - simple cast</li>
     *     <li>FLOOR - uses {@link Math#floor}</li>
     *     <li>CEILING - uses {@link Math#ceil}</li>
     *     <li>HALF_EVEN - uses {@link Math#rint}</li>
     * </ul>
     */
    public T fromDouble(double value, RoundingMode roundingMode) {
        value *= POW10[getScale()];
        switch (roundingMode) {
            case DOWN:
                return setIntDouble(value);
            case UP:
                return setIntDouble(value > 0 ? Math.ceil(value) : Math.floor(value));
            case FLOOR:
                return setIntDouble(Math.floor(value));
            case CEILING:
                return setIntDouble(Math.ceil(value));
            case HALF_EVEN:
                return setIntDouble(Math.rint(value));
            default:
                throw new IllegalArgumentException("Unsupported rounding mode for double: " + roundingMode);
        }
    }

    private T setIntDouble(double value) {
        return setRaw(value >= -Long.MAX_VALUE && value <= Long.MAX_VALUE ? (long) value : NaN);
    }

    /**
     * Converts to ASCII string. Allocating.
     * @see #toStringBuilder
     */
    public String toString() {
        return toStringBuilder(new StringBuilder(21)).toString();
    }

    /**
     * Converts to ASCII string. Shows number of dps as well, e.g. "1.00"
     * NaN values are displayed as "NaN"
     */
    public StringBuilder toStringBuilder(StringBuilder sb) {
        long raw = getRaw();
        if (raw == NaN) {
            sb.append("NaN");
            return sb;
        }

        if (raw < 0) {
            sb.append('-');
            raw = -raw;
        }

        int length = stringSize(raw);
        int scale = getScale();
        if (scale >= length) {
            sb.append(ZEROES[scale - length + 1]);
        }
        sb.append(raw);

        assert sb.length() > scale : "sb.length() >= length + scale - length + 1 = scale + 1 > scale";

        if (scale > 0) {
            sb.insert(sb.length() - scale, '.');
        }

        return sb;
    }

    /**
     * Parse a string (including NaN) and creates a value from it.
     * Unlike other methods, does NOT use NaN to indicate an error, uses ParseException instead.
     */
    public T parse(CharSequence charSequence) throws ParseException {
        return parse(charSequence, 0, charSequence.length());
    }

    public T parse(CharSequence charSequence, int offset, int length) throws ParseException {
        if (length == 0) {
            throw new ParseException("Empty string", 0);
        }

        boolean negative = false;
        char ch = charSequence.charAt(offset);
        switch (ch) {
            case '-':
                if (length == 1) {
                    throw new ParseException("Single '-' is not expected", 0);
                }
                negative = true;
                offset++;
                break;
            case 'N':
            case 'n':
                if (length != 3 ||
                        charSequence.charAt(offset + 1) != 'a' && charSequence.charAt(offset + 1) != 'A' ||
                        charSequence.charAt(offset + 2) != 'n' && charSequence.charAt(offset + 2) != 'N') {
                    throw new ParseException("Unexpected alphanumeric value", 0);
                }
                return setRaw(NaN);
            default:
                // go on
        }

        long result = 0;
        int fractionalStart = length;
        while (offset < length) {
            ch = charSequence.charAt(offset++);
            if (ch == '.') {
                if (fractionalStart != length) {
                    throw new ParseException("Double '.' found", offset);
                }
                if (offset == length) {
                    throw new ParseException("Last '.' found", offset);
                }
                fractionalStart = offset; // dot position incremented
                while (length > fractionalStart && charSequence.charAt(length - 1) == '0') {
                    length--;
                }
            } else if (ch >= '0' && ch <= '9') {
                if (result > Long.MAX_VALUE / 10) {
                    throw new ParseException("Overflow", 0);
                }
                result *= 10;
                result += ch - '0';
                if (result < 0) {
                    throw new ParseException("Overflow", 0);
                }
            } else {
                throw new ParseException("Unexpected " + ch, offset - 1);
            }
        }

        result = scaleWithOverflow(result, getScale() - (length - fractionalStart));
        if (result == NaN) {
            throw new ParseException("Overflow while scaling up", 0);
        }
        return setRaw(negative ? -result : result);
    }

    private static final int MAX_LONG_SIZE = Long.toString(Long.MAX_VALUE).length();
    private static int stringSize(long value) {
        long product = 10;
        for (int size = 1; size < MAX_LONG_SIZE; size++) {
            if (value < product)
                return size;
            product *= 10;
        }
        return 19;
    }

    /**
     * Adds 2 longs and multiply the result by non-negative power of 10
     */
    private static long plusAndScale(long a, long b, int nonNegScale) {
        return scaleWithOverflow(plusWithOverflow(a, b), nonNegScale);
    }

    /**
     * Multiply a value to a non-negative power of 10, then add it to another value, i.e.
     * a * 10^scale + b
     * Tries to eliminate multiplication overflow in case of different sign addends
     */
    private static long scaleAndPlus(long a, int nonNegScale, long b) {
        if (nonNegScale > 0) {
            long scaled = scaleWithOverflow(a, nonNegScale);
            if (scaled == NaN && a != NaN && b != NaN) {
                // trying to eliminate overflow (possible when a and raw has different signs and abs(raw) is big enough)

                // another bigger limit for unsigned multiplication (if we are out of this as well - give up)
                long unsignedLimit = SCALE_OVERFLOW_LIMITS[nonNegScale] * 2;
                if (b < 0 && a > 0 && a <= unsignedLimit) {
                    a = a * POW10[nonNegScale];
                    assert a < 0 : "Overflow to sign expected";
                    a += b; // subtracting abs(raw)
                    if (a >= 0) {
                        // no more overflow
                        return a;
                    }
                } else if (b > 0 && a >= -unsignedLimit && a < 0) {
                    a = -a * POW10[nonNegScale]; // negate a before multiplying
                    assert a < 0 : "Overflow to sign expected";
                    a -= b; // adding raw (in negated terms)
                    if (a >= 0) {
                        // no more overflow
                        return -a; // don't forget to negate "a" back
                    }
                }
            }
            a = scaled;
        }

        return plusWithOverflow(b, a);
    }

    private static long plusWithOverflow(long a, long b) {
        long result = a + b;
        return a == NaN || b == NaN || (result < 0) != (a < 0) && (result < 0) != (b < 0) ? NaN : result;
    }

    private static long scaleWithOverflow(long value, int scale) {
         return value >= -SCALE_OVERFLOW_LIMITS[scale] && value <= SCALE_OVERFLOW_LIMITS[scale] ?
                 value * LONG_POW10[scale] : NaN;
    }

    private long mulWithOverflow(long a, long b) {
        if (a > Integer.MIN_VALUE && a <= Integer.MAX_VALUE &&
                b > Integer.MIN_VALUE && b <= Integer.MAX_VALUE) { // and not NaN
            // can multiply without overflow
            return a * b;
        } else {
            return mulScaleRound(a, b, 0, RoundingMode.DOWN);
        }
    }
}
