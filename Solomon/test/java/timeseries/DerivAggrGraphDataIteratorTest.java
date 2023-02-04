package ru.yandex.solomon.model.timeseries;

import org.junit.Test;

import ru.yandex.solomon.model.point.column.StockpileColumn;

import static org.junit.Assert.assertEquals;
import static ru.yandex.solomon.model.point.AggrPoints.lpoint;
import static ru.yandex.solomon.model.point.AggrPoints.point;

/**
 * @author Vladimir Gordiychuk
 */
public class DerivAggrGraphDataIteratorTest {
    @Test
    public void empty() {
        AggrGraphDataArrayList result = deriv(AggrGraphDataArrayList.empty());
        assertEquals(AggrGraphDataArrayList.empty(), result);
    }

    @Test
    public void derivSameSource() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                lpoint("2018-08-07T14:36:00Z", 42),
                lpoint("2018-08-07T14:36:15Z", 42),
                lpoint("2018-08-07T14:36:30Z", 42),
                lpoint("2018-08-07T14:36:45Z", 42));

        AggrGraphDataArrayList result = deriv(source);

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                // absent first point it's ok
                point("2018-08-07T14:36:15Z", 0),
                point("2018-08-07T14:36:30Z", 0),
                point("2018-08-07T14:36:45Z", 0));

        assertEquals(expected, result);
    }

    @Test
    public void derivSameResult() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                lpoint("2018-08-07T14:36:00Z", 100),
                lpoint("2018-08-07T14:36:10Z", 200),
                lpoint("2018-08-07T14:36:20Z", 300),
                lpoint("2018-08-07T14:36:30Z", 400));

        AggrGraphDataArrayList result = deriv(source);

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                // absent first point it's ok
                point("2018-08-07T14:36:10Z", 10),
                point("2018-08-07T14:36:20Z", 10),
                point("2018-08-07T14:36:30Z", 10));

        assertEquals(expected, result);
    }

    @Test
    public void derivNotStableGrow() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                lpoint("2018-08-07T14:36:00Z", 100),
                lpoint("2018-08-07T14:36:10Z", 800),
                lpoint("2018-08-07T14:36:20Z", 1000),
                lpoint("2018-08-07T14:36:30Z", 1500));

        AggrGraphDataArrayList result = deriv(source);

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                // absent first point it's ok
                point("2018-08-07T14:36:10Z", 70),
                point("2018-08-07T14:36:20Z", 20),
                point("2018-08-07T14:36:30Z", 50));

        assertEquals(expected, result);
    }

    @Test
    public void derivOnResetCounter() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                lpoint("2018-08-07T14:36:00Z", 100),
                lpoint("2018-08-07T14:36:10Z", 200),
                lpoint("2018-08-07T14:36:20Z", 300),
                // gap here
                lpoint("2018-08-07T14:36:40Z", 10),
                lpoint("2018-08-07T14:36:50Z", 80),
                lpoint("2018-08-07T14:37:00Z", 300));

        AggrGraphDataArrayList result = deriv(source);

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                // absent
                point("2018-08-07T14:36:10Z", 10),
                point("2018-08-07T14:36:20Z", 10),
                point("2018-08-07T14:36:40Z", 10),
                point("2018-08-07T14:36:50Z", 7),
                point("2018-08-07T14:37:00Z", 22));

        assertEquals(expected, result);
    }

    @Test
    public void derivConsiderTime() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                lpoint("2018-08-07T14:30:00Z", 1_000_000),
                lpoint("2018-08-07T14:40:00Z", 7_000_000),
                lpoint("2018-08-07T14:50:00Z", 19_000_000));

        AggrGraphDataArrayList result = deriv(source);

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                // absent
                point("2018-08-07T14:40:00Z", 10_000),
                point("2018-08-07T14:50:00Z", 20_000));

        assertEquals(expected, result);
    }

    @Test
    public void countReduceIntoAggregate() {
        // every host add 1k into aggregate or 100 rps
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                lpoint("2018-08-07T14:00:00Z", 10_000, true, 10),
                lpoint("2018-08-07T14:00:10Z", 20_000, true, 10),
                lpoint("2018-08-07T14:00:20Z", 30_000, true, 10),
                lpoint("2018-08-07T14:00:30Z", 39_000, true, 9),
                lpoint("2018-08-07T14:00:40Z", 48_000, true, 9),
                lpoint("2018-08-07T14:00:50Z", 57_000, true, 9));

        AggrGraphDataArrayList result = deriv(source);
        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                // absent
                point("2018-08-07T14:00:10Z", 1000, true, 10),
                point("2018-08-07T14:00:20Z", 1000, true, 10),
                point("2018-08-07T14:00:30Z", 1000, true, 10),
                point("2018-08-07T14:00:40Z", 900, true, 9),
                point("2018-08-07T14:00:50Z", 900, true, 9));

        assertEquals(expected, result);
    }

    @Test
    public void countUpAndDownIntoAggregate() {
        // every host add 1k into aggregate or 100 rps
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
            lpoint("2018-08-07T14:00:00Z", 10_000, true, 10),
            lpoint("2018-08-07T14:00:10Z", 20_000, true, 10),
            lpoint("2018-08-07T14:00:20Z", 30_000, true, 10),
            // one of host unavailable 1 fetch iteration
            lpoint("2018-08-07T14:00:30Z", 39_000, true, 9),
            lpoint("2018-08-07T14:00:40Z", 50_000, true, 10),
            lpoint("2018-08-07T14:00:50Z", 60_000, true, 10),
            // two of host unavailable 2 fetch iteration
            lpoint("2018-08-07T14:01:00Z", 69_000, true, 9),
            lpoint("2018-08-07T14:01:10Z", 78_000, true, 9),
            lpoint("2018-08-07T14:01:20Z", 90_000, true, 10),
            lpoint("2018-08-07T14:01:30Z", 100_000, true, 10)
            );

        AggrGraphDataArrayList result = deriv(source);
        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
            // absent
            point("2018-08-07T14:00:10Z", 1000, true, 10),
            point("2018-08-07T14:00:20Z", 1000, true, 10),
            point("2018-08-07T14:00:30Z", 1000, true, 10),
            point("2018-08-07T14:00:40Z", 1000, true, 10),
            point("2018-08-07T14:00:50Z", 1000, true, 10),
            point("2018-08-07T14:01:00Z", 1000, true, 10),
            point("2018-08-07T14:01:10Z", 900, true, 9),
            point("2018-08-07T14:01:20Z", 900, true, 9),
            point("2018-08-07T14:01:30Z", 1000, true, 10));

        assertEquals(expected, result);
    }

    @Test
    public void countReduceByHugeAmountAggregate() {
        // 10 hosts add by 100 into aggregate or 10rps, one host add 10000 into aggregate or 1000rps
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                lpoint("2018-08-07T14:00:00Z", 11_000, true, 11),
                lpoint("2018-08-07T14:00:10Z", 22_000, true, 11),
                lpoint("2018-08-07T14:00:20Z", 33_000, true, 11),
                lpoint("2018-08-07T14:00:30Z", 4_000, true, 10),
                lpoint("2018-08-07T14:00:40Z", 5_000, true, 10),
                lpoint("2018-08-07T14:00:50Z", 6_000, true, 10));

        AggrGraphDataArrayList result = deriv(source);
        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                // absent
                point("2018-08-07T14:00:10Z", 1100, true, 11),
                point("2018-08-07T14:00:20Z", 1100, true, 11),
                point("2018-08-07T14:00:30Z", 1100, true, 11),
                point("2018-08-07T14:00:40Z", 100, true, 10),
                point("2018-08-07T14:00:50Z", 100, true, 10));

        assertEquals(expected, result);
    }

    @Test
    public void countIncreaseIntoAggregate() {
        // every host add 1k into aggregate or 100 rps
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                lpoint("2018-08-07T14:00:00Z", 9_000, true, 9),
                lpoint("2018-08-07T14:00:10Z", 18_000, true, 9),
                lpoint("2018-08-07T14:00:20Z", 27_000, true, 9),
                lpoint("2018-08-07T14:00:30Z", 37_000, true, 10),
                lpoint("2018-08-07T14:00:40Z", 47_000, true, 10),
                lpoint("2018-08-07T14:00:50Z", 57_000, true, 10));

        AggrGraphDataArrayList result = deriv(source);
        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                // absent
                point("2018-08-07T14:00:10Z", 900, true, 9),
                point("2018-08-07T14:00:20Z", 900, true, 9),
                // lost one point because new host added to aggregate, it's sad, but allow reduce spikes on graph
                point("2018-08-07T14:00:30Z", 900, true, 9),
                point("2018-08-07T14:00:40Z", 1000, true, 10),
                point("2018-08-07T14:00:50Z", 1000, true, 10));

        assertEquals(expected, result);
    }

    @Test
    public void countIncreaseIntoAggregateByHugeHost() {
        // 10 hosts add by 100 into aggregate or 10rps, one host add 10000 into aggregate or 1000rps
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                lpoint("2018-08-07T14:00:00Z", 1_000, true, 10),
                lpoint("2018-08-07T14:00:10Z", 2_000, true, 10),
                lpoint("2018-08-07T14:00:20Z", 3_000, true, 10),
                lpoint("2018-08-07T14:00:30Z", 44_000, true, 11),
                lpoint("2018-08-07T14:00:40Z", 55_000, true, 11),
                lpoint("2018-08-07T14:00:50Z", 66_000, true, 11));

        AggrGraphDataArrayList result = deriv(source);
        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                // absent
                point("2018-08-07T14:00:10Z", 100, true, 10),
                point("2018-08-07T14:00:20Z", 100, true, 10),
                // absent because we lost host add a most of value to aggregate
                point("2018-08-07T14:00:30Z", 100, true, 10),
                point("2018-08-07T14:00:40Z", 1100, true, 11),
                point("2018-08-07T14:00:50Z", 1100, true, 11));

        assertEquals(expected, result);
    }

    @Test
    public void dropPointAfterGap() {
        // every host add 1k into aggregate or 100 rps
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                lpoint("2018-08-07T14:00:00Z", 10_000, 10_000),
                lpoint("2018-08-07T14:00:10Z", 20_000, 10_000),
                // gap here
                lpoint("2018-08-07T14:01:40Z", 50_000, 10_000),
                lpoint("2018-08-07T14:01:50Z", 60_000, 10_000));

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                // absent
                point("2018-08-07T14:00:10Z", 1000),
                point("2018-08-07T14:01:40Z", 1000),
                point("2018-08-07T14:01:50Z", 1000));

        AggrGraphDataArrayList result = deriv(source).cloneWithMask(StockpileColumn.TS.mask() | StockpileColumn.VALUE.mask());
        assertEquals(expected, result);
    }

    @Test
    public void derivUnsignedLong() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                lpoint("2018-08-07T14:00:00Z", Long.MAX_VALUE - 100),
                lpoint("2018-08-07T14:00:10Z", Long.MAX_VALUE + 100),
                lpoint("2018-08-07T14:00:20Z", Long.MAX_VALUE + 200),
                lpoint("2018-08-07T14:00:30Z", Long.MAX_VALUE + 400),
                lpoint("2018-08-07T14:00:40Z", 0)
            );

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                // absent
                point("2018-08-07T14:00:10Z", 20),
                point("2018-08-07T14:00:20Z", 10),
                point("2018-08-07T14:00:30Z", 20),
                point("2018-08-07T14:00:40Z", 20));

        AggrGraphDataArrayList result = deriv(source);

        assertEquals(expected, result);
    }

    private AggrGraphDataArrayList deriv(AggrGraphDataArrayList source) {
        AggrGraphDataArrayList result = AggrGraphDataArrayList.of(DerivAggrGraphDataIterator.of(source));
        result.foldDenomIntoOne();
        return result;
    }
}
