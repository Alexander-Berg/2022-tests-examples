package ru.yandex.market.money;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;

import ru.yandex.market.feature.money.formatter.CurrencyFormatter;
import ru.yandex.market.clean.presentation.formatter.MoneyFormatter;
import ru.yandex.market.feature.money.formatter.FormatSymbolsProvider;
import ru.yandex.market.feature.money.formatter.MoneyFormatSymbols;
import ru.yandex.market.clean.data.mapper.money.MoneyMapper;
import ru.yandex.market.data.money.parser.MoneyAmountParser;
import ru.yandex.market.domain.money.model.Currency;
import ru.yandex.market.domain.money.model.Money;
import ru.yandex.market.common.android.ResourcesManager;
import ru.yandex.market.utils.Characters;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MoneyFormatterTest {

    public static final Currency CURRENCY = Currency.RUR;

    private static final char GROUPING_SEPARATOR = Characters.THIN_SPACE;
    private static final char DECIMAL_SEPARATOR = Characters.COMMA;
    private static final char CURRENCY_SEPARATOR = Characters.SPACE;
    private static final char MINUS = Characters.MINUS;

    private static final MoneyFormatSymbols SYMBOLS = new MoneyFormatSymbols(
            GROUPING_SEPARATOR,
            DECIMAL_SEPARATOR,
            CURRENCY_SEPARATOR,
            MINUS
    );

    @Mock
    ResourcesManager resourcesManager;

    @Mock
    MoneyAmountParser moneyAmountParser;

    @Mock
    FormatSymbolsProvider symbolsProvider;

    @Mock
    CurrencyFormatter currencyFormatter;

    @Mock
    MoneyMapper moneyMapper;

    @InjectMocks
    MoneyFormatter moneyFormatter;

    @Test
    public void testFormatBigIntegerValue() {
        when(symbolsProvider.getMoneyFormatSymbols()).thenReturn(SYMBOLS);
        final String formattedCurrency = "Руб";
        when(currencyFormatter.format(CURRENCY)).thenReturn(formattedCurrency);
        final Money money = new Money(new BigDecimal(100_000_000), CURRENCY);
        final String expected = "100" + GROUPING_SEPARATOR + "000" + GROUPING_SEPARATOR + "000" +
                CURRENCY_SEPARATOR + formattedCurrency;

        final String result = moneyFormatter.formatDeliveryPrice(money);
        assertThat(result, equalTo(expected));
    }
}
