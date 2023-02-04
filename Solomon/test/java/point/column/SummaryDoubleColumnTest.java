package ru.yandex.solomon.model.point.column;

import org.junit.Test;

import ru.yandex.monlib.metrics.summary.ImmutableSummaryDoubleSnapshot;
import ru.yandex.monlib.metrics.summary.SummaryDoubleSnapshot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static ru.yandex.solomon.model.point.column.SummaryDoubleColumn.merge;

/**
 * @author Vladimir Gordiychuk
 */
public class SummaryDoubleColumnTest {

    private static SummaryDoubleSnapshot summary(long count, double sum, double max, double min, double last) {
        return new ImmutableSummaryDoubleSnapshot(count, sum, min, max, last);
    }

    @Test
    public void mergeWhenLeftNull() {
        SummaryDoubleSnapshot right = summary(10, 42, 20, 1, 2.5);
        SummaryDoubleSnapshot result = merge(null, right);

        assertEquals(right, result);
    }

    @Test
    public void mergeWhenRightNull() {
        SummaryDoubleSnapshot left = summary(10, 42, 5, 2, 2.2);
        SummaryDoubleSnapshot result = merge(left, null);

        assertEquals(left, result);
    }

    @Test
    public void mergeBothNull() {
        SummaryDoubleSnapshot result = merge(null, null);
        assertNull(result);
    }

    @Test
    public void mergeWithLeftEmpty() {
        SummaryDoubleSnapshot left = ImmutableSummaryDoubleSnapshot.EMPTY;
        SummaryDoubleSnapshot right = summary(5, 25, 10, 1, 1.5);
        SummaryDoubleSnapshot result = merge(left, right);

        assertEquals(right, result);
    }

    @Test
    public void mergeWithRightEmpty() {
        var left = summary(5, 25, 10, 1, 1.3);
        var right = ImmutableSummaryDoubleSnapshot.EMPTY;
        var expect = summary(5, 25, 10, 1, 0);

        var result = merge(left, right);

        assertEquals(expect, result);
    }

    @Test
    public void mergeSumOnly() {
        var left = summary(0, 32, 0, 0, 0);
        var right = summary(0, 32, 0, 0, 0);
        var expect = summary(0, 64, 0, 0, 0);
        var result = merge(left, right);

        assertEquals(result, expect);
    }

    @Test
    public void mergeFew() {
        SummaryDoubleSnapshot one = summary(10, 100, 50, 2, 4);
        SummaryDoubleSnapshot two = summary(5, 25, 10, 1, 8);
        SummaryDoubleSnapshot tree = summary(15, 50, 8, 4, 5.5);

        SummaryDoubleSnapshot expected = summary(30, 175, 50, 1, 5.5);
        SummaryDoubleSnapshot result = merge(merge(one, two), tree);

        assertEquals(expected, result);
    }
}
