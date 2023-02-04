package ru.yandex.solomon.codec.compress.doubles;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.solomon.codec.bits.BitBufAllocator;
import ru.yandex.solomon.codec.compress.CompressStreamFactory;
import ru.yandex.solomon.codec.compress.TimeSeriesInputStream;
import ru.yandex.solomon.codec.compress.TimeSeriesOutputStream;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.column.StockpileColumn;
import ru.yandex.solomon.model.point.column.ValueColumn;
import ru.yandex.solomon.model.protobuf.MetricType;

/**
 * @author Vladimir Gordiychuk
 */
@ParametersAreNonnullByDefault
public class DoubleTimeSeriesStreamTest {

    private TimeSeriesOutputStream out;
    private TimeSeriesInputStream in;
    private List<AggrPoint> appendedPoints;

    @Before
    public void setUp() throws Exception {
        out = new DoubleTimeSeriesOutputStream(BitBufAllocator.buffer(16), 0);
        appendedPoints = new ArrayList<>();
    }

    @After
    public void tearDown() throws Exception {
        out.close();
    }

    @Test
    public void sequentialGrowAsInt() throws Exception {
        appendPoint(
            point("2017-05-25T08:43:00Z", 1),
            point("2017-05-25T08:43:15Z", 2),
            point("2017-05-25T08:43:30Z", 3),
            point("2017-05-25T08:43:45Z", 4),
            point("2017-05-25T08:44:00Z", 5)
        );

        List<AggrPoint> result = readPoints(StockpileColumn.TS.mask() | StockpileColumn.VALUE.mask(), appendedPoints.size());
        Assert.assertThat(result, CoreMatchers.equalTo(appendedPoints));
    }

    @Test
    public void sequentialGrowAsDouble() throws Exception {
        appendPoint(
            point("2017-05-25T08:43:00Z", 1.5),
            point("2017-05-25T08:43:15Z", 2),
            point("2017-05-25T08:43:30Z", 2.5),
            point("2017-05-25T08:43:45Z", 3),
            point("2017-05-25T08:44:00Z", 3.5),
            point("2017-05-25T08:44:15Z", 4)
        );

        List<AggrPoint> result = readPoints(StockpileColumn.TS.mask() | StockpileColumn.VALUE.mask(), appendedPoints.size());
        Assert.assertThat(result, CoreMatchers.equalTo(appendedPoints));
    }

    @Test
    public void sequentialButInRandomOrder() throws Exception {
        appendPoint(
            point("2017-05-25T08:43:15Z", 2),
            point("2017-05-25T08:44:00Z", 5),
            point("2017-05-25T08:43:30Z", 3),
            point("2017-05-25T08:43:00Z", 1),
            point("2017-05-25T08:43:45Z", 4)
        );

        List<AggrPoint> result = readPoints(StockpileColumn.TS.mask() | StockpileColumn.VALUE.mask(), appendedPoints.size());
        Assert.assertThat(result, CoreMatchers.equalTo(appendedPoints));
    }

    @Test
    public void sameValue() throws Exception {
        appendPoint(
            point("2017-05-25T08:43:15Z", 3),
            point("2017-05-25T08:44:00Z", 3),
            point("2017-05-25T08:43:30Z", 3),
            point("2017-05-25T08:43:00Z", 3),
            point("2017-05-25T08:43:45Z", 3)
        );

        List<AggrPoint> result = readPoints(StockpileColumn.TS.mask() | StockpileColumn.VALUE.mask(), appendedPoints.size());
        Assert.assertThat(result, CoreMatchers.equalTo(appendedPoints));
    }

    @Test
    public void spikes() throws Exception {
        appendPoint(
            point("2017-05-25T08:43:15Z", 1),
            point("2017-05-25T08:44:00Z", 2),
            point("2017-05-25T08:43:30Z", 1_123_123d),
            point("2017-05-25T08:43:00Z", 500),
            point("2017-05-25T08:43:45Z", 3)
        );

        List<AggrPoint> result = readPoints(StockpileColumn.TS.mask() | StockpileColumn.VALUE.mask(), appendedPoints.size());
        Assert.assertThat(result, CoreMatchers.equalTo(appendedPoints));
    }

    @Test
    public void positiveAndNegative() throws Exception {
        appendPoint(
            point("2017-05-25T08:43:00Z", 511),
            point("2017-05-25T08:43:15Z", -123),
            point("2017-05-25T08:43:30Z", 0),
            point("2017-05-25T08:43:45Z", 9123.5),
            point("2017-05-25T08:44:00Z", -333)
        );

        List<AggrPoint> result = readPoints(StockpileColumn.TS.mask() | StockpileColumn.VALUE.mask(), appendedPoints.size());
        Assert.assertThat(result, CoreMatchers.equalTo(appendedPoints));
    }

    @Test
    public void notStableInterval() throws Exception {
        appendPoint(
            point("2017-05-25T08:43:01Z", 511),
            point("2017-05-25T08:43:13Z", -123),
            point("2017-05-25T08:43:21Z", 0),
            point("2017-05-25T08:43:35Z", 9123.5),
            point("2017-05-25T08:44:00Z", -333)
        );

        List<AggrPoint> result = readPoints(StockpileColumn.TS.mask() | StockpileColumn.VALUE.mask(), appendedPoints.size());
        Assert.assertThat(result, CoreMatchers.equalTo(appendedPoints));
    }

    @Test
    public void doubleWithConstantStepMillis() throws Exception {
        appendPoint(
            pointWithStep("2017-05-25T08:43:00Z", 101, 15_000),
            pointWithStep("2017-05-25T08:43:15Z", 5, 15_000),
            pointWithStep("2017-05-25T08:43:30Z", 10, 15_000),
            pointWithStep("2017-05-25T08:43:45Z", 30, 15_000),
            pointWithStep("2017-05-25T08:44:00Z", 101, 15_000)
        );

        List<AggrPoint> result = readPoints(StockpileColumn.TS.mask() | StockpileColumn.VALUE.mask() | StockpileColumn.STEP.mask(), appendedPoints.size());
        Assert.assertThat(result, CoreMatchers.equalTo(appendedPoints));
    }

    @Test
    public void doubleWithChangeStepMillis() throws Exception {
        appendPoint(
            pointWithStep("2017-05-25T08:43:00Z", 101, 15_000),
            pointWithStep("2017-05-25T08:43:15Z", 5, 30_000),
            pointWithStep("2017-05-25T08:43:30Z", 10, 50_000),
            pointWithStep("2017-05-25T08:43:45Z", 30, 15_000),
            pointWithStep("2017-05-25T08:44:00Z", 101, 15_000)
        );

        List<AggrPoint> result = readPoints(StockpileColumn.TS.mask() | StockpileColumn.VALUE.mask() | StockpileColumn.STEP.mask(), appendedPoints.size());
        Assert.assertThat(result, CoreMatchers.equalTo(appendedPoints));
    }

    @Test
    public void doubleWithChangeMergeCount() throws Exception {
        appendPoint(
            pointWithMerge("2017-05-22T11:50:00Z", 12, 5),
            pointWithMerge("2017-05-22T11:55:00Z", 10, 3),
            pointWithMerge("2017-05-29T11:50:00Z", 3, 0),
            pointWithMerge("2017-05-29T11:50:15Z", 2, 0),
            pointWithMerge("2017-05-29T11:50:30Z", 1, 0),
            pointWithMerge("2017-05-29T11:50:45Z", 5, 0)
        );

        int mask = StockpileColumn.TS.mask() | StockpileColumn.VALUE.mask() | StockpileColumn.MERGE.mask() | StockpileColumn.COUNT.mask();
        List<AggrPoint> result = readPoints(mask, appendedPoints.size());
        Assert.assertThat(result, CoreMatchers.equalTo(appendedPoints));
    }

    @Test
    public void doubleWithChangeMergeCountAndStep() throws Exception {
        appendPoint(
            pointWithMergeAndStep("2017-05-22T11:50:00Z", 12, 5, TimeUnit.MINUTES.toMillis(5L)),
            pointWithMergeAndStep("2017-05-22T11:55:00Z", 10, 3, TimeUnit.MINUTES.toMillis(5L)),
            pointWithMergeAndStep("2017-05-29T11:50:00Z", 3, 0, TimeUnit.SECONDS.toMillis(15L)),
            pointWithMergeAndStep("2017-05-29T11:50:15Z", 2, 0, TimeUnit.SECONDS.toMillis(15L)),
            pointWithMergeAndStep("2017-05-29T11:50:30Z", 1, 0, TimeUnit.SECONDS.toMillis(15L)),
            pointWithMergeAndStep("2017-05-29T11:50:45Z", 5, 0, TimeUnit.SECONDS.toMillis(15L))
        );

        int mask = StockpileColumn.TS.mask()
            | StockpileColumn.VALUE.mask()
            | StockpileColumn.MERGE.mask()
            | StockpileColumn.COUNT.mask()
            | StockpileColumn.STEP.mask();

        List<AggrPoint> result = readPoints(mask, appendedPoints.size());
        Assert.assertThat(result, CoreMatchers.equalTo(appendedPoints));
    }

    @Test
    public void sameTime() throws Exception {
        appendPoint(
            point("2017-05-25T08:43:00Z", 3),
            point("2017-05-25T08:43:00Z", 5),
            point("2017-05-25T08:43:00Z", 5),
            point("2017-05-25T08:43:00Z", 1),
            point("2017-05-25T08:43:00Z", 12)
        );

        List<AggrPoint> result = readPoints(StockpileColumn.TS.mask() | StockpileColumn.VALUE.mask() | StockpileColumn.STEP.mask(), appendedPoints.size());
        Assert.assertThat(result, CoreMatchers.equalTo(appendedPoints));
    }

    @Test(expected = RuntimeException.class)
    public void notAbleWriteZeroTs() {
        AggrPoint point = new AggrPoint();
        point.columnSet = StockpileColumn.TS.mask() | StockpileColumn.VALUE.mask();
        point.setValue(42, ValueColumn.DEFAULT_DENOM);

        out.writePoint(point.columnSet, point);
    }

    private AggrPoint readNextPoint(int mask) {
        if (in == null || !in.hasNext()) {
            in = CompressStreamFactory.createInputStream(MetricType.DGAUGE, mask, out.getCompressedData());
        }

        AggrPoint point = new AggrPoint();
        point.columnSet = mask;
        in.readPoint(point.columnSet, point);
        return point;
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

    private static AggrPoint point(String time, double value) {
        return AggrPoint.shortPoint(Instant.parse(time).toEpochMilli(), value);
    }

    private static AggrPoint pointWithStep(String time, double value, int step) {
        return AggrPoint.shortPoint(Instant.parse(time).toEpochMilli(), value, step);
    }

    private static AggrPoint pointWithMerge(String time, double value, int count) {
        AggrPoint point = new AggrPoint();
        point.setTsMillis(Instant.parse(time).toEpochMilli());
        point.setValue(value, ValueColumn.DEFAULT_DENOM);
        point.setMerge(count != 0);
        point.setCount(count);

        return point;
    }

    private static AggrPoint pointWithMergeAndStep(String time, double value, int count, long stepMillis) {
        AggrPoint point = new AggrPoint();
        point.setTsMillis(Instant.parse(time).toEpochMilli());
        point.setValue(value, ValueColumn.DEFAULT_DENOM);
        point.setMerge(count != 0);
        point.setCount(count);
        point.setStepMillis(stepMillis);

        return point;
    }
}
