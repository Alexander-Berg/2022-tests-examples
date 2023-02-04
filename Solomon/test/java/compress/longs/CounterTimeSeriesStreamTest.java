package ru.yandex.solomon.codec.compress.longs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntToLongFunction;
import java.util.stream.IntStream;

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
import ru.yandex.solomon.model.point.column.LongValueRandomData;
import ru.yandex.solomon.model.point.column.StockpileColumns;
import ru.yandex.solomon.model.protobuf.MetricType;

import static org.junit.Assert.assertEquals;
import static ru.yandex.solomon.model.point.AggrPoints.lpoint;

/**
 * @author Vladimir Gordiychuk
 */
@RunWith(Parameterized.class)
public class CounterTimeSeriesStreamTest {
    private final static int MASK = StockpileColumns.minColumnSet(MetricType.COUNTER);

    private TimeSeriesOutputStream out;
    private TimeSeriesInputStream in;
    private List<AggrPoint> appendedPoints;

    @Parameterized.Parameter
    public StockpileFormat format;

    @Parameterized.Parameters(name = "{0}")
    public static Object[] data() {
        return IntStream.range(StockpileFormat.MIN.getFormat(), StockpileFormat.MAX.getFormat() + 1)
                .mapToObj(StockpileFormat::byNumber)
                .toArray();
    }

    @Before
    public void setUp() throws Exception {
        out = CompressStreamFactory.createOutputStream(MetricType.COUNTER, MASK);
        appendedPoints = new ArrayList<>();
    }

    @After
    public void tearDown() throws Exception {
        out.close();
        if (in != null) {
            in.close();
        }
    }

    @Test
    public void onePoint() {
        AggrPoint source = lpoint("2018-08-06T16:58:44Z", 0L);
        appendPoints(source);
        AggrPoint result = readNextPoint();
        assertEquals(source, result);
    }

    @Test
    public void oneMaxLong() {
        AggrPoint source = lpoint("2018-08-06T16:58:44Z", Long.MAX_VALUE);
        appendPoints(source);
        AggrPoint result = readNextPoint();
        assertEquals(source, result);
    }

    @Test
    public void oneMinLong() {
        AggrPoint source = lpoint("2018-08-06T16:58:44Z", Long.MIN_VALUE);
        appendPoints(source);
        AggrPoint result = readNextPoint();
        assertEquals(source, result);
    }

    @Test
    public void growPoints() {
        List<AggrPoint> source = Arrays.asList(
                lpoint("2018-08-06T16:50:00Z", 10L),
                lpoint("2018-08-06T16:51:00Z", 20L),
                lpoint("2018-08-06T16:52:00Z", 30L),
                lpoint("2018-08-06T16:53:00Z", 130L),
                lpoint("2018-08-06T16:54:00Z", 145L));
        appendPoints(source);

        List<AggrPoint> result = readPoints(source.size());
        assertEquals(source, result);
    }

    @Test
    public void samePoints() {
        List<AggrPoint> source = Arrays.asList(
                lpoint("2018-08-06T16:50:00Z", 42L),
                lpoint("2018-08-06T16:51:00Z", 42L),
                lpoint("2018-08-06T16:52:00Z", 42L),
                lpoint("2018-08-06T16:53:00Z", 42L),
                lpoint("2018-08-06T16:54:00Z", 42L));
        appendPoints(source);

        List<AggrPoint> result = readPoints(source.size());
        assertEquals(source, result);
    }

    @Test
    public void upAndDown() {
        List<AggrPoint> source = Arrays.asList(
                lpoint("2018-08-06T16:50:00Z", 5L),
                lpoint("2018-08-06T16:51:00Z", 8L),
                lpoint("2018-08-06T16:52:00Z", 2L),
                lpoint("2018-08-06T16:53:00Z", 3L),
                lpoint("2018-08-06T16:54:00Z", 0L));
        appendPoints(source);

        List<AggrPoint> result = readPoints(source.size());
        assertEquals(source, result);
    }

    @Test
    public void randomPoints() {
        for (int index = 0; index < 100; index++) {
            appendPoints(lpoint(System.currentTimeMillis(), ThreadLocalRandom.current().nextLong()));
        }

        List<AggrPoint> result = readPoints(100);
        assertEquals(appendedPoints, result);
    }

    @Test
    public void counterUnsignedOne() {
        long tsMillis = System.currentTimeMillis();
        long value = Long.MAX_VALUE - 10;
        for (int index = 0; index < 100; index++) {
            value += 1;
            tsMillis += 15_000;
            appendPoints(lpoint(tsMillis, value));
        }

        List<AggrPoint> result = readPoints(100);
        assertEquals(appendedPoints, result);
    }

    @Test
    public void counterUnsignedTwo() {
        long tsMillis = System.currentTimeMillis();
        long value = Long.MAX_VALUE - 10;
        for (int index = 0; index < 100; index++) {
            value += index * 200;
            tsMillis += 15_000;
            appendPoints(lpoint(tsMillis, value));
        }

        List<AggrPoint> result = readPoints(100);
        assertEquals(appendedPoints, result);
    }

    @Test
    public void constZero() {
        test(i -> 0);
    }

    @Test
    public void constOne() {
        test(i -> 1);
    }

    @Test
    public void oneOrZero() {
        test(i -> ThreadLocalRandom.current().nextBoolean() ? 1 : 0);
    }

    @Test
    public void oneOrTwo() {
        test(i -> ThreadLocalRandom.current().nextBoolean() ? 1 : 2);
    }

    @Test
    public void from_0_to_1000() {
        test(i -> ThreadLocalRandom.current().nextInt(1000));
    }

    @Test
    public void from_0_to_1000000() {
        test(i -> ThreadLocalRandom.current().nextInt(1000000));
    }

    @Test
    public void from_1500_to_1600() {
        test(i -> ThreadLocalRandom.current().nextInt(1500, 1600));
    }

    @Test
    public void anyLong() {
        test(i -> ThreadLocalRandom.current().nextLong());
    }

    @Test
    public void anyInt() {
        test(i -> ThreadLocalRandom.current().nextInt());
    }

    @Test
    public void random() {
        test(i -> LongValueRandomData.randomLongValue(ThreadLocalRandom.current()));
    }

    @Test
    public void grow() {
        test(i -> i);
    }

    private void test(IntToLongFunction supplier) {
        long[] source = IntStream.range(0, 500)
                .mapToLong(supplier)
                .toArray();

        long now = System.currentTimeMillis();
        for (long value : source) {
            now += 15_000;
            appendPoints(lpoint(now, value));
        }

        List<AggrPoint> result = readPoints(source.length);
        assertEquals(appendedPoints, result);
    }


    private void appendPoints(List<AggrPoint> points) {
        for (AggrPoint point : points) {
            out.writePoint(point.columnSet, point);
            appendedPoints.add(point.withMask(point.columnSet));
        }
    }

    private void appendPoints(AggrPoint... points) {
        appendPoints(Arrays.asList(points));
    }

    private List<AggrPoint> readPoints(int count) {
        List<AggrPoint> result = new ArrayList<>(count);

        for (int index = 0; index < count; index++) {
            AggrPoint point = readNextPoint();
            result.add(point);
        }

        return result;
    }

    private AggrPoint readNextPoint() {
        if (in == null || !in.hasNext()) {
            in = CompressStreamFactory.createInputStream(MetricType.COUNTER, MASK, out.getCompressedData());
        }

        AggrPoint point = new AggrPoint();
        point.columnSet = MASK;
        in.readPoint(point.columnSet, point);
        return point;
    }

}
