package ru.yandex.solomon.math.stat;


import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;

import org.apache.commons.math3.special.Erf;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.monlib.metrics.histogram.HistogramSnapshot;
import ru.yandex.monlib.metrics.histogram.Histograms;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.point.MutableDataPoint;
import ru.yandex.solomon.model.protobuf.MetricType;
import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.model.timeseries.GraphDataArrayList;
import ru.yandex.solomon.model.timeseries.GraphDataAsAggrIterable;
import ru.yandex.solomon.model.type.Histogram;
import ru.yandex.solomon.model.type.LogHistogram;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public class HistogramFunctionsTest {

    private static final double LOG_HIST_BASE = 1.125;
    private static final int LOG_SCALE = (int) (Math.log(50) / Math.log(LOG_HIST_BASE));
    private static final double INF = Double.POSITIVE_INFINITY;

    private LogHistogram fromExponentialSnapshot(HistogramSnapshot snapshot) {
        var builder = LogHistogram.newInstance()
                .setStartPower(LOG_SCALE - 1)
                .setBase(LOG_HIST_BASE);

        for (int i = 0; i < snapshot.count(); i++) {
            builder.addBucket(snapshot.value(i));
        }

        return builder.build();
    }

    @Test
    public void approximation() {
        final int SAMPLE_SIZE = 20_000;
        Random rng = new Random(42);
        long[] values = new long[SAMPLE_SIZE];
        var collector = Histograms.linear(48, 0, 10);
        var expCollector = Histograms.exponential(48, LOG_HIST_BASE, Math.pow(LOG_HIST_BASE, LOG_SCALE));

        final double mean = 350d;
        final double stddev = 20d;
        double zh = Math.sqrt(2) * Erf.erfInv(0.5);
        double p25exp = mean - zh * stddev;
        double p75exp = mean + zh * stddev;
        double sumExp = mean * SAMPLE_SIZE;

        double sumValues = 0;
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            long value = Math.round(mean + stddev * rng.nextGaussian());
            values[i] = value;
            collector.collect(value);
            expCollector.collect(value);
            sumValues += value;
        }

        Percentile percentile = new Percentile();
        percentile.setData(Arrays.stream(values).mapToDouble(x -> x).toArray());
        double p25 = percentile.evaluate(25);
        double p75 = percentile.evaluate(75);
        double countBetweenQuartiles = Arrays.stream(values)
                .filter(x -> x > p25exp && x < p75exp)
                .count();

        Histogram histogram = Histogram.copyOf(collector.snapshot());
        double p25h = percentile(histogram, 25);
        double p75h = percentile(histogram, 75);
        double sumH = HistogramAggr.sum(histogram);
        double avgH = HistogramAggr.avg(histogram);
        double[] countH = cumulativeCount(histogram, p25exp, p75exp);
        assertArrayEquals("count should be an exact inverse of percentile",
                new double[] {0.25 * SAMPLE_SIZE, 0.75 * SAMPLE_SIZE},
                cumulativeCount(histogram, p25h, p75h),
                1e-14 * SAMPLE_SIZE);

        LogHistogram logHistogram = fromExponentialSnapshot(expCollector.snapshot());
        double p25lh = percentile(logHistogram, 25);
        double p75lh = percentile(logHistogram, 75);
        double sumLh = HistogramAggr.sum(logHistogram);
        double avgLh = HistogramAggr.avg(logHistogram);
        double[] countLh = cumulativeCount(logHistogram, p25exp, p75exp);
        assertArrayEquals("count should be an exact inverse of percentile",
                new double[] {0.25 * SAMPLE_SIZE, 0.75 * SAMPLE_SIZE},
                cumulativeCount(logHistogram, p25lh, p75lh),
                1e-14 * SAMPLE_SIZE);

        assertThat(p25, closeTo(p25exp, 1d));
        assertThat(p25h, closeTo(p25exp, 1d));
        assertThat(p25lh, closeTo(p25exp, 5d));

        assertThat(p75, closeTo(p75exp, 1d));
        assertThat(p75h, closeTo(p75exp, 1d));
        assertThat(p75lh, closeTo(p75exp, 5d));

        assertThat(sumValues, closeTo(sumExp, 0.5 * SAMPLE_SIZE));
        assertThat(sumH, closeTo(sumExp, 0.5 * SAMPLE_SIZE));
        assertThat(sumLh, closeTo(sumExp, 0.5 * SAMPLE_SIZE));

        assertThat(avgH, closeTo(mean, 0.5));
        assertThat(avgLh, closeTo(mean, 0.5));

        assertThat(countH[1] - countH[0], closeTo(countBetweenQuartiles, 0.02 * SAMPLE_SIZE));
        assertThat(countLh[1] - countLh[0], closeTo(countBetweenQuartiles, 0.1 * SAMPLE_SIZE));
    }

    @Test
    public void onePointHistogram() {
        String ts = "2017-03-01T00:00:00Z";
        List<HistogramBucket> source = Arrays.asList(
                bucket(100, point(ts, 10)),
                bucket(200, point(ts, 40)),
                bucket(300, point(ts, 20)),
                bucket(400, point(ts, 30)),
                bucket(500, point(ts, 0))
        );

        assertThat(percentile(source, 20d), equalTo(GraphData.of(point(ts, 125d))));
        assertThat(percentile(source, 40d), equalTo(GraphData.of(point(ts, 175d))));
        assertThat(percentile(source, 50d), equalTo(GraphData.of(point(ts, 200d))));
        assertThat(percentile(source, 70d), equalTo(GraphData.of(point(ts, 300d))));
        assertThat(percentile(source, 85d), equalTo(GraphData.of(point(ts, 350d))));
        assertThat(percentile(source, 100d), equalTo(GraphData.of(point(ts, 400d))));

        double sum = 0 +
                50d * 10d +
                150d * 40d +
                250d * 20d +
                350d * 30d +
                450d * 0d;
        double count = 10d + 40d + 20d + 30d + 0d;
        assertThat(reduce(source, HistogramAggr::sum), equalTo(GraphData.of(point(ts, sum))));
        assertThat(reduce(source, HistogramAggr::avg), equalTo(GraphData.of(point(ts, sum / count))));
        List<GraphData> counts = cumulativeCount(source,
                new double[] {-INF, -50, 0 , 50 , 100, 150, 350, 350},
                new double[] {INF , 0  , 50, 100, 150, 300, 500, INF}
        );
        assertThat(counts, equalTo(List.of(
                GraphData.of(point(ts, count)),
                GraphData.of(point(ts, 0)),
                GraphData.of(point(ts, 5)),
                GraphData.of(point(ts, 5)),
                GraphData.of(point(ts, 20)),
                GraphData.of(point(ts, 40)),
                GraphData.of(point(ts, 15)),
                GraphData.of(point(ts, 15))
        )));
    }

    @Test
    public void onePointSeveralPercentiles() {
        String ts = "2017-03-01T00:00:00Z";
        List<HistogramBucket> source = Arrays.asList(
            bucket(100, point(ts, 10)),
            bucket(200, point(ts, 40)),
            bucket(300, point(ts, 20)),
            bucket(400, point(ts, 30)),
            bucket(500, point(ts, 0))
        );

        double[] percentiles = {20d, 40d, 50d, 70d, 85d, 100d};

        List<GraphData> actual = percentile(source, percentiles);

        List<GraphData> expected = Arrays.asList(
            GraphData.of(point(ts, 125d)),
            GraphData.of(point(ts, 175d)),
            GraphData.of(point(ts, 200d)),
            GraphData.of(point(ts, 300d)),
            GraphData.of(point(ts, 350d)),
            GraphData.of(point(ts, 400d))
        );

        assertThat(actual, equalTo(expected));
    }

    @Test
    public void allAtTheFirstBucket() {
        String ts = "2017-03-01T00:00:00Z";
        List<HistogramBucket> source = Arrays.asList(
                bucket(10, point(ts, 5)),
                bucket(20, point(ts, 0)),
                bucket(30, point(ts, 0))
        );

        assertThat(percentileAtPoint(ts, source, 10d), equalTo(1.0));
        assertThat(percentileAtPoint(ts, source, 20d), equalTo(2.0));
        assertThat(percentileAtPoint(ts, source, 30d), equalTo(3.0));
        assertThat(percentileAtPoint(ts, source, 50d), equalTo(5.0));
        assertThat(percentileAtPoint(ts, source, 90d), equalTo(9.0));
        assertThat(percentileAtPoint(ts, source, 99d), equalTo(9.9));
        assertThat(percentileAtPoint(ts, source, 100d), equalTo(10d));

        assertThat(sumAtPoint(ts, source), equalTo(5d * 5d));
        assertThat(avgAtPoint(ts, source), equalTo(5d));

        List<GraphData> counts = cumulativeCount(source,
                new double[] {-INF, -1 , 0, 5 , 10, -1, 2},
                new double[] {INF , 0  , 5, 10, 15, 1, INF}
        );
        assertThat(counts, equalTo(List.of(
                GraphData.of(point(ts, 5)),
                GraphData.of(point(ts, 0)),
                GraphData.of(point(ts, 2.5)),
                GraphData.of(point(ts, 2.5)),
                GraphData.of(point(ts, 0)),
                GraphData.of(point(ts, 0.5)),
                GraphData.of(point(ts, 4))
        )));
    }

    @Test
    public void allAtTheLastBucket() {
        String ts = "2017-03-01T00:00:00Z";
        List<HistogramBucket> source = Arrays.asList(
                bucket(10, point(ts, 0)),
                bucket(20, point(ts, 0)),
                bucket(30, point(ts, 300))
        );

        assertThat(percentileAtPoint(ts, source, 50d), equalTo(25.0));
        assertThat(percentileAtPoint(ts, source, 90d), equalTo(29.0));
        assertThat(percentileAtPoint(ts, source, 99d), equalTo(29.9));

        assertThat(sumAtPoint(ts, source), equalTo(25d * 300d));
        assertThat(avgAtPoint(ts, source), equalTo(25d));

        List<GraphData> counts = cumulativeCount(source,
                new double[] {-INF, -1 , 0 , 10, -50, 30},
                new double[] {INF , 0  , 25, 20, 25 , INF}
        );
        assertThat(counts, equalTo(List.of(
                GraphData.of(point(ts, 300)),
                GraphData.of(point(ts, 0)),
                GraphData.of(point(ts, 150)),
                GraphData.of(point(ts, 0)),
                GraphData.of(point(ts, 150)),
                GraphData.of(point(ts, 0))
        )));
    }

    @Test
    public void absentValuesForBuckets() {
        List<HistogramBucket> source = Arrays.asList(
                bucket(10),
                bucket(20),
                bucket(30)
        );

        assertThat(percentile(source, 80d), equalTo(GraphData.empty));

        assertThat(reduce(source, HistogramAggr::sum), equalTo(GraphData.empty));
        assertThat(reduce(source, HistogramAggr::avg), equalTo(GraphData.empty));

        List<GraphData> counts = cumulativeCount(source,
                new double[] {-INF, 0 , 10, 30},
                new double[] {INF , 25, 20, INF}
        );
        assertThat(counts, equalTo(List.of(
                GraphData.empty,
                GraphData.empty,
                GraphData.empty,
                GraphData.empty
        )));
    }

    @Test
    public void pointCanBeAbsentOnParticularTime() {
        String ts1 = "2017-03-01T00:00:00Z";
        String ts2 = "2017-03-02T00:00:00Z";
        List<HistogramBucket> source = Arrays.asList(
                bucket(10, point(ts1, 3), point(ts2, 2)),
                bucket(20,                      point(ts2, 8)),
                bucket(30, point(ts1, 2), point(ts2, 4))
        );

        assertThat(percentile(source, 60), equalTo(GraphData.of(point(ts1, 10d), point(ts2, 18.0))));
        assertThat(percentile(source, 80), equalTo(GraphData.of(point(ts1, 25d), point(ts2, 23))));

        assertThat(reduce(source, HistogramAggr::sum), equalTo(GraphData.of(
                point(ts1, 3d * 5d + 0d * 15d + 2d * 25d),
                point(ts2, 2d * 5d + 8d * 15d + 4d * 25d))));
        assertThat(reduce(source, HistogramAggr::avg), equalTo(GraphData.of(
                point(ts1, (3d * 5d + 0d * 15d + 2d * 25d) / (3 +/* 0 +*/ 2)),
                point(ts2, (2d * 5d + 8d * 15d + 4d * 25d) / (2 + 8 + 4)))));

        List<GraphData> counts = cumulativeCount(source,
                new double[] {-INF, -1 , 0 , 10, -50, 25},
                new double[] {INF , 0  , 25, 20, 5  , INF}
        );
        assertThat(counts, equalTo(List.of(
                GraphData.of(point(ts1, 5), point(ts2, 14)),
                GraphData.of(point(ts1, 0), point(ts2, 0)),
                GraphData.of(point(ts1, 4), point(ts2, 12)),
                GraphData.of(point(ts1, 0), point(ts2, 8)),
                GraphData.of(point(ts1, 1.5), point(ts2, 1)),
                GraphData.of(point(ts1, 1), point(ts2, 2))
        )));
    }

    @Test
    public void nanEqualToAbsentPointsOnParticularBucket() {
        String ts1 = "2017-03-01T00:00:00Z";
        String ts2 = "2017-03-02T00:00:00Z";
        List<HistogramBucket> source = Arrays.asList(
                bucket(10, point(ts1, 3), point(ts2, 2)),
                bucket(20, point(ts1, Double.NaN), point(ts2, 8)),
                bucket(30, point(ts1, 2), point(ts2, 4))
        );

        assertThat(percentile(source, 60), equalTo(GraphData.of(point(ts1, 10d), point(ts2, 18.0))));
        assertThat(percentile(source, 80), equalTo(GraphData.of(point(ts1, 25d), point(ts2, 23.0))));

        assertThat(reduce(source, HistogramAggr::sum), equalTo(GraphData.of(
                point(ts1, 3d * 5d + 0d * 15d + 2d * 25d),
                point(ts2, 2d * 5d + 8d * 15d + 4d * 25d))));
        assertThat(reduce(source, HistogramAggr::avg), equalTo(GraphData.of(
                point(ts1, (3d * 5d + 0d * 15d + 2d * 25d) / (3 +/* 0 +*/ 2)),
                point(ts2, (2d * 5d + 8d * 15d + 4d * 25d) / (2 + 8 + 4)))));

        List<GraphData> counts = cumulativeCount(source,
                new double[] {-INF, -1 , 0 , 10, -50, 25},
                new double[] {INF , 0  , 25, 20, 5  , INF}
        );
        assertThat(counts, equalTo(List.of(
                GraphData.of(point(ts1, 5), point(ts2, 14)),
                GraphData.of(point(ts1, 0), point(ts2, 0)),
                GraphData.of(point(ts1, 4), point(ts2, 12)),
                GraphData.of(point(ts1, 0), point(ts2, 8)),
                GraphData.of(point(ts1, 1.5), point(ts2, 1)),
                GraphData.of(point(ts1, 1), point(ts2, 2))
        )));
    }

    @Test
    public void functionsOfHistogram() {
        var histogram = Histogram.newInstance(
            new double[]{100, 200, 300, 400, 500},
            new long[]{10, 40, 20, 30, 0});

        assertThat(percentile(histogram, 20d), equalTo(125d));
        assertThat(percentile(histogram, 40d), equalTo(175d));
        assertThat(percentile(histogram, 50d), equalTo(200d));
        assertThat(percentile(histogram, 70d), equalTo(300d));
        assertThat(percentile(histogram, 85d), equalTo(350d));
        assertThat(percentile(histogram, 100d), equalTo(400d));

        assertThat(HistogramAggr.sum(histogram),
                equalTo(10 * 50d + 40 * 150d + 20 * 250d + 30 * 350d + 0 * 450d));
        assertThat(HistogramAggr.avg(histogram),
                equalTo((10 * 50d + 40 * 150d + 20 * 250d + 30 * 350d + 0 * 450d) / 100d));

        double[] counts = cumulativeCount(histogram, -INF, -50, -10, 0, 50, 250, 300, 450, 600, INF);
        assertThat(counts, equalTo(new double[] {0, 0, 0, 0, 5, 60, 70, 100, 100, 100}));
    }

    @Test
    public void percentileOfHistogramExactMinMax() {
        var histogram = Histogram.newInstance(
            new double[] { 100, 200, 300, 400, 500 },
            new long[] { 0, 0, 10, 10, 0});

        assertThat(percentile(histogram, 0d), equalTo(200d));
        assertThat(percentile(histogram,  0.001), closeTo(200d, 1e-2));
        assertThat(percentile(histogram, 99.999), closeTo(400d, 1e-2));
        assertThat(percentile(histogram, 100d), equalTo(400d));
    }

    @Test
    public void functionsOfHistogramWithInf() {
        var histogram = Histogram.newInstance(
            new double[] { 100, 200, 300, 400, 500, Histograms.INF_BOUND },
            new long[] { 10, 40, 20, 20, 0, 10 });

        assertThat(percentile(histogram, 90.00), equalTo(400d));
        assertThat(percentile(histogram, 90.01), closeTo(500d, 10d));
        assertThat(percentile(histogram, 99.00), equalTo(500d));
        assertThat(percentile(histogram, 99.90), equalTo(500d));
        assertThat(percentile(histogram, 99.99), equalTo(500d));
        assertThat(percentile(histogram, 100.0), equalTo(500d));

        double sum = 10 * 50d + 40 * 150d + 20 * 250d + 20 * 350d + 0 * 450d + 10 * 500d;
        assertThat(HistogramAggr.sum(histogram),
                equalTo(sum));
        assertThat(HistogramAggr.avg(histogram),
                equalTo(sum / 100d));

        double[] counts = cumulativeCount(histogram, -INF, -50, -10, 0, 50, 250, 300, 450, 600, INF);
        assertThat(counts, equalTo(new double[] {0, 0, 0, 0, 5, 60, 70, 90, 90, 100}));
    }

    @Test
    public void functionsOfHistogramAllInf() {
        var histogram = Histogram.newInstance(
            new double[] { 100, 200, 300, 400, 500, Histograms.INF_BOUND },
            new long[] { 0, 0, 0, 0, 0, 10 });

        assertThat(percentile(histogram, 00.00), equalTo(500d));
        assertThat(percentile(histogram, 01.00), equalTo(500d));
        assertThat(percentile(histogram, 90.00), equalTo(500d));
        assertThat(percentile(histogram, 99.00), equalTo(500d));
        assertThat(percentile(histogram, 100.0), equalTo(500d));

        assertThat(HistogramAggr.sum(histogram), equalTo(10 * 500d));
        assertThat(HistogramAggr.avg(histogram), equalTo(500d));

        double[] counts = cumulativeCount(histogram, -INF, -50, -10, 0, 50, 250, 300, 450, 600, INF);
        assertThat(counts, equalTo(new double[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 10}));
    }

    @Test
    public void functionsOfHistogramEmpty() {
        var histogram = Histogram.newInstance(
            new double[] { 100, 200, 300, 400, 500, Histograms.INF_BOUND },
            new long[] { 0, 0, 0, 0, 0, 0 });

        assertThat(percentile(histogram, 00.00), equalTo(HistogramPercentile.EMPTY_HISTOGRAM_PERCENTILE_VALUE));
        assertThat(percentile(histogram, 01.00), equalTo(HistogramPercentile.EMPTY_HISTOGRAM_PERCENTILE_VALUE));
        assertThat(percentile(histogram, 90.00), equalTo(HistogramPercentile.EMPTY_HISTOGRAM_PERCENTILE_VALUE));
        assertThat(percentile(histogram, 99.00), equalTo(HistogramPercentile.EMPTY_HISTOGRAM_PERCENTILE_VALUE));
        assertThat(percentile(histogram, 100.0), equalTo(HistogramPercentile.EMPTY_HISTOGRAM_PERCENTILE_VALUE));

        assertThat(HistogramAggr.sum(histogram), equalTo(0d));
        assertThat(HistogramAggr.avg(histogram), equalTo(Double.NaN));

        double[] counts = cumulativeCount(histogram, -INF, -50, -10, 0, 50, 250, 300, 450, 600, INF);
        assertThat(counts, equalTo(new double[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0}));
    }

    @Test
    public void basicTest() {
        Assert.assertEquals(0.00, helper( 0, new double[]{10, 40, 20, 30, 0}, new double[]{1, 2, 3, 4, 5}), 0.01);
        Assert.assertEquals(2.00, helper(50, new double[]{10, 40, 20, 30, 0}, new double[]{1, 2, 3, 4, 5}), 0.01);
        Assert.assertEquals(2.50, helper(60, new double[]{10, 40, 20, 30, 0}, new double[]{1, 2, 3, 4, 5}), 0.01);
        Assert.assertEquals(3.00, helper(70, new double[]{10, 40, 20, 30, 0}, new double[]{1, 2, 3, 4, 5}), 0.01);
        Assert.assertEquals(3.33, helper(80, new double[]{10, 40, 20, 30, 0}, new double[]{1, 2, 3, 4, 5}), 0.01);
        Assert.assertEquals(3.66, helper(90, new double[]{10, 40, 20, 30, 0}, new double[]{1, 2, 3, 4, 5}), 0.01);
        Assert.assertEquals(3.97, helper(99, new double[]{10, 40, 20, 30, 0}, new double[]{1, 2, 3, 4, 5}), 0.01);
        Assert.assertEquals(3.97, helper(99, new double[]{100, 400, 200, 300, 0}, new double[]{1, 2, 3, 4, 5}), 0.01);
        Assert.assertEquals(39.7, helper(99, new double[]{100, 400, 200, 300, 0}, new double[]{10, 20, 30, 40, 50}), 0.1);
        Assert.assertEquals(40.0, helper(100, new double[]{100, 400, 200, 300, 0}, new double[]{10, 20, 30, 40, 50}), 0);
    }

    @Test
    public void testNaN() {
        double NaN = Double.NaN;
        Assert.assertEquals(2.00,  helper(50, new double[]{10,   40,  20,  30, NaN}, new double[]{1, 2, 3, 4, 5}), 0.01);
        Assert.assertEquals(1.625, helper(50, new double[]{10,   40,  20, NaN, NaN}, new double[]{1, 2, 3, 4, 5}), 0.01);
        Assert.assertEquals(1.375, helper(50, new double[]{10,   40, NaN, NaN, NaN}, new double[]{1, 2, 3, 4, 5}), 0.01);
        Assert.assertEquals(0.5,   helper(50, new double[]{10,  NaN, NaN, NaN, NaN}, new double[]{1, 2, 3, 4, 5}), 0.01);
        Assert.assertEquals(NaN,   helper(50, new double[]{NaN, NaN, NaN, NaN, NaN}, new double[]{1, 2, 3, 4, 5}), 0.01);
    }

    @Test
    public void p100() {
        double[] weights = {
                8516.133333333335, 3474.6, 1138.4, 1781.6000000000001, 159.46666666666667,
                43.53333333333333, 10.133333333333333, 7.0, 3.266666666666667, 10.066666666666666,
                0.9333333333333333, 0.9333333333333333, 1.8, 1.9333333333333333, 0.0, 0.13333333333333333,
                0.26666666666666666, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
        };

        double[] bounds = {
                1, 5, 10, 50, 100, 150, 200, 250, 300, 500, 750, 1000, 1500, 2000, 2500, 3000, 4000,
                5000, 7500, 10000, 15000, 20000, 30000, 40000, 50000, 60000
        };

        Assert.assertEquals(4000, helper(100, weights, bounds), 0.01);
    }

    @Test
    public void hugePercentile() {
        double[] bounds = new double[]{
                17,
                26,
                39,
                59,
                88,
                132,
                198,
                296,
                444,
                667
        };

        double[] buckets = new double[]{
                0.4666666667,
                3.4,
                2.0666666667,
                0.3333333333,
                0.0666666667,
                0.1333333333,
                0,
                0,
                0.1333333333,
                0
        };

        Assert.assertEquals(444, helper(99.9999, buckets, bounds), 0.01);
    }

    private double helper(double percent, double[] bucketWeights, double[] bucketValues) {
        double[] result = new double[1];
        HistogramPercentile.percentilesAtTimePoint(new double[]{ percent }, bucketWeights, bucketValues, result);
        return result[0];
    }

    private GraphData percentile(List<HistogramBucket> buckets, double percentile) {
        return percentile(buckets, new double[] {percentile}).get(0);
    }

    private GraphData reduce(List<HistogramBucket> buckets, ToDoubleFunction<LegacyHistogramPoint> reducer) {
        var cursors = buckets.stream()
                .map(bucket -> new HistogramBucketCursor(
                        bucket.getBucketLimit(),
                        MetricType.DGAUGE,
                        new GraphDataAsAggrIterable(bucket.getCounts())
                ))
                .collect(Collectors.toList());
        var legacyHistIterator = new LegacyHistogramIterator(cursors);
        var combineIterator = HistogramReduceIterator.of(legacyHistIterator, LegacyHistogramPoint::new, reducer);
        GraphDataArrayList result = new GraphDataArrayList();

        MutableDataPoint point = new MutableDataPoint();
        while (combineIterator.next(point)) {
                result.add(point.getTsMillis(), point.getValue());
        }
        return result.buildGraphData();
    }

    private List<GraphData> cumulativeCount(List<HistogramBucket> buckets, double[] lowerBounds, double[] upperBounds) {
        var cursors = buckets.stream()
                .map(bucket -> new HistogramBucketCursor(
                        bucket.getBucketLimit(),
                        MetricType.DGAUGE,
                        new GraphDataAsAggrIterable(bucket.getCounts())
                ))
                .collect(Collectors.toList());
        var legacyHistIterator = new LegacyHistogramIterator(cursors);
        var combineIterator = new HistogramCumulativeDistributionIterator<>(legacyHistIterator,
                lowerBounds, upperBounds,
                EnumSet.noneOf(HistogramCumulativeDistributionIterator.Options.class))
        {
            @Override
            public LegacyHistogramPoint newPoint() {
                return new LegacyHistogramPoint();
            }

            @Override
            public void computeCumulativeDistribution(
                    @Nonnull LegacyHistogramPoint legacyHistogramPoint,
                    @Nonnull double[] cumulativeCount,
                    @Nonnull double[] sortedBounds)
            {
                HistogramCumulativeDistribution.cumulativeCount(legacyHistogramPoint, cumulativeCount, sortedBounds);
            }
        };
        List<GraphDataArrayList> result = IntStream.range(0, lowerBounds.length)
                .mapToObj(i -> new GraphDataArrayList())
                .collect(Collectors.toList());

        VectorPoint point = new VectorPoint(lowerBounds.length);
        while (combineIterator.next(point)) {
            for (int i = 0; i < point.values.length; i++) {
                result.get(i).add(point.getTsMillis(), point.values[i]);
            }
        }
        return result.stream()
                .map(GraphDataArrayList::buildGraphData)
                .collect(Collectors.toList());
    }

    private double sumAtPoint(String time, List<HistogramBucket> buckets) {
        long tsMillis = Instant.parse(time).toEpochMilli();
        GraphData sum = reduce(buckets, HistogramAggr::sum);
        assertEquals(tsMillis, sum.getTimeline().getPointMillisAt(0));
        return sum.getValues().at(0);
    }

    private double avgAtPoint(String time, List<HistogramBucket> buckets) {
        long tsMillis = Instant.parse(time).toEpochMilli();
        GraphData sum = reduce(buckets, HistogramAggr::avg);
        assertEquals(tsMillis, sum.getTimeline().getPointMillisAt(0));
        return sum.getValues().at(0);
    }

    private List<GraphData> percentile(List<HistogramBucket> buckets, double[] percentileLevels) {
        var cursors = buckets.stream()
                .map(bucket -> new HistogramBucketCursor(
                        bucket.getBucketLimit(),
                        MetricType.DGAUGE,
                        new GraphDataAsAggrIterable(bucket.getCounts())
                ))
                .collect(Collectors.toList());
        var legacyHistIterator = new LegacyHistogramIterator(cursors);
        var combineIterator = HistogramPercentileIterator.of(legacyHistIterator, percentileLevels,
                LegacyHistogramPoint::new, HistogramPercentile::percentiles);

        List<GraphDataArrayList> results = Arrays.stream(percentileLevels)
                .mapToObj(ignore -> new GraphDataArrayList())
                .collect(toList());

        VectorPoint point = new VectorPoint(percentileLevels.length);
        while (combineIterator.next(point)) {
            for (int i = 0; i < percentileLevels.length; i++) {
                results.get(i).add(point.tsMillis, point.values[i]);
            }
        }
        return results.stream().map(GraphDataArrayList::buildGraphData).collect(toList());
    }

    private static double percentile(Histogram histogram, double percentile) {
        return HistogramPercentile.percentiles(histogram, new double[] {percentile})[0];
    }

    private static double percentile(LogHistogram histogram, double percLevel) {
        return HistogramPercentile.percentiles(histogram, new double[] {percLevel})[0];
    }

    private double percentileAtPoint(String time, List<HistogramBucket> buckets, double level) {
        long tsMillis = Instant.parse(time).toEpochMilli();
        GraphData percentile = percentile(buckets, level);
        assertEquals(tsMillis, percentile.getTimeline().getPointMillisAt(0));
        return percentile.getValues().at(0);
    }

    private static double[] cumulativeCount(Histogram histogram, double... sortedBounds) {
        return HistogramCumulativeDistribution.cumulativeCount(histogram, sortedBounds);
    }

    private static double[] cumulativeCount(LogHistogram histogram, double... sortedBounds) {
        return HistogramCumulativeDistribution.cumulativeCount(histogram, sortedBounds);
    }

    private DataPoint point(String time, double value) {
        return new DataPoint(Instant.parse(time), value);
    }

    private HistogramBucket bucket(long bucketLimit, DataPoint... points) {
        return new HistogramBucket(bucketLimit, GraphData.of(points));
    }
}
