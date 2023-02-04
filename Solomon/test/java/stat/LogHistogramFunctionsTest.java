package ru.yandex.solomon.math.stat;

import java.util.Arrays;

import org.junit.Test;

import ru.yandex.solomon.model.type.LogHistogram;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Ivan Tsybulin
 */
public class LogHistogramFunctionsTest {
    private final static double INF = Double.POSITIVE_INFINITY;

    @Test
    public void bucketQuantile() {
        // https://bb.yandex-team.ru/projects/SEARCH_INFRA/repos/yasm/browse/test/handler_test/test_hgram_quantile.py#54
        var percLevels = new double[] {
                0, 10, 19.99,
                20, 40,
                50, 80, 85,
                90, 91, 95, 99, 100
        };

        var percs = percentiles(2, new double[] {0, 3, 4, 0, 1}, 2, 2, percLevels);
        double[] logBase = new double[] {
                Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
                3.0, 11.0 / 3,
                4.0, 4.75, 4.875,
                5.0, 6.1, 6.5, 6.9, 7
        };
        double[] expected = Arrays.stream(logBase).map(x -> Math.pow(2, x)).toArray();
        assertArrayEquals(expected, percs, 1e-14);
    }

    @Test
    public void logHistMatchesHistOfLogs() {
        var percLevels = new double[] {
                0, 10, 19.99,
                20, 40,
                50, 80, 85,
                90, 91, 95, 99, 100
        };

        var percs = percentiles(2, new double[] {0, 3, 4, 0, 1}, 2, 2, percLevels);
        double[] logPercs = new double[percs.length];
        HistogramPercentile.percentilesAtTimePoint(percLevels,
                new double[] {2, 0, 3, 4, 0, 1},
                new double[] {3, 3, 4, 5, 6, 7},
                logPercs);
        for (int i = 0; i < 3; i++) {
            logPercs[i] = Double.NEGATIVE_INFINITY;
        }
        double[] expected = Arrays.stream(logPercs).map(x -> Math.pow(2, x)).toArray();
        assertArrayEquals(expected, percs, 1e-14);
    }

    @Test
    public void singleBucketQuantile() {
        // https://bb.yandex-team.ru/projects/SEARCH_INFRA/repos/yasm/browse/test/handler_test/test_hgram_quantile.py#87
        double p0 = percentile(new double[] {4}, 5, 0);
        double p1 = percentile(new double[] {4}, 5, 1);
        double p99 = percentile(new double[] {4}, 5, 99);
        double p100 = percentile(new double[] {4}, 5, 100);
        assertTrue(p1 > p0);
        assertTrue(p100 > p99);
    }


    @Test
    public void singlePointQuantile() {
        // https://bb.yandex-team.ru/projects/SEARCH_INFRA/repos/yasm/browse/test/handler_test/test_hgram_quantile.py#102
        double p0 = percentile(new double[] {1}, 5, 0);
        double p1 = percentile(new double[] {1}, 5, 1);
        double p99 = percentile(new double[] {1}, 5, 99);
        double p100 = percentile(new double[] {1}, 5, 100);
        assertTrue(p1 > p0);
        assertTrue(p100 > p99);
    }

    @Test
    public void singleBucketMedian() {
        // https://bb.yandex-team.ru/projects/SEARCH_INFRA/repos/yasm/browse/test/handler_test/test_hgram_quantile.py#131
        double p50 = percentile(new double[] {3}, 4, 50);
        assertTrue(p50 < 6.33);
    }

    @Test
    public void singleBucketAvg() {
        // https://bb.yandex-team.ru/projects/SEARCH_INFRA/repos/yasm/browse/test/handler_test/test_hgram_avg.py#31
        assertEquals(Math.pow(2, 2.5), HistogramAggr.avg(LogHistogram.newBuilder()
                .setCountZero(0)
                .setBase(2)
                .setStartPower(2)
                .addBucket(1)
                .build()), 1e-15);
        assertEquals(Math.pow(2, 2.5) / 2, HistogramAggr.avg(LogHistogram.newBuilder()
                .setCountZero(1)
                .setBase(2)
                .setStartPower(2)
                .addBucket(1)
                .build()), 1e-15);

        double[] buckets = new double[] {0, 2, 5, 0, 1};
        double valuesCnt = 2 + 5 + 1;
        double valuesSum = 2*Math.pow(2, 3.5) + 5*Math.pow(2, 4.5) + 1*Math.pow(2, 6.5);
        assertEquals(valuesSum / valuesCnt, HistogramAggr.avg(LogHistogram.newBuilder()
                .setCountZero(0)
                .setBase(2)
                .setStartPower(2)
                .setBuckets(buckets)
                .build()), 1e-15);
        assertEquals(valuesSum / (valuesCnt + 2), HistogramAggr.avg(LogHistogram.newBuilder()
                .setCountZero(2)
                .setBase(2)
                .setStartPower(2)
                .setBuckets(buckets)
                .build()), 1e-15);
    }

    @Test
    public void zeroesBuckets() {
        // https://bb.yandex-team.ru/projects/SEARCH_INFRA/repos/yasm/browse/test/handler_test/test_hgram_perc.py#81
        assertThat(percBetweenBounds(LogHistogram.newBuilder().build(), 0, 0), equalTo(Double.NaN));

        LogHistogram logHistogram = LogHistogram.newBuilder()
                .setCountZero(1)
                .setStartPower(4)
                .build();
        assertThat(percBetweenBounds(logHistogram, 0, 0), equalTo(100d));
        assertThat(percBetweenBounds(logHistogram, 0, 5), equalTo(100d));
        assertThat(percBetweenBounds(logHistogram, -INF, 5), equalTo(100d));
        assertThat(percBetweenBounds(logHistogram, -INF, INF), equalTo(100d));
        assertThat(percBetweenBounds(logHistogram, 0, INF), equalTo(100d));
        assertThat(percBetweenBounds(logHistogram, 0.1, INF), equalTo(0d));
        assertThat(percBetweenBounds(logHistogram, 0.1, 0.2), equalTo(0d));
        assertThat(percBetweenBounds(logHistogram, -0.1, 0.1), equalTo(100d));
        assertThat(percBetweenBounds(logHistogram, -0.1, 0), equalTo(100d));
    }

    @Test
    public void nonZeroesBuckets() {
        // https://bb.yandex-team.ru/projects/SEARCH_INFRA/repos/yasm/browse/test/handler_test/test_hgram_perc.py#96
        LogHistogram logHistogram = LogHistogram.newBuilder()
                .setCountZero(0)
                .setBase(2)
                .setStartPower(2)
                .addBucket(1)
                .build();
        assertThat(percBetweenBounds(logHistogram, -0.1, 10), equalTo(100d));
        assertThat(percBetweenBounds(logHistogram, -0.1, 0), equalTo(0d));
        assertThat(percBetweenBounds(logHistogram, 0, 0.1), equalTo(0d));
        assertThat(percBetweenBounds(logHistogram, 0, 10), equalTo(100d));
        assertThat(percBetweenBounds(logHistogram, 0, INF), equalTo(100d));
        assertThat(percBetweenBounds(logHistogram, -INF, INF), equalTo(100d));
        assertThat(percBetweenBounds(logHistogram, -INF, 10), equalTo(100d));
        assertThat(percBetweenBounds(logHistogram, 0, 10), equalTo(100d));
        assertThat(percBetweenBounds(logHistogram, 4, 10), equalTo(100d));
        assertThat(percBetweenBounds(logHistogram, 8, 10), equalTo(0d));
    }

    @Test
    public void mixedBuckets() {
        // https://bb.yandex-team.ru/projects/SEARCH_INFRA/repos/yasm/browse/test/handler_test/test_hgram_perc.py#114
        {
            LogHistogram logHistogram = LogHistogram.newBuilder()
                    .setCountZero(3)
                    .setBase(2)
                    .setStartPower(2)
                    .addBucket(1)
                    .build();

            assertThat(percBetweenBounds(logHistogram, -0.1, 10), equalTo(100d));
            assertThat(percBetweenBounds(logHistogram, -0.1, 0), equalTo(75d));
            assertThat(percBetweenBounds(logHistogram, -INF, 0), equalTo(75d));
            assertThat(percBetweenBounds(logHistogram, 0, 0.1), equalTo(75d));
            assertThat(percBetweenBounds(logHistogram, 0, 10), equalTo(100d));
            assertThat(percBetweenBounds(logHistogram, 0, 8), equalTo(100d));
            assertThat(percBetweenBounds(logHistogram, -INF, INF), equalTo(100d));
            assertThat(percBetweenBounds(logHistogram, -INF, 8), equalTo(100d));
            assertThat(percBetweenBounds(logHistogram, 4, 10), equalTo(25d));
            assertThat(percBetweenBounds(logHistogram, 4, INF), equalTo(25d));
            assertThat(percBetweenBounds(logHistogram, 8, 10), equalTo(0d));
            assertThat(percBetweenBounds(logHistogram, 8, INF), equalTo(0d));
            assertThat(percBetweenBounds(logHistogram, Math.pow(2, 2.5), INF), equalTo(12.5d));
        }
        {
            LogHistogram logHistogram = LogHistogram.newBuilder()
                    .setCountZero(2)
                    .setBase(2)
                    .setStartPower(2)
                    .addBucket(1)
                    .addBucket(1)
                    .build();

            assertThat(percBetweenBounds(logHistogram, 8, 20), equalTo(25d));
        }
        {
            LogHistogram logHistogram = LogHistogram.newBuilder()
                    .setCountZero(2)
                    .setBase(2)
                    .setStartPower(2)
                    .addBucket(1)
                    .addBucket(2)
                    .build();

            assertThat(percBetweenBounds(logHistogram, 8, 16), equalTo(40d));
            assertThat(percBetweenBounds(logHistogram, 8, 20), equalTo(40d));
            assertThat(percBetweenBounds(logHistogram, 8, INF), equalTo(40d));
            assertThat(percBetweenBounds(logHistogram, 4, 8), equalTo(20d));
            assertThat(percBetweenBounds(logHistogram, 5, 5), equalTo(0d));
            assertThat(percBetweenBounds(logHistogram, 2, 3), equalTo(0d));
            assertThat(percBetweenBounds(logHistogram, 0, 8), equalTo(60d));
            assertThat(percBetweenBounds(logHistogram, -INF, 8), equalTo(60d));

            // This test differs. No +1 correction is done for the first and the last bucket
            assertThat(percBetweenBounds(logHistogram, Math.pow(2, 3.25), Math.pow(2, 3.75)), equalTo(20d));
        }
    }

    private static double percentile(double[] buckets, int startPower, double percLevel) {
        return percentiles(0, buckets, startPower, 1.5, new double[] {percLevel})[0];
    }

    private static double[] percentiles(long zeros, double[] buckets, int startPower, double base, double[] percLevels) {
        LogHistogram logHistogram = LogHistogram.newBuilder()
                .setCountZero(zeros)
                .setBase(base)
                .setStartPower(startPower)
                .setBuckets(buckets)
                .build();
        return HistogramPercentile.percentiles(logHistogram, percLevels);
    }

    private static double percBetweenBounds(LogHistogram logHistogram, double from, double to) {
        if (to == 0) {
            to = Double.MIN_VALUE;
        }
        double[] result = HistogramCumulativeDistribution.cumulativeCount(logHistogram,
                new double[] {from, to, Double.POSITIVE_INFINITY});
        return 100d * (result[1] - result[0]) / result[2];
    }
}
