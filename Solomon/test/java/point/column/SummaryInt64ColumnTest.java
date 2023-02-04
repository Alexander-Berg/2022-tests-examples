package ru.yandex.solomon.model.point.column;

import org.junit.Test;

import ru.yandex.monlib.metrics.summary.ImmutableSummaryInt64Snapshot;
import ru.yandex.monlib.metrics.summary.SummaryInt64Snapshot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static ru.yandex.solomon.model.point.column.SummaryInt64Column.merge;

/**
 * @author Vladimir Gordiychuk
 */
public class SummaryInt64ColumnTest {

    private static SummaryInt64Snapshot summary(long count, long sum, long max, long min, long last) {
        return new ImmutableSummaryInt64Snapshot(count, sum, min, max, last);
    }

    @Test
    public void mergeWhenLeftNull() {
        SummaryInt64Snapshot right = summary(10, 42, 20, 1, 4);
        SummaryInt64Snapshot result = merge(null, right);

        assertEquals(right, result);
    }

    @Test
    public void mergeWhenRightNull() {
        SummaryInt64Snapshot left = summary(10, 42, 5, 2, 3);
        SummaryInt64Snapshot result = merge(left, null);

        assertEquals(left, result);
    }

    @Test
    public void mergeBothNull() {
        SummaryInt64Snapshot result = merge(null, null);
        assertNull(result);
    }

    @Test
    public void mergeWithLeftEmpty() {
        SummaryInt64Snapshot left = ImmutableSummaryInt64Snapshot.EMPTY;
        SummaryInt64Snapshot right = summary(5, 25, 10, 1, 2);
        SummaryInt64Snapshot result = merge(left, right);

        assertEquals(right, result);
    }

    @Test
    public void mergeWithRightEmpty() {
        var left = summary(5, 25, 10, 1, 2);
        var right = ImmutableSummaryInt64Snapshot.EMPTY;
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
        SummaryInt64Snapshot one = summary(10, 100, 50, 2, 4);
        SummaryInt64Snapshot two = summary(5, 25, 10, 1, 8);
        SummaryInt64Snapshot tree = summary(15, 50, 8, 4, 5);

        SummaryInt64Snapshot expected = summary(30, 175, 50, 1, 5);
        SummaryInt64Snapshot result = merge(merge(one, two), tree);

        assertEquals(expected, result);
    }

}
