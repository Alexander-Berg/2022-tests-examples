package ru.yandex.solomon.model.timeseries;

import java.time.Instant;
import java.util.Random;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.model.point.DataPoint;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;

/**
 * @author Stepan Koltsov
 */
public class GraphDataTest {

    private void testOp(long[] origTimeline, double[] origValues, long[] newTimeline, double[] newValues, Function<GraphData, GraphData> tr) {
        GraphData orig = new GraphData(new Timeline(origTimeline, SortedOrCheck.CHECK), origValues);
        GraphData expected = new GraphData(new Timeline(newTimeline, SortedOrCheck.CHECK), newValues);
        Assert.assertEquals(expected, tr.apply(orig));
    }

    @Test
    public void filterNonNan() {
        testOp(new long[]{ 100, 200 }, new double[]{ 1, Double.NaN },
            new long[]{ 100 }, new double[]{ 1 },
            GraphData::filterNonNan);
    }

    private void testDropConsecutiveNaNs(
            long[] origTimeline, double[] origValue, long[] newTimeline, double[] newValues)
    {
        testOp(origTimeline, origValue, newTimeline, newValues, GraphData::dropConsecutiveNaNs);
    }

    @Test
    public void dropConsecutiveNaNs() {
        // no NaNs
        testDropConsecutiveNaNs(
                new long[] { 100, 200 }, new double[]{ 1, 2 },
                new long[]{ 100, 200 }, new double[]{ 1, 2 });
        // keep one in the middle
        testDropConsecutiveNaNs(
                new long[] { 100, 200, 300 }, new double[]{ 1, 2, 3 },
                new long[]{ 100, 200, 300 }, new double[]{ 1, 2, 3 });
        // keep only one in the middle
        testDropConsecutiveNaNs(
                new long[] { 100, 200, 300, 400 }, new double[]{ 1, Double.NaN, Double.NaN, 3 },
                new long[]{ 100, 300 /* or 200 */, 400 }, new double[]{ 1, Double.NaN, 3 });
        // NaNs at edges
        testDropConsecutiveNaNs(
            new long[]{ 100, 200, 300, 400 }, new double[]{ Double.NaN, 1, 2, Double.NaN },
            new long[]{ 200, 300 }, new double[]{ 1, 2 });
    }

    @Test
    public void differentiate() {
        Assert.assertEquals(GraphData.empty, GraphData.empty.deriv());
        Assert.assertEquals(GraphData.empty, new GraphData(new long[]{ 1000 }, new double[]{ 1 }, SortedOrCheck.CHECK).deriv());

        {
            GraphData orig = new GraphData(new long[]{ 1000, 3000 }, new double[]{ 3, 9 }, SortedOrCheck.CHECK);
            GraphData expe = new GraphData(new long[] { 3000 }, new double[] { 3 }, SortedOrCheck.CHECK);
            Assert.assertEquals(expe, orig.deriv());
        }

        {
            GraphData orig = new GraphData(new long[]{1000, 3000, 4000}, new double[]{3, 9, 9}, SortedOrCheck.CHECK);
            GraphData expe = new GraphData(new long[]{3000, 4000}, new double[]{3, 0}, SortedOrCheck.CHECK);
            Assert.assertEquals(expe, orig.deriv());
        }

        {
            GraphData orig = new GraphData(new long[]{1000, 3000, 4000, 5000}, new double[]{3, 9, 9, 8}, SortedOrCheck.CHECK);
            GraphData expe = new GraphData(new long[]{3000, 4000, 5000}, new double[]{3, 0, Double.NaN}, SortedOrCheck.CHECK);
            Assert.assertEquals(expe, orig.deriv());
        }
    }

    @Test
    public void merge() {
        {
            GraphData a = new GraphData(new long[]{1000, 2000, 3000}, new double[]{10, 20, 30}, SortedOrCheck.CHECK);
            GraphData b = new GraphData(new long[]{4000, 5000, 6000}, new double[]{40, 50, 60}, SortedOrCheck.CHECK);
            GraphData expected = new GraphData(
                    new long[]{ 1000, 2000, 3000, 4000, 5000, 6000 },
                new double[]{10, 20, 30, 40, 50, 60}, SortedOrCheck.CHECK);
            Assert.assertEquals(expected, GraphData.merge(a, b));
        }
        {
            GraphData a = new GraphData(new long[]{1000, 2000, 3000}, new double[]{10, 20, 30}, SortedOrCheck.CHECK);
            GraphData b = new GraphData(new long[]{1000, 3000, 4000}, new double[]{40, 50, 60}, SortedOrCheck.CHECK);
            GraphData expected = new GraphData(
                    new long[]{ 1000, 2000, 3000, 4000 },
                new double[]{40, 20, 50, 60}, SortedOrCheck.CHECK);
            Assert.assertEquals(expected, GraphData.merge(a, b));
        }
    }

    @Test
    public void dropBeforeManual() {
        GraphData orig = new GraphData(
                new long[]{ 2000, 3000, 4000, 5000, 6000 },
            new double[]{20, 30, 40, 50, 60}, SortedOrCheck.CHECK);
        Assert.assertEquals(orig, orig.dropLt(1000));
        Assert.assertEquals(orig, orig.dropLt(2000));
        {
            GraphData expected = new GraphData(
                    new long[]{ 3000, 4000, 5000, 6000 },
                new double[]{30, 40, 50, 60}, SortedOrCheck.CHECK);
            Assert.assertEquals(expected, orig.dropLt(2100));
        }
        {
            GraphData expected = new GraphData(
                    new long[]{ 3000, 4000, 5000, 6000 },
                new double[]{30, 40, 50, 60}, SortedOrCheck.CHECK);
            Assert.assertEquals(expected, orig.dropLt(3000));
        }{
            GraphData expected = new GraphData(
                    new long[]{ 6000 },
                new double[]{60}, SortedOrCheck.CHECK);
            Assert.assertEquals(expected, orig.dropLt(6000));
        }
        {
            GraphData expected = GraphData.empty;
            Assert.assertEquals(expected, orig.dropLt(6100));
        }
    }

    private static GraphData randomGraphData(Random random) {
        long ts0 = Instant.parse("2016-12-20T12:13:14Z").toEpochMilli();

        GraphDataArrayList r = new GraphDataArrayList();

        int points = random.nextInt(50);

        for (int j = 0; j < points; ++j) {
            long ts = ts0 + random.nextInt(100);
            double value = random.nextDouble();
            r.add(ts, value);
        }

        return r.buildGraphData();
    }

    private long randomTsFor(Random random, Timeline timeline) {
        if (timeline.isEmpty()) {
            return Instant.parse("2016-12-20T12:13:14Z").toEpochMilli();
        }

        switch (random.nextInt(4)) {
            case 0:
                return timeline.first() - random.nextInt(10);
            case 1:
                return timeline.last() + random.nextInt(10);
            case 2:
                return timeline.getPointMillisAt(random.nextInt(timeline.getPointCount()));
            case 3: {
                if (timeline.getPointCount() == 1) {
                    return timeline.first();
                }

                int pos = random.nextInt(timeline.getPointCount() - 1);
                return (timeline.getPointMillisAt(pos) + timeline.getPointMillisAt(pos + 1)) / 2;
            }
            default:
                throw new UnsupportedOperationException("unreachable");
        }
    }

    @Test
    public void dropLt() {
        Random random = new Random(17);

        for (int i = 0; i < 1000; ++i) {
            GraphData graphData = randomGraphData(random);
            long x = randomTsFor(random, graphData.getTimeline());

            Assert.assertEquals(graphData.dropLt(x), graphData.filterByTs(ts -> ts >= x));
        }
    }

    @Test
    public void dropGe() {
        Random random = new Random(17);

        for (int i = 0; i < 1000; ++i) {
            GraphData graphData = randomGraphData(random);
            long x = randomTsFor(random, graphData.getTimeline());

            Assert.assertEquals(graphData.dropGe(x), graphData.filterByTs(ts -> ts < x));
        }
    }

    @Test
    public void dropGt() {
        Random random = new Random(17);

        for (int i = 0; i < 1000; ++i) {
            GraphData graphData = randomGraphData(random);
            long x = randomTsFor(random, graphData.getTimeline());

            Assert.assertEquals(graphData.dropGt(x), graphData.filterByTs(ts -> ts <= x));
        }
    }

    @Test
    public void dropLe() {
        Random random = new Random(17);

        for (int i = 0; i < 1000; ++i) {
            GraphData graphData = randomGraphData(random);
            long x = randomTsFor(random, graphData.getTimeline());

            Assert.assertEquals(graphData.dropLe(x), graphData.filterByTs(ts -> ts > x));
        }
    }

    @Test
    public void derivWithNan() {
        GraphData source = GraphData.of(
                point("2018-05-31T14:10:01Z", 5),
                point("2018-05-31T14:10:02Z", 20),
                point("2018-05-31T14:10:03Z", Double.NaN), // solomon was unavailable
                point("2018-05-31T14:10:04Z", Double.NaN),
                point("2018-05-31T14:10:05Z", Double.NaN),
                point("2018-05-31T14:10:06Z", 40),
                point("2018-05-31T14:10:07Z", 50),
                point("2018-05-31T14:10:08Z", 80));

        GraphData expected = GraphData.of(
                point("2018-05-31T14:10:02Z", 20 - 5),
                point("2018-05-31T14:10:03Z", Double.NaN), // solomon was unavailable
                point("2018-05-31T14:10:04Z", Double.NaN),
                point("2018-05-31T14:10:05Z", Double.NaN),
                point("2018-05-31T14:10:06Z", Double.NaN),
                point("2018-05-31T14:10:07Z", 50 - 40),
                point("2018-05-31T14:10:08Z", 80 - 50));

        GraphData rate = source.deriv();
        Assert.assertThat(rate, equalTo(expected));
    }

    @Test
    public void deltaToRate() {
        GraphData source = GraphData.of(
                point("2018-04-09T14:50:00Z", 120),
                point("2018-04-09T14:51:00Z", 180),
                point("2018-04-09T14:52:00Z", 300));

        GraphData expected = GraphData.of(
                point("2018-04-09T14:50:00Z", 2), // 120 / (14:51:00 - 14:50:00) = 2 hack here
                point("2018-04-09T14:51:00Z", 3), // 180 / (14:51:00 - 14:50:00) = 3
                point("2018-04-09T14:52:00Z", 5));// 300 / (14:52:00 - 14:51:00) = 5

        GraphData result = source.deltaToRate();
        assertEquals(expected, result);
    }

    @Test
    public void deltaToRate_empty() {
        GraphData source = GraphData.of(
                point("2018-04-09T14:52:00Z", 300));

        GraphData expected = GraphData.empty;

        GraphData result = source.deltaToRate();
        assertEquals(expected, result);
    }

    @Test
    public void rateToDelta() {
        GraphData source = GraphData.of(
                point("2018-04-09T14:50:00Z", 2),
                point("2018-04-09T14:51:00Z", 3),
                point("2018-04-09T14:52:00Z", 5));

        GraphData expected = GraphData.of(
                point("2018-04-09T14:50:00Z", 120), // 2 * (14:51:00 - 14:50:00) = 120 hack here
                point("2018-04-09T14:51:00Z", 180), // 3 * (14:51:00 - 14:50:00) = 180
                point("2018-04-09T14:52:00Z", 300));// 5 * (14:52:00 - 14:51:00) = 300

        GraphData result = source.rateToDelta();
        assertEquals(expected, result);
    }

    @Test
    public void rateToDelta_empty() {
        GraphData source = GraphData.of(
                point("2018-04-09T14:52:00Z", 300));

        GraphData expected = GraphData.empty;

        GraphData result = source.rateToDelta();
        assertEquals(expected, result);
    }

    private static DataPoint point(String time, double value) {
        return new DataPoint(Instant.parse(time), value);
    }
}
