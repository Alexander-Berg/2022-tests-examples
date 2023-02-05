package ru.yandex.yandexbus.inhouse.common.locale;


import org.junit.Assert;
import org.junit.Test;

public class RublesCurrencyUnitTest {

    @Test
    public void priceWithTwoDigitsAtFraction() throws Exception {

        String result = CurrencyFormatter.formatPrice(100.52);
        String expected = "100,52 \u20BD";

        Assert.assertEquals(expected, result);
    }

    @Test
    public void priceWithOneDigitsAtFraction() throws Exception {

        String result = CurrencyFormatter.formatPrice(100.5);
        String expected = "100,50 \u20BD";

        Assert.assertEquals(expected, result);
    }

    @Test
    public void priceWithoutDigitsAtFraction() throws Exception {

        String result = CurrencyFormatter.formatPrice(100);
        String expected = "100,00 \u20BD";

        Assert.assertEquals(expected, result);
    }
}