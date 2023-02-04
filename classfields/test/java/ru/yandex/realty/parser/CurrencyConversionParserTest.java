package ru.yandex.realty.parser;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.common.serialization.parser.xbi.XbiFactory;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.realty.domain.currency.BaseCurrency;
import ru.yandex.realty.domain.currency.ConversionsParseResult;
import ru.yandex.realty.domain.currency.CurrencyConversion;
import ru.yandex.realty.model.serialization.extractor.CurrencyExtractor;

import java.io.File;

import static org.junit.Assert.assertEquals;

/**
 * @author: rmuzhikov
 */
public class CurrencyConversionParserTest {
    private XbiFactory xbiFactory;

    @Before
    public void setUp() throws Exception {
        xbiFactory = new XbiFactory();
        xbiFactory.registerDefaultExtractors();
        xbiFactory.registerExtractor(new CurrencyExtractor());
    }

    @Test
    public void testParse() throws Exception {
        File file = new File("../realty-common/test-data/currency/currency-conversion.xml");
        ConversionsParseResult parseResult = CurrencyConversionParser.parse(file, xbiFactory);
        assertEquals(3, parseResult.currencyConversions.size());

//        <conversion stock-id="1"  from="USD" to="RUB" multiplier="1" />
        CurrencyConversion currencyConversion = new CurrencyConversion();
        currencyConversion.setStockId(1);
        currencyConversion.setMultiplier(1);
        currencyConversion.setFrom(Currency.USD);
        currencyConversion.setTo(Currency.RUR);
        assertEquals(currencyConversion, parseResult.currencyConversions.get(0));

//        <conversion stock-id="23" from="EUR" to="RUB" multiplier="1" />
        currencyConversion.setStockId(23);
        currencyConversion.setFrom(Currency.EUR);
        currencyConversion.setTo(Currency.RUR);
        assertEquals(currencyConversion, parseResult.currencyConversions.get(1));

//        <conversion stock-id="24" from="GBR" to="RUB" multiplier="1" />
        currencyConversion.setStockId(24);
        currencyConversion.setFrom(Currency.GBP);
        currencyConversion.setTo(Currency.RUR);
        assertEquals(currencyConversion, parseResult.currencyConversions.get(2));

        assertEquals(2, parseResult.baseCurrencies.size());

        BaseCurrency baseCurrency = new BaseCurrency();
        baseCurrency.setCurrency(Currency.USD);

        assertEquals(baseCurrency, parseResult.baseCurrencies.get(0));
        baseCurrency.setCurrency(Currency.RUR);
        baseCurrency.setGeoId(225);
        assertEquals(baseCurrency, parseResult.baseCurrencies.get(1));

    }
}
