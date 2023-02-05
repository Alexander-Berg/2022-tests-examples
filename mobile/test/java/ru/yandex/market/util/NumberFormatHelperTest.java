package ru.yandex.market.util;

import androidx.annotation.NonNull;
import android.text.Editable;
import android.text.SpannableStringBuilder;

import junit.framework.Assert;

import org.junit.Test;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import ru.yandex.market.BaseTest;

public class NumberFormatHelperTest extends BaseTest{

    private static NumberFormatHelper HELPER;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        DecimalFormat decimalFormat = (DecimalFormat) DecimalFormat.getInstance();
        DecimalFormatSymbols symbols = decimalFormat.getDecimalFormatSymbols();
        symbols.setDecimalSeparator(',');
        symbols.setMinusSign('-');
        symbols.setZeroDigit('0');
        decimalFormat.setDecimalFormatSymbols(symbols);
        HELPER = NumberFormatHelper.getInstance(decimalFormat);
    }

    @Test
    public void testIsNegative() {
        Assert.assertTrue(HELPER.isNegative("-1"));
        Assert.assertTrue(HELPER.isNegative("-rfff"));
        Assert.assertTrue(HELPER.isNegative("--"));
        Assert.assertTrue(HELPER.isNegative("-"));
        Assert.assertTrue(HELPER.isNegative("-,"));

        Assert.assertFalse(HELPER.isNegative("0"));
        Assert.assertFalse(HELPER.isNegative("111"));
        Assert.assertFalse(HELPER.isNegative("fdhfdjf"));
        Assert.assertFalse(HELPER.isNegative(""));
        Assert.assertFalse(HELPER.isNegative(";-1234"));
        Assert.assertFalse(HELPER.isNegative("1-5"));
        Assert.assertFalse(HELPER.isNegative("12-"));
        Assert.assertFalse(HELPER.isNegative("42"));
    }

    @Test(expected = NullPointerException.class)
    public void testIsNegativeNPE() {
        //noinspection ConstantConditions
        HELPER.isNegative(null);
    }

    @Test
    public void testIsDecimalSeparator() {

        Assert.assertTrue(HELPER.isDecimalSeparator(","));

        Assert.assertFalse(HELPER.isDecimalSeparator(""));
        Assert.assertFalse(HELPER.isDecimalSeparator("0"));
        Assert.assertFalse(HELPER.isDecimalSeparator("111"));
        Assert.assertFalse(HELPER.isDecimalSeparator("ABC"));
    }

    @Test(expected = NullPointerException.class)
    public void testIsDecimalSeparatorNPE() {
        //noinspection ConstantConditions
        HELPER.isDecimalSeparator(null);
    }

    @Test
    public void testFindDecimalSeparator() {

        Assert.assertEquals(-1, HELPER.findDecimalSeparator("-"));
        Assert.assertEquals(1, HELPER.findDecimalSeparator("0,1"));
        Assert.assertEquals(1, HELPER.findDecimalSeparator("0,"));
        Assert.assertEquals(0, HELPER.findDecimalSeparator(","));
        Assert.assertEquals(-1, HELPER.findDecimalSeparator(""));
        Assert.assertEquals(5, HELPER.findDecimalSeparator("-1234,355599"));
        Assert.assertEquals(12, HELPER.findDecimalSeparator("-1234,355599,"));
        Assert.assertEquals(2, HELPER.findDecimalSeparator(",,,"));
        Assert.assertEquals(0, HELPER.findDecimalSeparator(",-1234"));
        Assert.assertEquals(7, HELPER.findDecimalSeparator("adsdad.,sfdsfsfsf"));
        Assert.assertEquals(7, HELPER.findDecimalSeparator("adsdad,,sfdsfsfsf"));
        Assert.assertEquals(6, HELPER.findDecimalSeparator("adsdad,.sfdsfsfsf"));
        Assert.assertEquals(-1, HELPER.findDecimalSeparator("1.2"));
        Assert.assertEquals(3, HELPER.findDecimalSeparator("12.,5"));
        Assert.assertEquals(2, HELPER.findDecimalSeparator("12,.5"));
        Assert.assertEquals(-1, HELPER.findDecimalSeparator("42"));
        Assert.assertEquals(1, HELPER.findDecimalSeparator("-,"));
    }

    @Test(expected = NullPointerException.class)
    public void testFindDecimalSeparatorNPE() {
        //noinspection ConstantConditions
        HELPER.findDecimalSeparator(null);
    }

    @Test
    public void testFindIntegerPart() {

        Assert.assertEquals(new IntRange(0, 0), HELPER.findIntegerPart("-", true));
        Assert.assertEquals(new IntRange(0, 0), HELPER.findIntegerPart("0", true));
        Assert.assertEquals(new IntRange(0, 2), HELPER.findIntegerPart("111", true));
        Assert.assertEquals(new IntRange(0, 6), HELPER.findIntegerPart("fdhfdjf", true));
        Assert.assertEquals(new IntRange(0, 1), HELPER.findIntegerPart("-1", true));
        Assert.assertEquals(new IntRange(0, 4), HELPER.findIntegerPart("-rfff", true));
        Assert.assertEquals(new IntRange(0, 1), HELPER.findIntegerPart("--", true));
        Assert.assertEquals(new IntRange(-1, -1), HELPER.findIntegerPart("", true));
        Assert.assertEquals(new IntRange(0, 5), HELPER.findIntegerPart(";-1234", true));
        Assert.assertEquals(new IntRange(0, 2), HELPER.findIntegerPart("1-5", true));
        Assert.assertEquals(new IntRange(0, 2), HELPER.findIntegerPart("12-", true));
        Assert.assertEquals(new IntRange(0, 0), HELPER.findIntegerPart("0,1", true));
        Assert.assertEquals(new IntRange(0, 0), HELPER.findIntegerPart("0,", true));
        Assert.assertEquals(new IntRange(-1, -1), HELPER.findIntegerPart(",", true));
        Assert.assertEquals(new IntRange(0, 4), HELPER.findIntegerPart("-1234,355599", true));
        Assert.assertEquals(new IntRange(0, 11), HELPER.findIntegerPart("-1234,355599,", true));
        Assert.assertEquals(new IntRange(0, 1), HELPER.findIntegerPart(",,,", true));
        Assert.assertEquals(new IntRange(-1, -1), HELPER.findIntegerPart(",-1234", true));
        Assert.assertEquals(new IntRange(0, 6), HELPER.findIntegerPart("adsdad,,sfdsfsfsf", true));
        Assert.assertEquals(new IntRange(0, 2), HELPER.findIntegerPart("1.2", true));
        Assert.assertEquals(new IntRange(0, 2), HELPER.findIntegerPart("12.,5", true));
        Assert.assertEquals(new IntRange(0, 1), HELPER.findIntegerPart("12,.5", true));
        Assert.assertEquals(new IntRange(-1, -1), HELPER.findIntegerPart(",125", true));
        Assert.assertEquals(new IntRange(0, 1), HELPER.findIntegerPart("42", true));
        Assert.assertEquals(new IntRange(0, 0), HELPER.findIntegerPart("-,", true));

        Assert.assertEquals(new IntRange(-1, -1), HELPER.findIntegerPart("-", false));
        Assert.assertEquals(new IntRange(0, 0), HELPER.findIntegerPart("0", false));
        Assert.assertEquals(new IntRange(0, 2), HELPER.findIntegerPart("111", false));
        Assert.assertEquals(new IntRange(0, 6), HELPER.findIntegerPart("fdhfdjf", false));
        Assert.assertEquals(new IntRange(1, 1), HELPER.findIntegerPart("-1", false));
        Assert.assertEquals(new IntRange(1, 4), HELPER.findIntegerPart("-rfff", false));
        Assert.assertEquals(new IntRange(1, 1), HELPER.findIntegerPart("--", false));
        Assert.assertEquals(new IntRange(-1, -1), HELPER.findIntegerPart("", false));
        Assert.assertEquals(new IntRange(0, 5), HELPER.findIntegerPart(";-1234", false));
        Assert.assertEquals(new IntRange(0, 2), HELPER.findIntegerPart("1-5", false));
        Assert.assertEquals(new IntRange(0, 2), HELPER.findIntegerPart("12-", false));
        Assert.assertEquals(new IntRange(0, 0), HELPER.findIntegerPart("0,1", false));
        Assert.assertEquals(new IntRange(0, 0), HELPER.findIntegerPart("0,", false));
        Assert.assertEquals(new IntRange(-1, -1), HELPER.findIntegerPart(",", false));
        Assert.assertEquals(new IntRange(1, 4), HELPER.findIntegerPart("-1234,355599", false));
        Assert.assertEquals(new IntRange(1, 11), HELPER.findIntegerPart("-1234,355599,", false));
        Assert.assertEquals(new IntRange(0, 1), HELPER.findIntegerPart(",,,", false));
        Assert.assertEquals(new IntRange(-1, -1), HELPER.findIntegerPart(",-1234", false));
        Assert.assertEquals(new IntRange(0, 6), HELPER.findIntegerPart("adsdad,,sfdsfsfsf", false));
        Assert.assertEquals(new IntRange(0, 2), HELPER.findIntegerPart("1.2", false));
        Assert.assertEquals(new IntRange(0, 2), HELPER.findIntegerPart("12.,5", false));
        Assert.assertEquals(new IntRange(0, 1), HELPER.findIntegerPart("12,.5", false));
        Assert.assertEquals(new IntRange(-1, -1), HELPER.findIntegerPart(",125", false));
        Assert.assertEquals(new IntRange(0, 1), HELPER.findIntegerPart("42", false));
        Assert.assertEquals(new IntRange(-1, -1), HELPER.findIntegerPart("-,", false));
    }

    @Test(expected = NullPointerException.class)
    public void testFindIntegerPartNPE() {
        //noinspection ConstantConditions
        HELPER.findIntegerPart(null, true);
    }

    @Test
    public void testFixNumericString() {

        Editable editable = new SpannableStringBuilder();

        setupEditable(editable, "-");
        Assert.assertFalse(HELPER.fixNumericString(editable));
        Assert.assertEquals("-", editable.toString());

        setupEditable(editable, "0");
        Assert.assertFalse(HELPER.fixNumericString(editable));
        Assert.assertEquals("0", editable.toString());

        setupEditable(editable, "111");
        Assert.assertFalse(HELPER.fixNumericString(editable));
        Assert.assertEquals("111", editable.toString());

        setupEditable(editable, "fdhfdjf");
        Assert.assertTrue(HELPER.fixNumericString(editable));
        Assert.assertEquals(",", editable.toString());

        setupEditable(editable, "-1");
        Assert.assertFalse(HELPER.fixNumericString(editable));
        Assert.assertEquals("-1", editable.toString());

        setupEditable(editable, "-rfff");
        Assert.assertTrue(HELPER.fixNumericString(editable));
        Assert.assertEquals("-,", editable.toString());

        setupEditable(editable, "--");
        Assert.assertTrue(HELPER.fixNumericString(editable));
        Assert.assertEquals("-,", editable.toString());

        setupEditable(editable, "fdhfdjf");
        Assert.assertTrue(HELPER.fixNumericString(editable));
        Assert.assertEquals(",", editable.toString());

        setupEditable(editable, ";-1234");
        Assert.assertTrue(HELPER.fixNumericString(editable));
        Assert.assertEquals(",1234", editable.toString());

        setupEditable(editable, "1-5");
        Assert.assertTrue(HELPER.fixNumericString(editable));
        Assert.assertEquals("1,5", editable.toString());

        setupEditable(editable, "12-");
        Assert.assertTrue(HELPER.fixNumericString(editable));
        Assert.assertEquals("12,", editable.toString());

        setupEditable(editable, "0,1");
        Assert.assertFalse(HELPER.fixNumericString(editable));
        Assert.assertEquals("0,1", editable.toString());

        setupEditable(editable, "0,");
        Assert.assertFalse(HELPER.fixNumericString(editable));
        Assert.assertEquals("0,", editable.toString());

        setupEditable(editable, ",");
        Assert.assertFalse(HELPER.fixNumericString(editable));
        Assert.assertEquals(",", editable.toString());

        setupEditable(editable, "-1234,355599");
        Assert.assertFalse(HELPER.fixNumericString(editable));
        Assert.assertEquals("-1234,355599", editable.toString());

        setupEditable(editable, "-1234,355599,");
        Assert.assertTrue(HELPER.fixNumericString(editable));
        Assert.assertEquals("-1234355599,", editable.toString());

        setupEditable(editable, ",,,");
        Assert.assertTrue(HELPER.fixNumericString(editable));
        Assert.assertEquals(",", editable.toString());

        setupEditable(editable, ",-1234");
        Assert.assertTrue(HELPER.fixNumericString(editable));
        Assert.assertEquals(",1234", editable.toString());

        setupEditable(editable, "adsdad,,sfdsfsfsf");
        Assert.assertTrue(HELPER.fixNumericString(editable));
        Assert.assertEquals(",", editable.toString());

        setupEditable(editable, "1.2");
        Assert.assertTrue(HELPER.fixNumericString(editable));
        Assert.assertEquals("1,2", editable.toString());

        setupEditable(editable, "12.,5");
        Assert.assertTrue(HELPER.fixNumericString(editable));
        Assert.assertEquals("12,5", editable.toString());

        setupEditable(editable, "12,.5");
        Assert.assertTrue(HELPER.fixNumericString(editable));
        Assert.assertEquals("12,5", editable.toString());

        setupEditable(editable, ",125");
        Assert.assertFalse(HELPER.fixNumericString(editable));
        Assert.assertEquals(",125", editable.toString());

        setupEditable(editable, "42");
        Assert.assertFalse(HELPER.fixNumericString(editable));
        Assert.assertEquals("42", editable.toString());
    }

    private void setupEditable(@NonNull Editable editable, @NonNull CharSequence charSequence) {
        editable.replace(0, editable.length(), charSequence);
    }

    @Test(expected = NullPointerException.class)
    public void testFixNumericStringNPE() {
        //noinspection ConstantConditions
        HELPER.fixNumericString(null);
    }

    @Test
    public void testStripLeadingZeros() {

        SpannableStringBuilder editable = new SpannableStringBuilder();

        setupEditable(editable, "");
        Assert.assertFalse(HELPER.stripLeadingZeros(editable));
        Assert.assertEquals("", editable.toString());

        setupEditable(editable, "0");
        Assert.assertFalse(HELPER.stripLeadingZeros(editable));
        Assert.assertEquals("0", editable.toString());

        setupEditable(editable, "0000");
        Assert.assertTrue(HELPER.stripLeadingZeros(editable));
        Assert.assertEquals("0", editable.toString());

        setupEditable(editable, "0,0");
        Assert.assertFalse(HELPER.stripLeadingZeros(editable));
        Assert.assertEquals("0,0", editable.toString());

        setupEditable(editable, "0000,000");
        Assert.assertTrue(HELPER.stripLeadingZeros(editable));
        Assert.assertEquals("0,000", editable.toString());

        setupEditable(editable, "-0");
        Assert.assertFalse(HELPER.stripLeadingZeros(editable));
        Assert.assertEquals("-0", editable.toString());

        setupEditable(editable, "-000");
        Assert.assertTrue(HELPER.stripLeadingZeros(editable));
        Assert.assertEquals("-0", editable.toString());

        setupEditable(editable, "-0,0");
        Assert.assertFalse(HELPER.stripLeadingZeros(editable));
        Assert.assertEquals("-0,0", editable.toString());

        setupEditable(editable, "-0000,000");
        Assert.assertTrue(HELPER.stripLeadingZeros(editable));
        Assert.assertEquals("-0,000", editable.toString());

        setupEditable(editable, "42");
        Assert.assertFalse(HELPER.stripLeadingZeros(editable));
        Assert.assertEquals("42", editable.toString());

        setupEditable(editable, "042");
        Assert.assertTrue(HELPER.stripLeadingZeros(editable));
        Assert.assertEquals("42", editable.toString());

        setupEditable(editable, "00042");
        Assert.assertTrue(HELPER.stripLeadingZeros(editable));
        Assert.assertEquals("42", editable.toString());

        setupEditable(editable, "-42");
        Assert.assertFalse(HELPER.stripLeadingZeros(editable));
        Assert.assertEquals("-42", editable.toString());

        setupEditable(editable, "-00042");
        Assert.assertTrue(HELPER.stripLeadingZeros(editable));
        Assert.assertEquals("-42", editable.toString());

        setupEditable(editable, "-420000");
        Assert.assertFalse(HELPER.stripLeadingZeros(editable));
        Assert.assertEquals("-420000", editable.toString());

        setupEditable(editable, "-0004200,000");
        Assert.assertTrue(HELPER.stripLeadingZeros(editable));
        Assert.assertEquals("-4200,000", editable.toString());

        setupEditable(editable, ",0");
        Assert.assertFalse(HELPER.stripLeadingZeros(editable));
        Assert.assertEquals(",0", editable.toString());

        setupEditable(editable, "0000ABC");
        Assert.assertTrue(HELPER.stripLeadingZeros(editable));
        Assert.assertEquals("ABC", editable.toString());

        setupEditable(editable, "-0000ABC");
        Assert.assertTrue(HELPER.stripLeadingZeros(editable));
        Assert.assertEquals("-ABC", editable.toString());

        setupEditable(editable, "-,00000");
        Assert.assertFalse(HELPER.stripLeadingZeros(editable));
        Assert.assertEquals("-,00000", editable.toString());

        setupEditable(editable, "A00000");
        Assert.assertFalse(HELPER.stripLeadingZeros(editable));
        Assert.assertEquals("A00000", editable.toString());

        setupEditable(editable, "000000A0000");
        Assert.assertTrue(HELPER.stripLeadingZeros(editable));
        Assert.assertEquals("A0000", editable.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testStripLeadingZerosNPE() {
        //noinspection ConstantConditions
        HELPER.stripLeadingZeros(null);
    }
}
