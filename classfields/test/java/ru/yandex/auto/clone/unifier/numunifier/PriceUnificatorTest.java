package ru.yandex.auto.clone.unifier.numunifier;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.yandex.auto.core.model.PriceHistoryEntry;
import ru.yandex.auto.core.model.UnifiedCarInfo;
import ru.yandex.auto.core.stock.http.*;
import ru.yandex.common.util.currency.RateSource;

/** @author obolshakova */
public class PriceUnificatorTest {

  private static PriceUnificator priceUnificator;
  private static PriceConverter priceConverter;

  @NotNull private final UnifiedCarInfo info = new UnifiedCarInfo("1");

  @BeforeClass
  public static void setUp() {
    priceUnificator = new PriceUnificator();
    priceConverter = new FakePriceConverter();
    priceUnificator.setPriceConverter(priceConverter);
  }

  @Test
  public void testRubleToRuble() {
    info.setCurrencyType("RUR");
    info.setPrice(779277);
    priceUnificator.modify(info);
    assertEquals(new Integer(779277), info.getPrice());
  }

  @Test
  public void testUsdToRuble() throws CurrencyNotFoundException, CurrencyTechnicalException {
    ConversionRate rate = priceConverter.getConversionRate(CurrencyConversion.USD_TO_RUR);

    info.setCurrencyType("USD");
    info.setPrice(779277);
    priceUnificator.modify(info);
    assertEquals(
        new Integer((int) Math.round(779277 * rate.getValue().doubleValue())), info.getPrice());
  }

  @Test
  public void testEuroToRuble() throws CurrencyNotFoundException, CurrencyTechnicalException {
    ConversionRate rate = priceConverter.getConversionRate(CurrencyConversion.EUR_TO_RUR);

    info.setCurrencyType(Currency.EUR.name());
    info.setPrice(779277);
    priceUnificator.modify(info);
    assertEquals(
        new Integer((int) Math.round(779277 * rate.getValue().doubleValue())), info.getPrice());
  }

  @Test
  public void testNullToRuble() {
    info.setCurrencyType(null);
    info.setPrice(779277);
    priceUnificator.modify(info);
    assertEquals(new Integer(779277), info.getPrice());
  }

  @Test(expected = IllegalStateException.class)
  public void testNoSuchCurrency() {
    info.setCurrencyType("abracadabra");
    info.setPrice(779277);
    priceUnificator.modify(info);
  }

  @Test
  public void testConvertionOfPriceHistoryToRubles() {
    final List<PriceHistoryEntry> priceHistoryEntries = generatePriceHistory("RUR", "USD", "EUR");
    info.setPrice(10);
    info.setCurrencyType(Currency.RUR.name());
    info.setPriceHistoryEntries(priceHistoryEntries);
    priceUnificator.modify(info);
    assertEquals(generatePriceHistoryAfterConvertion(), info.getPriceHistoryEntries());
  }

  /** Everything should be OK, even with null history */
  @Test
  public void testConvertionOfPriceHistoryToRublesWithNullHistory() {
    info.setPrice(10);
    info.setCurrencyType(Currency.RUR.name());
    info.setPriceHistoryEntries(null);
    priceUnificator.modify(info);
  }

  @Test(expected = IllegalStateException.class)
  public void testConvertionOfPriceHistoryToRublesWithInvalidCurrency() {
    final List<PriceHistoryEntry> priceHistoryEntries =
        generatePriceHistory("RUR", "INVALID_CURRENCY", "EUR");
    info.setPrice(10);
    info.setCurrencyType(Currency.RUR.name());
    info.setPriceHistoryEntries(priceHistoryEntries);
    priceUnificator.modify(info);
  }

  @NotNull
  private List<PriceHistoryEntry> generatePriceHistory(@NotNull final String... currencies) {
    final List<PriceHistoryEntry> priceHistoryEntries = new ArrayList<>();
    for (String currency : currencies) {
      priceHistoryEntries.add(new PriceHistoryEntry(10, 1000L, currency));
    }
    return priceHistoryEntries;
  }

  @NotNull
  private List<PriceHistoryEntry> generatePriceHistoryAfterConvertion() {
    final List<PriceHistoryEntry> priceHistoryEntries = new ArrayList<>();
    priceHistoryEntries.add(new PriceHistoryEntry(10, 1000L, Currency.RUR.name()));
    priceHistoryEntries.add(new PriceHistoryEntry(300, 1000L, Currency.RUR.name()));
    priceHistoryEntries.add(new PriceHistoryEntry(350, 1000L, Currency.RUR.name()));
    return priceHistoryEntries;
  }

  // helper stuff

  private static class FakePriceConverter implements PriceConverter {
    private static final int USD_COURSE = 30;
    private static final int EUR_COURSE = 35;

    public double convertToCurrency(
        Currency targetCurrency, double value, @NotNull Currency currency)
        throws CurrencyNotFoundException {
      if (currency.equals(Currency.RUR)) {
        return value;
      } else if (currency.equals(Currency.USD)) {
        return value * USD_COURSE;
      } else if (currency.equals(Currency.EUR)) {
        return value * EUR_COURSE;
      }
      throw new CurrencyNotFoundException("");
    }

    @Override
    public double convertFromCurrency(
        Currency sourceCurrency, double value, ru.yandex.auto.core.stock.http.Currency currency)
        throws CurrencyNotFoundException, CurrencyTechnicalException {
      throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public ConversionRate getConversionRate(@NotNull CurrencyConversion conversion)
        throws CurrencyNotFoundException, CurrencyTechnicalException {
      if (conversion.equals(CurrencyConversion.USD_TO_RUR)) {
        return new ConversionRate(
            conversion, RateSource.CBRF_DAILY, new BigDecimal(USD_COURSE), null);
      } else if (conversion.equals(CurrencyConversion.EUR_TO_RUR)) {
        return new ConversionRate(
            conversion, RateSource.CBRF_DAILY, new BigDecimal(EUR_COURSE), null);
      }

      throw new CurrencyNotFoundException("");
    }
  }
}
