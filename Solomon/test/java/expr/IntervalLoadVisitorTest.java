package ru.yandex.solomon.expression.expr;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.solomon.expression.NamedGraphData;
import ru.yandex.solomon.expression.analytics.GraphDataLoaderStub;
import ru.yandex.solomon.expression.analytics.Program;
import ru.yandex.solomon.expression.test.ForEachSelVersionRunner;
import ru.yandex.solomon.expression.test.VersionedSelTestBase;
import ru.yandex.solomon.expression.version.SelVersion;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.point.RecyclableAggrPoint;
import ru.yandex.solomon.model.point.column.StockpileColumns;
import ru.yandex.solomon.model.protobuf.MetricType;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.solomon.model.timeseries.AggrGraphDataIterable;
import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.model.timeseries.iterator.AggrPointCursor;
import ru.yandex.solomon.model.timeseries.iterator.GenericCombineIterator;
import ru.yandex.solomon.model.timeseries.iterator.GenericIterator;
import ru.yandex.solomon.model.timeseries.iterator.GenericPointCollector;
import ru.yandex.solomon.util.time.Interval;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
@RunWith(ForEachSelVersionRunner.class)
public class IntervalLoadVisitorTest extends VersionedSelTestBase {
    private Interval intervalShort;
    private Interval intervalLong;
    private Duration grid;
    private GraphDataLoaderStub graphLoader;

    public IntervalLoadVisitorTest(SelVersion version) {
        super(version);
    }

    @Before
    public void setUp() {
        Instant now = Instant.parse("2021-02-03T12:00:00Z");

        this.graphLoader = new GraphDataLoaderStub();
        this.grid = Duration.ofMinutes(5);
        AggrGraphDataArrayList data = new AggrGraphDataArrayList(StockpileColumns.minColumnSet(MetricType.DGAUGE), 10000);

        for (Instant i = now.minus(Duration.ofDays(7)); i.isBefore(now); i = i.plus(grid)) {
            data.addRecordShort(i.toEpochMilli(), Math.sin(i.toEpochMilli() / 454535.));
        }
        this.intervalShort = new Interval(now.minus(Duration.ofHours(1)), now);
        this.intervalLong = new Interval(now.minus(Duration.ofDays(1)), now);
        graphLoader.putSelectorValue("{sensor=test}", NamedGraphData.newBuilder()
                .setGraphData(MetricType.DGAUGE, data)
                .build());
    }

    private <T> void forEach(GenericIterator<T> data, T point, Consumer<T> action) {
        while (data.next(point)) {
            action.accept(point);
        }
    }

    private <T> void forEach(AggrGraphDataIterable data, Consumer<AggrPoint> action) {
        RecyclableAggrPoint point = RecyclableAggrPoint.newInstance();
        try {
            forEach(data.iterator(), point, action);
        } finally {
            point.recycle();
        }
    }

    private AggrGraphDataIterable exec(String programCode, Interval interval) {
        return Program.fromSourceWithReturn(version, programCode, false)
                .compile()
                .prepare(interval)
                .evaluate(graphLoader, Map.of())
                .get("external$0")
                .castToGraphData()
                .getNamedGraphData()
                .getAggrGraphDataArrayList();
    }

    private void testResultInsideInterval(String programCode) {
        long beginIntervalMillis = intervalShort.getBeginMillis();
        long endIntervalMillis = intervalShort.getEndMillis();

        var result = exec(programCode, intervalShort);
        Assert.assertFalse(result.isEmpty()); // Sanity
        forEach(result, point -> {
            String message = Instant.ofEpochMilli(point.tsMillis) + " is outside of " + intervalShort;
            Assert.assertTrue(message, point.tsMillis >= beginIntervalMillis);
            Assert.assertTrue(message, point.tsMillis <= endIntervalMillis);
        });
    }

    @Test
    public void movingAvg() {
        testResultInsideInterval("moving_avg({sensor=test}, 7d)");
    }

    @Test
    public void movingPerc() {
        testResultInsideInterval("moving_percentile({sensor=test}, 7d, 50)");
    }

    @Test
    public void asap() {
        testResultInsideInterval("asap({sensor=test})");
    }

    @Test
    public void shift() {
        testResultInsideInterval("shift({sensor=test}, 1d)");
    }

    @Test
    public void chainedMoving() {
        String programCode = "moving_avg(moving_percentile({sensor=test}, 1d, 50), 1d)";

        var resultShort = exec(programCode, intervalShort);
        var resultLong = exec(programCode, intervalLong);

        var cursors = List.of(resultShort, resultLong).stream()
                .map(AggrGraphDataIterable::iterator)
                .map(AggrPointCursor::new)
                .collect(Collectors.toList());

        Assert.assertEquals(intervalLong.duration().dividedBy(grid), resultLong.getRecordCount(), 3d);
        Assert.assertEquals(intervalShort.duration().dividedBy(grid), resultShort.getRecordCount(), 3d);

        var combined = new GenericCombineIterator<>(cursors, new GenericPointCollector<AggrPointCursor, Void>() {
            private final AggrPoint[] points = new AggrPoint[2];

            @Override
            public void reset() {
                points[0] = points[1] = null;
            }

            @Override
            public void append(int cursorIndex, AggrPointCursor cursor) {
                points[cursorIndex] = cursor.getPoint();
            }

            @Override
            public void compute(long timestamp, Void target) {
                if (points[0] != null && points[1] != null) {
                    double val0 = points[0].getValueDivided();
                    double val1 = points[1].getValueDivided();
                    Assert.assertEquals("At " + Instant.ofEpochMilli(timestamp), val0, val1, 1e-10);
                }
            }
        });

        forEach(combined, null, aVoid -> {});
    }

    @Test
    public void maxMovingAvg() {
        Instant now = intervalShort.getEnd().plusSeconds(60);
        graphLoader.putSelectorValue("{sensor=test2}", GraphData.of(
                DataPoint.point(now.minus(Duration.ofMinutes(120)).toEpochMilli(), 1000),
                DataPoint.point(now.minus(Duration.ofMinutes(100)).toEpochMilli(), 38),
                DataPoint.point(now.minus(Duration.ofMinutes(80)).toEpochMilli(), 12),
                DataPoint.point(now.minus(Duration.ofMinutes(60)).toEpochMilli(), 14),
                DataPoint.point(now.minus(Duration.ofMinutes(40)).toEpochMilli(), 8),
                DataPoint.point(now.minus(Duration.ofMinutes(20)).toEpochMilli(), 20)
        ));

        String prog = "constant_line(max(moving_avg({sensor=test2}, 1h)))";

        var result = exec(prog, intervalShort);
        forEach(result, point -> {
            Assert.assertTrue(point.getValueDivided() < 300);
        });
    }
}
