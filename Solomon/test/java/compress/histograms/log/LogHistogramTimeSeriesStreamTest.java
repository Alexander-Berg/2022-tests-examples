package ru.yandex.solomon.codec.compress.histograms.log;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.solomon.codec.compress.CompressStreamFactory;
import ru.yandex.solomon.codec.compress.TimeSeriesInputStream;
import ru.yandex.solomon.codec.compress.TimeSeriesOutputStream;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.column.StockpileColumn;
import ru.yandex.solomon.model.protobuf.MetricType;
import ru.yandex.solomon.model.type.LogHistogram;

/**
 * @author Vladimir Gordiychuk
 */
public class LogHistogramTimeSeriesStreamTest {
    private TimeSeriesOutputStream out;
    private TimeSeriesInputStream in;
    private List<AggrPoint> appendedPoints;

    @Before
    public void setUp() throws Exception {
        out = CompressStreamFactory.createOutputStream(
                MetricType.LOG_HISTOGRAM,
            StockpileColumn.TS.mask() | StockpileColumn.LOG_HISTOGRAM.mask()
        );
        appendedPoints = new ArrayList<>();
    }

    @After
    public void tearDown() throws Exception {
        out.close();
    }

    @Test
    public void serializeDeserializeNonDefaultBase() throws Exception {
        final double base = LogHistogram.DEFAULT_BASE * 2;

        appendPoint(
            AggrPoint.builder()
                .time("2017-06-15T12:40:42Z")
                .logHistogram(
                    LogHistogram.newBuilder()
                        .setBuckets(new double[]{1, 2, 3})
                        .setStartPower(2)
                        .setMaxBucketsSize(10)
                        .setBase(base)
                        .build()
                ).build(),

            AggrPoint.builder()
                .time("2017-06-15T12:45:00Z")
                .logHistogram(
                    LogHistogram.newBuilder()
                        .setBuckets(new double[]{3, 2, 1})
                        .setStartPower(1)
                        .setMaxBucketsSize(10)
                        .setBase(base)
                        .build()
                ).build(),

            AggrPoint.builder()
                .time("2017-06-15T12:50:00Z")
                .logHistogram(
                    LogHistogram.newBuilder()
                        .setBuckets(new double[]{3, 0, 1})
                        .setStartPower(1)
                        .setMaxBucketsSize(10)
                        .setBase(base)
                        .build()
                ).build()
        );

        List<AggrPoint> points = readPoints(StockpileColumn.TS.mask() | StockpileColumn.LOG_HISTOGRAM.mask(), 3);
        Assert.assertThat(points, CoreMatchers.equalTo(appendedPoints));
    }

    @Test
    public void serializeDeserializeChangeBase() throws Exception {
        appendPoint(
            AggrPoint.builder()
                .time("2017-06-15T12:40:42Z")
                .logHistogram(
                    LogHistogram.newBuilder()
                        .setBuckets(new double[]{1, 2, 3})
                        .setStartPower(2)
                        .setMaxBucketsSize(10)
                        .setBase(LogHistogram.DEFAULT_BASE)
                        .build()
                ).build(),

            AggrPoint.builder()
                .time("2017-06-15T12:45:00Z")
                .logHistogram(
                    LogHistogram.newBuilder()
                        .setBuckets(new double[]{3, 2, 1})
                        .setStartPower(1)
                        .setMaxBucketsSize(10)
                        .setBase(LogHistogram.DEFAULT_BASE * 2)
                        .build()
                ).build(),

            AggrPoint.builder()
                .time("2017-06-15T12:50:00Z")
                .logHistogram(
                    LogHistogram.newBuilder()
                        .setBuckets(new double[]{3, 0, 1})
                        .setStartPower(1)
                        .setMaxBucketsSize(10)
                        .setBase(LogHistogram.DEFAULT_BASE * 3)
                        .build()
                ).build()
        );

        List<AggrPoint> points = readPoints(StockpileColumn.TS.mask() | StockpileColumn.LOG_HISTOGRAM.mask(), 3);
        Assert.assertThat(points, CoreMatchers.equalTo(appendedPoints));
    }

    @Test
    public void single() throws Exception {
        LogHistogram source = LogHistogram.newBuilder()
            .setStartPower(-1)
            .setCountZero(2)
            .setBuckets(new double[]{2, 1, 1234, 1})
            .build();

        encode(source);
        LogHistogram result = decode();

        Assert.assertThat(result, CoreMatchers.equalTo(source));
    }

    @Test
    public void fullSame() throws Exception {
        LogHistogram source = LogHistogram.newBuilder()
            .setBuckets(new double[]{1, 2, 3, 4, 5, 6, 7, 0, 0, 12, 123123123})
            .setStartPower(3)
            .setCountZero(5)
            .build();

        encode(source);
        encode(source);

        Assert.assertThat(decode(), CoreMatchers.equalTo(source));
        Assert.assertThat(decode(), CoreMatchers.equalTo(source));
    }

    @Test
    public void sameStartPower() throws Exception {
        LogHistogram first = LogHistogram.newBuilder()
            .setBuckets(new double[]{1, 2, 3})
            .setStartPower(3)
            .setCountZero(5)
            .build();

        LogHistogram second = LogHistogram.newBuilder()
            .setBuckets(new double[]{4, 3, 2, 1})
            .setStartPower(3)
            .setCountZero(0)
            .build();

        encode(first);
        encode(second);

        Assert.assertThat(decode(), CoreMatchers.equalTo(first));
        Assert.assertThat(decode(), CoreMatchers.equalTo(second));
    }

    @Test
    public void sameBucketSize() throws Exception {
        LogHistogram first = LogHistogram.newBuilder()
            .setBuckets(new double[]{1, 2, 3})
            .setStartPower(0)
            .setCountZero(1)
            .build();

        LogHistogram second = LogHistogram.newBuilder()
            .setBuckets(new double[]{3, 2, 1})
            .setStartPower(-3)
            .setCountZero(123)
            .build();

        encode(first);
        encode(second);

        Assert.assertThat(decode(), CoreMatchers.equalTo(first));
        Assert.assertThat(decode(), CoreMatchers.equalTo(second));
    }

    @Test
    public void sameBucketValue() throws Exception {
        LogHistogram first = LogHistogram.newBuilder()
            .setBuckets(new double[]{1, 2, 3})
            .setStartPower(0)
            .setCountZero(1)
            .build();

        LogHistogram second = LogHistogram.newBuilder()
            .setBuckets(new double[]{1, 2, 3})
            .setStartPower(0)
            .setCountZero(123)
            .build();

        encode(first);
        encode(second);

        Assert.assertThat(decode(), CoreMatchers.equalTo(first));
        Assert.assertThat(decode(), CoreMatchers.equalTo(second));
    }

    @Test
    public void sameCountZero() throws Exception {
        LogHistogram first = LogHistogram.newBuilder()
            .setBuckets(new double[]{1, 2, 3})
            .setStartPower(-12)
            .setCountZero(1000)
            .build();

        LogHistogram second = LogHistogram.newBuilder()
            .setBuckets(new double[]{123, 321, 333, 222, 111})
            .setStartPower(0)
            .setCountZero(1000)
            .build();

        encode(first);
        encode(second);

        Assert.assertThat(decode(), CoreMatchers.equalTo(first));
        Assert.assertThat(decode(), CoreMatchers.equalTo(second));
    }

    @Test
    public void differentAtAll() throws Exception {
        LogHistogram first = LogHistogram.newBuilder()
            .setBuckets(new double[]{1, 2, 3, 555})
            .setStartPower(4)
            .setCountZero(13)
            .build();

        LogHistogram second = LogHistogram.newBuilder()
            .setBuckets(new double[]{123, 321, 333, 222, 111})
            .setStartPower(-12)
            .setCountZero(9341)
            .build();

        encode(first);
        encode(second);

        Assert.assertThat(decode(), CoreMatchers.equalTo(first));
        Assert.assertThat(decode(), CoreMatchers.equalTo(second));
    }

    @Test
    public void multipleEncodeDecode() throws Exception {
        LogHistogram[] source = new LogHistogram[] {
            LogHistogram.newBuilder()
                .setBuckets(new double[]{1, 2, 3, 555})
                .setStartPower(4)
                .setCountZero(13)
                .build(),

            LogHistogram.newBuilder()
                .setBuckets(new double[]{1, 2, 3, 555})
                .setStartPower(4)
                .setCountZero(13)
                .build(),

            LogHistogram.newBuilder()
                .setBuckets(new double[]{4})
                .setStartPower(4)
                .setCountZero(0)
                .build(),

            LogHistogram.newBuilder()
                .setBuckets(new double[]{4, 3, 2, 1})
                .setStartPower(4)
                .setCountZero(12)
                .build(),

            LogHistogram.newBuilder()
                .setBuckets(new double[]{1, 3, 3, 1})
                .setStartPower(4)
                .setCountZero(12)
                .build(),

            LogHistogram.newBuilder()
                .setBuckets(new double[]{2, 1, 2})
                .setStartPower(0)
                .setCountZero(13)
                .build()
        };

        encode(source);

        LogHistogram[] result = IntStream.range(0, source.length)
            .mapToObj(index -> decode())
            .toArray(LogHistogram[]::new);

        Assert.assertArrayEquals(source, result);
    }

    @Test
    public void countZeroMaxInt() throws Exception {
        LogHistogram first = LogHistogram.newBuilder()
            .setBuckets(new double[]{1, 2, 3})
            .setStartPower(0)
            .setCountZero(0)
            .build();

        LogHistogram second = LogHistogram.newBuilder()
            .setBuckets(new double[]{1, 2, 3})
            .setStartPower(0)
            .setCountZero(Integer.MAX_VALUE)
            .build();

        encode(first);
        encode(second);

        Assert.assertThat(decode(), CoreMatchers.equalTo(first));
        Assert.assertThat(decode(), CoreMatchers.equalTo(second));
    }

    @Test
    public void startPowerMinInt() throws Exception {
        LogHistogram first = LogHistogram.newBuilder()
            .setBuckets(new double[]{1, 2, 3})
            .setStartPower(10)
            .build();

        LogHistogram second = LogHistogram.newBuilder()
            .setBuckets(new double[]{1, 2, 3})
            .setStartPower(Integer.MIN_VALUE)
            .build();

        encode(first);
        encode(second);

        Assert.assertThat(decode(), CoreMatchers.equalTo(first));
        Assert.assertThat(decode(), CoreMatchers.equalTo(second));
    }

    @Test
    public void maxBucketSize() throws Exception {
        LogHistogram one = LogHistogram.newBuilder()
            .setBuckets(new double[]{1, 2, 3})
            .setMaxBucketsSize(10)
            .build();

        LogHistogram two = LogHistogram.newBuilder()
            .setBuckets(new double[]{1, 0, 2, 1})
            .setMaxBucketsSize(10)
            .build();

        LogHistogram tree = LogHistogram.newBuilder()
            .setBuckets(new double[]{1, 0, 3})
            .setMaxBucketsSize(3)
            .build();

        encode(one, two, tree);

        Assert.assertThat(decode(), CoreMatchers.equalTo(one));
        Assert.assertThat(decode(), CoreMatchers.equalTo(two));
        Assert.assertThat(decode(), CoreMatchers.equalTo(tree));
    }

    private AggrPoint readNextPoint(int mask) {
        if (in == null || !in.hasNext()) {
            in = CompressStreamFactory.createInputStream(MetricType.LOG_HISTOGRAM, mask, out.getCompressedData());
        }

        AggrPoint point = new AggrPoint();
        point.columnSet = mask;
        in.readPoint(point.columnSet, point);
        return point;
    }

    private void encode(LogHistogram... histograms) {
        long timeMillis = Instant.parse("2017-06-15T12:40:42Z").toEpochMilli();
        long stepMillis = TimeUnit.SECONDS.toMillis(30L);

        AggrPoint[] points = new AggrPoint[histograms.length];
        for (int index = 0; index < histograms.length; index++) {
            AggrPoint point = new AggrPoint();
            point.setTsMillis(timeMillis);
            point.setLogHistogram(histograms[index]);

            timeMillis += stepMillis;
            points[index] = point;
        }

        appendPoint(points);
    }

    private LogHistogram decode() {
        AggrPoint point = readNextPoint(StockpileColumn.TS.mask() | StockpileColumn.LOG_HISTOGRAM.mask());
        return point.logHistogram;
    }

    private List<AggrPoint> readPoints(int mask, int count) {
        List<AggrPoint> result = new ArrayList<>(count);

        for (int index = 0; index < count; index++) {
            AggrPoint point = readNextPoint(mask);
            result.add(point);
        }

        return result;
    }

    private void appendPoint(AggrPoint... points) {
        for (AggrPoint point : points) {
            out.writePoint(point.columnSet, point);
            appendedPoints.add(point.withMask(point.columnSet));
        }
    }
}
