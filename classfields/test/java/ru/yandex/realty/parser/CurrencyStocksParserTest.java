package ru.yandex.realty.parser;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.common.serialization.parser.xbi.XbiFactory;
import ru.yandex.realty.domain.currency.ConversionsParseResult;
import ru.yandex.realty.domain.currency.CurrencyExchangeRate;
import ru.yandex.realty.model.serialization.extractor.CurrencyExtractor;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;


/**
 * @author: rmuzhikov
 */
public class CurrencyStocksParserTest {
    private XbiFactory xbiFactory;

    @Before
    public void setUp() throws Exception {
        xbiFactory = new XbiFactory();
        xbiFactory.registerDefaultExtractors();
        xbiFactory.registerExtractor(new CurrencyExtractor());
    }

    @Test
    public void testParse() throws Exception {
        File conversionsFile = new File("../realty-common/test-data/currency/currency-conversion.xml");
        ConversionsParseResult parseResult = CurrencyConversionParser.parse(conversionsFile, xbiFactory);
        File stocksFile = new File("../realty-common/test-data/currency/stocks.xml");
        List<CurrencyExchangeRate> exchangeRates = CurrencyStocksParser.parse(stocksFile, parseResult.currencyConversions);
        assertEquals(3, exchangeRates.size());

    }
}
