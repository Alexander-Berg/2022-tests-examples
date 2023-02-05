package ru.yandex.market.money;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.feature.money.formatter.FormatSymbolsProvider;
import ru.yandex.market.feature.money.formatter.MoneyFormatSymbols;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class FormatSymbolsProviderTest {

    private FormatSymbolsProvider provider;

    @Before
    public void setUp() {
        provider = new FormatSymbolsProvider();
    }

    @Test
    public void testProviderProvidesValidSymbols() {
        final MoneyFormatSymbols symbols = provider.getMoneyFormatSymbols();
        assertThat(symbols, notNullValue());
    }
}