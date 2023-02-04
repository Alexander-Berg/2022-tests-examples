package ru.yandex.solomon.model.timeseries;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static ru.yandex.solomon.model.point.AggrPoints.point;

/**
 * @author Vladimir Gordiychuk
 */
public class FilteringAfterAggrGraphDataIteratorTest {

    @Test
    public void empty() {
        assertEquals(AggrGraphDataArrayList.empty(), filter(AggrGraphDataArrayList.empty(), 42));
    }

    @Test
    public void filterAll() {
        long ts0 = System.currentTimeMillis();
        var source = AggrGraphDataArrayList.of(
            point(ts0, 1),
            point(ts0 + 10_000, 2),
            point(ts0 + 20_000, 3));

        assertEquals(AggrGraphDataArrayList.empty(), filter(source, ts0 - 100));
        assertEquals(AggrGraphDataArrayList.empty(), filter(source, ts0));
    }

    @Test
    public void filterPart() {
        long ts0 = System.currentTimeMillis();
        var source = AggrGraphDataArrayList.of(
            point(ts0, 1),
            point(ts0 + 10_000, 2),
            point(ts0 + 20_000, 3));

        assertEquals(
            AggrGraphDataArrayList.of(
                point(ts0, 1)),
            filter(source, ts0 + 10_000));

        assertEquals(
            AggrGraphDataArrayList.of(
                point(ts0, 1),
                point(ts0 + 10_000, 2)),
            filter(source, ts0 + 20_000));
    }

    @Test
    public void filterNone() {
        long ts0 = System.currentTimeMillis();
        var source = AggrGraphDataArrayList.of(
            point(ts0, 1),
            point(ts0 + 10_000, 2),
            point(ts0 + 20_000, 3));

        assertEquals(source, filter(source, ts0 + 30_000));
    }

    private AggrGraphDataArrayList filter(AggrGraphDataArrayList source, long tsMillis) {
        return AggrGraphDataArrayList.of(new FilteringAfterAggrGraphDataIterator(source.iterator(), tsMillis));
    }
}
