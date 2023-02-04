package ru.yandex.realty.model.history;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.realty.model.offer.Money;

import static org.junit.Assert.assertEquals;

/**
 * Created by abulychev on 23.12.16.
 */
public class PriceHistoryTest {

    private final static Duration THREE_DAYS = Duration.standardDays(3);
    private final static Duration TWO_DAYS = Duration.standardDays(2);
    private final static Duration ONE_DAY = Duration.standardDays(1);

    private final static PriceHistory DEFAULT_HISTORY = PriceHistory.EMPTY
            .append(getPastTime(THREE_DAYS), getMoney(9_000_000L))
            .append(getPastTime(TWO_DAYS), getMoney(10_000_000L));

    private static Instant getPastTime(Duration duration) {
        return new Instant().minus(duration);
    }

    private static Money getMoney(long scaledValue) {
        return Money.scaledOf(Currency.RUR, scaledValue);
    }

    private void assertHistory(PriceHistory history, int size, long lastValue) {
        assertEquals(size, history.getHistory().size());
        assertEquals(lastValue, history.getLastValue().getScaled());
    }

    @Test
    public void insignificantChangeAdded() {
        PriceHistory updatedHistory = DEFAULT_HISTORY.append(getMoney(10_001_000L));
        assertHistory(updatedHistory, 3, 10_001_000L);
    }

    @Test
    public void significantChangeAdded() {
        PriceHistory updatedHistory = DEFAULT_HISTORY.append(getMoney(12_000_000L));
        assertHistory(updatedHistory, 3, 12_000_000L);
    }

    @Test
    public void insignificantChangeMerged() {
        PriceHistory oldHistory = DEFAULT_HISTORY.append(getPastTime(ONE_DAY), getMoney(10_001_000L));
        PriceHistory updatedHistory = oldHistory.append(getMoney(10_002_000L));
        assertHistory(updatedHistory, 3, 10_002_000L);
    }

    @Test
    public void significantChangeNotMerged() {
        PriceHistory oldHistory = DEFAULT_HISTORY.append(getPastTime(ONE_DAY), getMoney(10_001_000L));
        PriceHistory updatedHistory = oldHistory.append(getMoney(12_000_000L));
        assertHistory(updatedHistory, 4, 12_000_000L);
    }

    @Test
    public void insignificantChangesNeutralised() {
        PriceHistory oldHistory = DEFAULT_HISTORY.append(getPastTime(ONE_DAY), getMoney(10_001_000L));
        PriceHistory updatedHistory = oldHistory.append(getMoney(10_000_000L));
        assertHistory(updatedHistory, 2, 10_000_000L);
    }

    @Test
    public void trendIsUnchanged() {
        PriceHistory history = PriceHistory.EMPTY.append(getMoney(5_000_000L));
        assertEquals(PriceTrend.UNCHANGED, history.getTrend());
    }

    @Test
    public void trendIsIncreased() {
        assertEquals(PriceTrend.INCREASED, DEFAULT_HISTORY.getTrend());
    }

    @Test
    public void trendIsDecreased() {
        PriceHistory history = DEFAULT_HISTORY.append(getMoney(8_000_000L));
        assertEquals(PriceTrend.DECREASED, history.getTrend());
    }

    @Test
    public void trendIsIncreasedForInsignificant() {
        PriceHistory history = DEFAULT_HISTORY
                .append(getPastTime(ONE_DAY), getMoney(8_000_000L))
                .append(getMoney(8_001_000L));
        assertEquals(PriceTrend.INCREASED, history.getTrend());
    }

    @Test
    public void trendIsDecreasedForInsignificant() {
        PriceHistory history = DEFAULT_HISTORY.append(getMoney(9_999_000L));
        assertEquals(PriceTrend.DECREASED, history.getTrend());
    }
}
