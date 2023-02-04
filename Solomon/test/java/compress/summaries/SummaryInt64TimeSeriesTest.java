package ru.yandex.solomon.codec.compress.summaries;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.monlib.metrics.summary.ImmutableSummaryInt64Snapshot;
import ru.yandex.monlib.metrics.summary.SummaryInt64Snapshot;
import ru.yandex.solomon.codec.compress.CompressStreamFactory;
import ru.yandex.solomon.codec.compress.TimeSeriesInputStream;
import ru.yandex.solomon.codec.compress.TimeSeriesOutputStream;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.column.StockpileColumn;
import ru.yandex.solomon.model.protobuf.MetricType;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @author Vladimir Gordiychuk
 */
public class SummaryInt64TimeSeriesTest {
    private TimeSeriesOutputStream out;
    private TimeSeriesInputStream in;
    private List<AggrPoint> appendedPoints;

    private static SummaryInt64Snapshot summary(long count, long sum, long min, long max, long last) {
        return new ImmutableSummaryInt64Snapshot(count, sum, min, max, last);
    }

    @Before
    public void setUp() throws Exception {
        int mask = StockpileColumn.TS.mask() | StockpileColumn.ISUMMARY.mask();
        out = CompressStreamFactory.createOutputStream(MetricType.ISUMMARY, mask);
        appendedPoints = new ArrayList<>();
    }

    @After
    public void tearDown() throws Exception {
        out.close();
    }

    @Test
    public void one() {
        SummaryInt64Snapshot summary = summary(10, 123, 1, 100, 0);
        encode(summary);
        SummaryInt64Snapshot result = decode();

        assertThat(result, equalTo(summary));
    }

    @Test
    public void many() {
        encode(ImmutableSummaryInt64Snapshot.EMPTY);
        encode(summary(1, 5, 5, 5, 0));
        encode(summary(2, 6, 1, 5, 0));
        encode(summary(2, 6, 1, 5, 0));
        encode(summary(3, 11, 1, 6, 0));
        encode(summary(161, 241, -12, 102, 0));

        List<AggrPoint> result = decodeAllPoints();
        assertThat(result, equalTo(appendedPoints));
    }

    @Test
    public void last() {
        encode(summary(1, 5, 5, 5, 5));
        encode(summary(2, 6, 1, 5, 1));
        encode(summary(2, 6, 1, 5, 1));
        encode(summary(3, 11, 1, 6, 5));
        encode(summary(161, 241, -12, 102, 42));

        List<AggrPoint> result = decodeAllPoints();
        assertEquals(appendedPoints, result);
    }

    private void encode(SummaryInt64Snapshot... summaries) {
        long timeMillis = Instant.parse("2017-06-15T12:40:42Z").toEpochMilli();
        long stepMillis = TimeUnit.SECONDS.toMillis(30L);

        AggrPoint[] points = new AggrPoint[summaries.length];
        for (int index = 0; index < summaries.length; index++) {
            AggrPoint point = new AggrPoint();
            point.setTsMillis(timeMillis);
            point.setSummaryInt64(summaries[index]);

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

    private SummaryInt64Snapshot decode() {
        AggrPoint point = readNextPoint();
        return point.summaryInt64;
    }

    private AggrPoint readNextPoint() {
        int mask = StockpileColumn.TS.mask() | StockpileColumn.ISUMMARY.mask();
        if (in == null || !in.hasNext()) {
            in = CompressStreamFactory.createInputStream(MetricType.ISUMMARY, mask, out.getCompressedData());
        }

        AggrPoint point = new AggrPoint();
        point.columnSet = mask;
        in.readPoint(point.columnSet, point);
        return point;
    }

    private List<AggrPoint> decodeAllPoints() {
        int mask = StockpileColumn.TS.mask() | StockpileColumn.ISUMMARY.mask();
        TimeSeriesInputStream stream = CompressStreamFactory.createInputStream(MetricType.ISUMMARY, mask, out.getCompressedData());
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
