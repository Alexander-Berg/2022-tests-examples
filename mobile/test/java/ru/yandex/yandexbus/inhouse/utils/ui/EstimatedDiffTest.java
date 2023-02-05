package ru.yandex.yandexbus.inhouse.utils.ui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import ru.yandex.yandexbus.inhouse.BaseTest;
import ru.yandex.yandexbus.inhouse.utils.datetime.DateTime;

public class EstimatedDiffTest extends BaseTest {

    private static final long TEST_CURRENT_TIME_MILLIS = 1516900_202_000L;

    @Test
    public void ifDiffIsNegativeThenReturnZero() {
        DateTime estimatedBeforeNow = new DateTime(1516900_000_000L);

        final int actual = EstimatedDiffUtils.getDiffMinutes(TEST_CURRENT_TIME_MILLIS, estimatedBeforeNow.getUtcMillis());
        final int expected = 0;

        assertEquals(expected, actual);
    }

    @Test
    public void diffIsPositive() {
        DateTime estimatedBeforeNow = new DateTime(1516900_502_000L);

        final int actual = EstimatedDiffUtils.getDiffMinutes(TEST_CURRENT_TIME_MILLIS, estimatedBeforeNow.getUtcMillis());
        final int expected = 5;

        assertEquals(expected, actual);
    }
}
