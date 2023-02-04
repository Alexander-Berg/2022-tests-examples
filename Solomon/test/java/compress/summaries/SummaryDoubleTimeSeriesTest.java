package ru.yandex.solomon.codec.compress.summaries;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.monlib.metrics.summary.ImmutableSummaryDoubleSnapshot;
import ru.yandex.monlib.metrics.summary.SummaryDoubleSnapshot;
import ru.yandex.solomon.codec.compress.CompressStreamFactory;
import ru.yandex.solomon.codec.compress.TimeSeriesInputStream;
import ru.yandex.solomon.codec.compress.TimeSeriesOutputStream;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.column.StockpileColumn;
import ru.yandex.solomon.model.protobuf.MetricType;

import static org.junit.Assert.assertEquals;

/**
 * @author Vladimir Gordiychuk
 */
public class SummaryDoubleTimeSeriesTest {
    private TimeSeriesOutputStream out;
    private TimeSeriesInputStream in;
    private List<AggrPoint> appendedPoints;

    private static SummaryDoubleSnapshot summary(long count, double sum, double min, double max, double last) {
        return new ImmutableSummaryDoubleSnapshot(count, sum, min, max, last);
    }

    @Before
    public void setUp() throws Exception {
        int mask = StockpileColumn.TS.mask() | StockpileColumn.DSUMMARY.mask();
        out = CompressStreamFactory.createOutputStream(MetricType.DSUMMARY, mask);
        appendedPoints = new ArrayList<>();
    }

    @After
    public void tearDown() throws Exception {
        out.close();
    }

    @Test
    public void one() {
        SummaryDoubleSnapshot summary = summary(10, 123.21, 1.3, 100.4, 0);
        encode(summary);
        SummaryDoubleSnapshot result = decode();

        assertEquals(summary, result);
    }

    @Test
    public void zeroSum() {
        SummaryDoubleSnapshot summary = summary(1, 0, 0, 0, 0);
        encode(summary);
        SummaryDoubleSnapshot result = decode();

        assertEquals(summary, result);
    }

    @Test
    public void many() {
        encode(ImmutableSummaryDoubleSnapshot.EMPTY);
        encode(summary(1, 5.3, 5.1, 5, 0));
        encode(summary(2, 6.7, 1.5, 5.2, 0));
        encode(summary(2, 6.7, 1.5, 5.7, 0));
        encode(summary(3, 11.23, 1.5, 6.8, 0));
        encode(summary(161, 241.12, -12.1, 102.12, 0));

        List<AggrPoint> result = decodeAllPoints();
        assertEquals(appendedPoints, result);
    }

    @Test
    public void maxOnly() {
        encode(summary(0, 0, Double.POSITIVE_INFINITY, 0, 0));
        encode(summary(0, 0, Double.POSITIVE_INFINITY, 5, 0));
        encode(summary(0, 0, Double.POSITIVE_INFINITY, 112, 0));
        encode(summary(0, 0, Double.POSITIVE_INFINITY, 2, 0));
        encode(summary(0, 0, Double.POSITIVE_INFINITY, 2, 0));
        encode(summary(0, 0, Double.POSITIVE_INFINITY, 2.5, 0));

        List<AggrPoint> result = decodeAllPoints();
        assertEquals(appendedPoints, result);
    }

    @Test
    public void minOnly() {
        encode(summary(0, 0, 0, Double.NEGATIVE_INFINITY, 0));
        encode(summary(0, 0, 42, Double.NEGATIVE_INFINITY, 0));
        encode(summary(0, 0, 22, Double.NEGATIVE_INFINITY, 0));
        encode(summary(0, 0, 22, Double.NEGATIVE_INFINITY, 0));
        encode(summary(0, 0, 3, Double.NEGATIVE_INFINITY, 0));
        encode(summary(0, 0, -2.5, Double.NEGATIVE_INFINITY, 0));

        List<AggrPoint> result = decodeAllPoints();
        assertEquals(appendedPoints, result);
    }

    @Test
    public void last() {
        encode(summary(1, 5.3, 5.3, 5.3, 5.3));
        encode(summary(2, 6.7, 1.4, 5.3, 1.4));
        encode(summary(2, 6.7, 1.4, 5.3, 1.4));
        encode(summary(3, 11.23, 1.5, 6.8, 3));
        encode(summary(161, 241.12, -12.1, 102.12, 42.2));

        List<AggrPoint> result = decodeAllPoints();
        assertEquals(appendedPoints, result);
    }

    private void encode(SummaryDoubleSnapshot... summaries) {
        long timeMillis = Instant.parse("2017-06-15T12:40:42Z").toEpochMilli();
        long stepMillis = TimeUnit.SECONDS.toMillis(30L);

        AggrPoint[] points = new AggrPoint[summaries.length];
        for (int index = 0; index < summaries.length; index++) {
            AggrPoint point = new AggrPoint();
            point.setTsMillis(timeMillis);
            point.setSummaryDouble(summaries[index]);

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

    private SummaryDoubleSnapshot decode() {
        AggrPoint point = readNextPoint();
        return point.summaryDouble;
    }

    private AggrPoint readNextPoint() {
        int mask = StockpileColumn.TS.mask() | StockpileColumn.DSUMMARY.mask();
        if (in == null || !in.hasNext()) {
            in = CompressStreamFactory.createInputStream(MetricType.DSUMMARY, mask, out.getCompressedData());
        }

        AggrPoint point = new AggrPoint();
        point.columnSet = mask;
        in.readPoint(point.columnSet, point);
        return point;
    }

    private List<AggrPoint> decodeAllPoints() {
        int mask = StockpileColumn.TS.mask() | StockpileColumn.DSUMMARY.mask();
        TimeSeriesInputStream stream = CompressStreamFactory.createInputStream(MetricType.DSUMMARY, mask, out.getCompressedData());
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
