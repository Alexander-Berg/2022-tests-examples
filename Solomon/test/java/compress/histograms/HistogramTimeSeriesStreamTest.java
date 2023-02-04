package ru.yandex.solomon.codec.compress.histograms;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.solomon.codec.compress.CompressStreamFactory;
import ru.yandex.solomon.codec.compress.TimeSeriesInputStream;
import ru.yandex.solomon.codec.compress.TimeSeriesOutputStream;
import ru.yandex.solomon.codec.serializer.StockpileFormat;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.column.StockpileColumn;
import ru.yandex.solomon.model.protobuf.MetricType;
import ru.yandex.solomon.model.type.Histogram;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Vladimir Gordiychuk
 */
@RunWith(Parameterized.class)
public class HistogramTimeSeriesStreamTest {
    private TimeSeriesOutputStream out;
    private TimeSeriesInputStream in;
    private List<AggrPoint> appendedPoints;

    @Parameterized.Parameter
    public StockpileFormat format;

    @Parameterized.Parameters(name = "{0}")
    public static Object[] data() {
        return Stream.of(StockpileFormat.values())
                .distinct()
                .toArray();
    }

    private static Histogram histogram(double[] bounds, long[] buckets) {
        return Histogram.newInstance(bounds, buckets);
    }

    @Before
    public void setUp() throws Exception {
        int mask = StockpileColumn.TS.mask() | StockpileColumn.HISTOGRAM.mask();
        out = CompressStreamFactory.createOutputStream(MetricType.HIST, mask);
        appendedPoints = new ArrayList<>();
    }

    @After
    public void tearDown() throws Exception {
        out.close();
    }

    @Test
    public void one() {
        encode(histogram(new double[]{10, 20, 30}, new long[]{0, 3, 2}));
        var result = decode();

        assertThat(result, equalTo(result));
    }

    @Test
    public void empty() {
        var expect = histogram(new double[0], new long[0]);
        encode(expect);
        var result = decodeAllPoints();
        assertThat(result, equalTo(appendedPoints));
    }

    @Test
    public void multipleEmpty() {
        encode(histogram(new double[0], new long[0]));
        encode(histogram(new double[0], new long[0]));
        encode(histogram(new double[0], new long[0]));

        var result = decodeAllPoints();
        assertThat(result, equalTo(appendedPoints));
    }

    @Test
    public void manySameBounds() {
        encode(histogram(new double[]{10, 20, 30}, new long[]{0, 3, 2}));
        encode(histogram(new double[]{10, 20, 30}, new long[]{0, 3, 2}));
        encode(histogram(new double[]{10, 20, 30}, new long[]{4, 3, 8}));
        encode(histogram(new double[]{10, 20, 30}, new long[]{5, 9, 8}));
        encode(histogram(new double[]{10, 20, 30}, new long[]{9, 9, 9}));

        List<AggrPoint> result = decodeAllPoints();
        assertThat(result, equalTo(appendedPoints));
    }

    @Test
    public void manyMigrateBoundsToNew() {
        encode(histogram(new double[]{10, 20, 30}, new long[]{1, 2, 3}));
        encode(histogram(new double[]{10, 20, 30}, new long[]{3, 4, 5}));
        encode(histogram(new double[]{10, 20, 30}, new long[]{6, 7, 8}));
        encode(histogram(new double[]{10, 15, 20, 25, 30}, new long[]{5, 4, 3, 2, 1}));
        encode(histogram(new double[]{10, 15, 20, 25, 30}, new long[]{1, 2, 3, 4, 5}));
        encode(histogram(new double[]{10, 15, 20, 25, 30}, new long[]{3, 3, 3, 3, 3}));

        List<AggrPoint> result = decodeAllPoints();
        assertThat(result, equalTo(appendedPoints));
    }

    @Test
    public void changeBucketsSize() {
        encode(histogram(new double[]{10, 20}, new long[]{1, 2}));
        encode(histogram(new double[]{10, 20, 30}, new long[]{3, 4, 5}));
        encode(histogram(new double[]{10, 20}, new long[]{6, 7}));
        encode(histogram(new double[]{10, 15, 20, 25, 30}, new long[]{5, 4, 3, 2, 1}));
        encode(histogram(new double[]{10, 15, 20, 25, 30}, new long[]{1, 2, 3, 4, 5}));
        encode(histogram(new double[]{10, 15}, new long[]{3, 3}));

        List<AggrPoint> result = decodeAllPoints();
        assertThat(result, equalTo(appendedPoints));
    }

    @Test
    public void manyCustomBoundsEachIteration() {
        encode(histogram(new double[]{10, 20, 30}, new long[]{1, 2, 3}));
        encode(histogram(new double[]{5, 10, 15}, new long[]{3, 4, 5}));
        encode(histogram(new double[]{1, 3, 5}, new long[]{6, 7, 8}));
        encode(histogram(new double[]{1, 2, 3, 4, 5}, new long[]{2, 4, 6, 8, 10}));

        List<AggrPoint> result = decodeAllPoints();
        assertThat(result, equalTo(appendedPoints));
    }

    @Test
    public void boundsAsDouble() {
        encode(histogram(new double[]{1, 1.5, 2, 2.5}, new long[]{1, 2, 3, 1}));
        encode(histogram(new double[]{1, 1.5, 2, 2.5}, new long[]{3, 4, 5, 1}));
        encode(histogram(new double[]{1, 1.5, 2, 2.5}, new long[]{6, 7, 8, 5}));
        encode(histogram(new double[]{1, 1.5, 2, 2.5}, new long[]{8, 9, 10, 8}));

        List<AggrPoint> result = decodeAllPoints();
        assertThat(result, equalTo(appendedPoints));
    }

    @Test
    public void reuseHistogramObject() {
        var expectedFirst = histogram(new double[]{1, 10, 100}, new long[]{1, 2, 1});
        var expectedSecond = histogram(new double[]{1, 10, 100}, new long[]{2, 3, 0});

        encode(expectedFirst);
        encode(expectedSecond);

        int mask = StockpileColumn.TS.mask() | StockpileColumn.HISTOGRAM.mask();
        AggrPoint point = new AggrPoint();
        TimeSeriesInputStream stream = CompressStreamFactory.createInputStream(MetricType.HIST, mask, out.getCompressedData());

        assertTrue(stream.hasNext());
        stream.readPoint(mask, point);

        var first = point.histogram;
        var copy = Histogram.copyOf(first);

        assertTrue(stream.hasNext());
        stream.readPoint(mask, point);

        assertSame(first, point.histogram);
        assertEquals(expectedFirst, copy);
        assertEquals(expectedSecond, point.histogram);
    }

    @Test
    public void enodeDenom() {
        Histogram[] source = {
            histogram(new double[]{1, 10, 100}, new long[]{1, 2, 1}),
            histogram(new double[]{1, 10, 100}, new long[]{2, 3, 0}),
            histogram(new double[]{1, 10, 100}, new long[]{2, 3, 0}),
            histogram(new double[]{1, 10, 100}, new long[]{2, 3, 0}).setDenom(15_000),
            histogram(new double[]{1, 10, 100}, new long[]{1, 5, 0}).setDenom(15_000),
            histogram(new double[]{1, 10, 100}, new long[]{1, 2, 0}).setDenom(15_000),
            histogram(new double[]{1, 10, 100, 200}, new long[]{1, 2, 0, 0}).setDenom(15_000),
            histogram(new double[]{1, 10, 100, 200}, new long[]{1, 2, 3, 0}).setDenom(15_000),
            histogram(new double[]{1, 10, 100, 200}, new long[]{2, 2, 3, 0}).setDenom(15_000),
            histogram(new double[]{1, 10, 100, 200}, new long[]{3, 2, 1, 0}).setDenom(30_000)
        };

        encode(source);
        Histogram[] result = decodeAllPoints()
            .stream()
            .map(point -> point.histogram)
            .toArray(Histogram[]::new);

        assertArrayEquals(source, result);
    }

    private void encode(Histogram... histograms) {
        long timeMillis = Instant.parse("2017-06-15T12:40:42Z").toEpochMilli();
        long stepMillis = TimeUnit.SECONDS.toMillis(30L);

        AggrPoint[] points = new AggrPoint[histograms.length];
        for (int index = 0; index < histograms.length; index++) {
            AggrPoint point = new AggrPoint();
            point.setTsMillis(timeMillis);
            point.setHistogram(histograms[index]);

            timeMillis += stepMillis;
            points[index] = point;
        }

        appendPoint(points);
    }

    private void appendPoint(AggrPoint... points) {
        for (AggrPoint point : points) {
            out.writePoint(point.columnSet, point);
            appendedPoints.add(point.withMask(point.columnSet));
        }
    }

    private Histogram decode() {
        AggrPoint point = readNextPoint();
        return point.histogram;
    }

    private AggrPoint readNextPoint() {
        int mask = StockpileColumn.TS.mask() | StockpileColumn.HISTOGRAM.mask();
        if (in == null || !in.hasNext()) {
            in = CompressStreamFactory.createInputStream(MetricType.HIST, mask, out.getCompressedData());
        }

        AggrPoint point = new AggrPoint();
        point.columnSet = mask;
        in.readPoint(point.columnSet, point);
        return point;
    }

    private List<AggrPoint> decodeAllPoints() {
        int mask = StockpileColumn.TS.mask() | StockpileColumn.HISTOGRAM.mask();
        TimeSeriesInputStream stream = CompressStreamFactory.createInputStream(MetricType.HIST, mask, out.getCompressedData());
        List<AggrPoint> result = new ArrayList<>();
        while (stream.hasNext()) {
            AggrPoint point = new AggrPoint();
            point.columnSet = mask;
            stream.readPoint(mask, point);
            result.add(point);
        }

        return result;
    }
}
