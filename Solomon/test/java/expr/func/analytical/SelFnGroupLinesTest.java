package ru.yandex.solomon.expression.expr.func.analytical;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.monlib.metrics.MetricType;
import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.expression.NamedGraphData;
import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.expression.type.SelTypes;
import ru.yandex.solomon.expression.value.SelValue;
import ru.yandex.solomon.expression.version.SelVersion;
import ru.yandex.solomon.model.point.AggrPoints;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.protobuf.MetricTypeConverter;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.model.timeseries.Timeline;
import ru.yandex.solomon.model.type.LogHistogram;
import ru.yandex.solomon.util.time.Interval;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.solomon.model.point.AggrPoints.dhistogram;
import static ru.yandex.solomon.model.point.AggrPoints.dpoint;
import static ru.yandex.solomon.model.point.AggrPoints.lpoint;

/**
 * @author Vladimir Gordiychuk
 */
public class SelFnGroupLinesTest {

    @Test
    public void sumWithSingleLineEqualToItLine() throws Exception {
        GraphData source = GraphData.of(
            point("2010-04-24T00:00:00Z", 42),
            point("2010-04-24T00:00:15Z", 43),
            point("2010-04-24T00:00:30Z", 44)
        );

        GraphData result = execExpr("group_lines('sum', graphData);", source);
        Assert.assertThat(result, CoreMatchers.equalTo(source));
    }

    @Test
    public void sumWithTimeAlignedLine() throws Exception {
        GraphData first = GraphData.of(
            point("2010-04-24T00:00:00Z", 10),
            point("2010-04-24T00:00:15Z", 20),
            point("2010-04-24T00:00:30Z", 30)
        );

        GraphData second = GraphData.of(
            point("2010-04-24T00:00:00Z", 5),
            point("2010-04-24T00:00:15Z", 10),
            point("2010-04-24T00:00:30Z", 15)
        );

        GraphData expected = GraphData.of(
            point("2010-04-24T00:00:00Z", 10 + 5),
            point("2010-04-24T00:00:15Z", 20 + 10),
            point("2010-04-24T00:00:30Z", 30 + 15)
        );

        GraphData result = execExpr("group_lines('sum', graphData);", first, second);

        Assert.assertThat(result, CoreMatchers.equalTo(expected));
    }

    @Test
    public void sumWithNotTimeAlignedLine() throws Exception {
        GraphData first = GraphData.of(
            point("2010-04-24T00:00:01Z", 10),
            point("2010-04-24T00:00:16Z", 20),
            point("2010-04-24T00:00:33Z", 30)
        );

        GraphData second = GraphData.of(
            point("2010-04-24T00:00:00Z", 5),
            point("2010-04-24T00:00:15Z", 10),
            point("2010-04-24T00:00:30Z", 15)
        );

        GraphData expected = GraphData.of(
            point("2010-04-24T00:00:00Z", 5),
            point("2010-04-24T00:00:01Z", 10),
            point("2010-04-24T00:00:15Z", 10),
            point("2010-04-24T00:00:16Z", 20),
            point("2010-04-24T00:00:30Z", 15),
            point("2010-04-24T00:00:33Z", 30)
        );

        GraphData result = execExpr("group_lines('sum',graphData);", first, second);

        Assert.assertThat(result, CoreMatchers.equalTo(expected));
    }

    @Test
    public void sumWithNotTimeAlignedLineButWithCommonPoint() throws Exception {
        GraphData first = GraphData.of(
            point("2010-04-24T00:00:00Z", 10),
            point("2010-04-24T00:00:15Z", 20),
            point("2010-04-24T00:00:33Z", 30)
        );

        GraphData second = GraphData.of(
            point("2010-04-24T00:00:00Z", 5),
            point("2010-04-24T00:00:15Z", 10),
            point("2010-04-24T00:00:30Z", 15)
        );

        GraphData expected = GraphData.of(
            point("2010-04-24T00:00:00Z", 10 + 5),
            point("2010-04-24T00:00:15Z", 20 + 10),
            point("2010-04-24T00:00:30Z", 15),
            point("2010-04-24T00:00:33Z", 30)
        );

        GraphData result = execExpr("group_lines('sum', graphData);", first, second);
        Assert.assertThat(result, CoreMatchers.equalTo(expected));
    }

    @Test
    public void groupLinesByMax() throws Exception {
        GraphData first = GraphData.of(
            point("2010-04-24T00:00:00Z", 3),
            point("2010-04-24T00:00:15Z", 9),
            point("2010-04-24T00:00:33Z", 15)
        );

        GraphData second = GraphData.of(
            point("2010-04-24T00:00:00Z", 5),
            point("2010-04-24T00:00:15Z", 3),
            point("2010-04-24T00:00:30Z", 20)
        );

        GraphData expected = GraphData.of(
            point("2010-04-24T00:00:00Z", 5),
            point("2010-04-24T00:00:15Z", 9),
            point("2010-04-24T00:00:30Z", 20),
            point("2010-04-24T00:00:33Z", 15)
        );

        GraphData result = execExpr("group_lines('max', graphData);", first, second);
        Assert.assertThat(result, CoreMatchers.equalTo(expected));
    }

    @Test
    public void groupLinesByMaxAlias() throws Exception {
        GraphData first = GraphData.of(
                point("2010-04-24T00:00:00Z", 3),
                point("2010-04-24T00:00:15Z", 9),
                point("2010-04-24T00:00:33Z", 15)
        );

        GraphData second = GraphData.of(
                point("2010-04-24T00:00:00Z", 5),
                point("2010-04-24T00:00:15Z", 3),
                point("2010-04-24T00:00:30Z", 20)
        );

        GraphData expected = GraphData.of(
                point("2010-04-24T00:00:00Z", 5),
                point("2010-04-24T00:00:15Z", 9),
                point("2010-04-24T00:00:30Z", 20),
                point("2010-04-24T00:00:33Z", 15)
        );

        GraphData result = execExpr("series_max(graphData);", first, second);
        Assert.assertThat(result, CoreMatchers.equalTo(expected));
    }

    @Test
    public void groupLinesByMin() throws Exception {
        GraphData first = GraphData.of(
            point("2010-04-24T00:00:00Z", 3),
            point("2010-04-24T00:00:15Z", 9),
            point("2010-04-24T00:00:33Z", 15)
        );

        GraphData second = GraphData.of(
            point("2010-04-24T00:00:00Z", 5),
            point("2010-04-24T00:00:15Z", 3),
            point("2010-04-24T00:00:30Z", 20)
        );

        GraphData expected = GraphData.of(
            point("2010-04-24T00:00:00Z", 3),
            point("2010-04-24T00:00:15Z", 3),
            point("2010-04-24T00:00:30Z", 20),
            point("2010-04-24T00:00:33Z", 15)
        );

        GraphData result = execExpr("group_lines('min', graphData);", first, second);
        Assert.assertThat(result, CoreMatchers.equalTo(expected));
    }

    @Test
    public void emptyResultOnEmptyGraphData() throws Exception {
        GraphData result = execExpr("group_lines('min', graphData);", GraphData.empty, GraphData.empty, GraphData.empty);
        Assert.assertThat(result, CoreMatchers.equalTo(GraphData.empty));
    }

    @Test
    public void skipEmptyGraphData() throws Exception {
        GraphData first = GraphData.of(
            point("2010-04-24T00:00:00Z", 1),
            point("2010-04-24T00:00:15Z", 2),
            point("2010-04-24T00:00:30Z", 3)
        );

        GraphData second = GraphData.of(
            point("2010-04-24T00:00:00Z", 4),
            point("2010-04-24T00:00:15Z", 5),
            point("2010-04-24T00:00:30Z", 6)
        );

        GraphData third = GraphData.empty;

        GraphData expected = GraphData.of(
            point("2010-04-24T00:00:00Z", 1 + 4),
            point("2010-04-24T00:00:15Z", 2 + 5),
            point("2010-04-24T00:00:30Z", 3 + 6)
        );

        GraphData result = execExpr("group_lines('sum', graphData);", first, second, third);
        Assert.assertThat(result, CoreMatchers.equalTo(expected));
    }

    @Test
    public void groupWithGap() {
        GraphData first = GraphData.of(
            point("2010-04-24T00:00:00Z", 1),
            point("2010-04-24T00:00:15Z", Double.NaN),
            point("2010-04-24T00:00:30Z", 3)
        );

        GraphData second = GraphData.of(
            point("2010-04-24T00:00:00Z", 4),
            point("2010-04-24T00:00:15Z", Double.NaN),
            point("2010-04-24T00:00:30Z", 6)
        );

        GraphData third = GraphData.empty;

        GraphData expected = GraphData.of(
            point("2010-04-24T00:00:00Z", 1 + 4),
            point("2010-04-24T00:00:15Z", Double.NaN),
            point("2010-04-24T00:00:30Z", 3 + 6)
        );

        GraphData result = execExpr("group_lines('sum', graphData);", first, second, third);
        Assert.assertThat(result, CoreMatchers.equalTo(expected));
    }

    @Test
    public void aggregateSinglePoint() throws Exception {
        GraphData first = GraphData.of(point("2010-04-24T00:00:00Z", 2));
        GraphData second = GraphData.of(point("2010-04-24T00:00:00Z", 3));

        GraphData expected = GraphData.of(point("2010-04-24T00:00:00Z", 2));
        GraphData result = execExpr("group_lines('min', graphData);", first, second);
        Assert.assertThat(result, CoreMatchers.equalTo(expected));
    }

    @Test
    public void combineIGauge() {
        var first = NamedGraphData.of(AggrGraphDataArrayList.of(
            lpoint("2010-04-24T00:00:00Z", 10),
            lpoint("2010-04-24T00:00:15Z", 20),
            lpoint("2010-04-24T00:00:33Z", 30)
        ));

        var second = NamedGraphData.of(AggrGraphDataArrayList.of(
            lpoint("2010-04-24T00:00:00Z", 5),
            lpoint("2010-04-24T00:00:15Z", 10),
            lpoint("2010-04-24T00:00:30Z", 15)
        ));

        var expected = AggrGraphDataArrayList.of(
            lpoint("2010-04-24T00:00:00Z", 10 + 5),
            lpoint("2010-04-24T00:00:15Z", 20 + 10),
            lpoint("2010-04-24T00:00:30Z", 15),
            lpoint("2010-04-24T00:00:33Z", 30)
        );

        var result = execExpr("group_lines('sum', graphData);", first, second);
        assertEquals(expected, result.getAggrGraphDataArrayList());
        assertEquals(MetricType.IGAUGE, result.getType());
        assertEquals(ru.yandex.solomon.model.protobuf.MetricType.IGAUGE, result.getDataType());
    }

    @Test
    public void combineIGaugeWithDGauge() {
        var first = NamedGraphData.of(AggrGraphDataArrayList.of(
                lpoint("2010-04-24T00:00:00Z", 10),
                lpoint("2010-04-24T00:00:15Z", 20),
                lpoint("2010-04-24T00:00:33Z", 30)
        ));

        var second = NamedGraphData.of(AggrGraphDataArrayList.of(
                dpoint("2010-04-24T00:00:00Z", 5),
                dpoint("2010-04-24T00:00:15Z", 10),
                dpoint("2010-04-24T00:00:30Z", 15)
        ));

        var expected = AggrGraphDataArrayList.of(
                dpoint("2010-04-24T00:00:00Z", 10 + 5),
                dpoint("2010-04-24T00:00:15Z", 20 + 10),
                dpoint("2010-04-24T00:00:30Z", 15),
                dpoint("2010-04-24T00:00:33Z", 30)
        );

        var result = execExpr("group_lines('sum', graphData);", first, second);
        assertEquals(expected, result.getAggrGraphDataArrayList());
        assertEquals(MetricType.DGAUGE, result.getType());
        assertEquals(MetricTypeConverter.toNotNullProto(MetricType.DGAUGE), result.getDataType());
    }

    @Test
    public void combineHist() {
        double[] bins = new double[] {100, 200, 300};

        var first = NamedGraphData.of(AggrGraphDataArrayList.of(
                AggrPoints.point("2010-04-24T00:00:00Z", dhistogram(bins, new long[] {0, 50, 250})),
                AggrPoints.point("2010-04-24T00:00:15Z", dhistogram(bins, new long[] {10, 20, 50})),
                AggrPoints.point("2010-04-24T00:00:33Z", dhistogram(bins, new long[] {20, 80, 150}))
        ));

        var second = NamedGraphData.of(AggrGraphDataArrayList.of(
                AggrPoints.point("2010-04-24T00:00:00Z", dhistogram(bins, new long[] {80, 5, 50})),
                AggrPoints.point("2010-04-24T00:00:15Z", dhistogram(bins, new long[] {20, 28, 100})),
                AggrPoints.point("2010-04-24T00:00:30Z", dhistogram(bins, new long[] {10, 32, 0}))
        ));

        var expected = AggrGraphDataArrayList.of(
                AggrPoints.point("2010-04-24T00:00:00Z", dhistogram(bins, new long[] {80, 55, 300})),
                AggrPoints.point("2010-04-24T00:00:15Z", dhistogram(bins, new long[] {30, 48, 150})),
                AggrPoints.point("2010-04-24T00:00:30Z", dhistogram(bins, new long[] {10, 32, 0})),
                AggrPoints.point("2010-04-24T00:00:33Z", dhistogram(bins, new long[] {20, 80, 150}))
        );

        var result = execExpr("group_lines('sum', graphData);", first, second);
        assertEquals(expected, result.getAggrGraphDataArrayList());
        assertEquals(MetricType.HIST, result.getType());
        assertEquals(MetricTypeConverter.toNotNullProto(MetricType.HIST), result.getDataType());
    }

    private LogHistogram makeLogHist(double[] buckets) {
        return LogHistogram.newInstance()
                .setBase(1.5)
                .setStartPower(0)
                .setBuckets(buckets);
    }

    @Test
    public void combineHistAndLogHist() {
        double[] bins = new double[] {1, 1.5, 2.25, 3.375};

        var first = NamedGraphData.of(AggrGraphDataArrayList.of(
                AggrPoints.point("2010-04-24T00:00:00Z", dhistogram(bins, new long[] {0, 0, 50, 250})),
                AggrPoints.point("2010-04-24T00:00:15Z", dhistogram(bins, new long[] {0, 10, 20, 50})),
                AggrPoints.point("2010-04-24T00:00:33Z", dhistogram(bins, new long[] {0, 20, 80, 150}))
        ));

        var second = NamedGraphData.of(AggrGraphDataArrayList.of(
                AggrPoints.point("2010-04-24T00:00:00Z", makeLogHist(new double[] {80, 5, 50})),
                AggrPoints.point("2010-04-24T00:00:15Z", makeLogHist(new double[] {20, 28, 100})),
                AggrPoints.point("2010-04-24T00:00:30Z", makeLogHist(new double[] {10, 32, 0}))
        ));

        var expected = AggrGraphDataArrayList.of(
                AggrPoints.point("2010-04-24T00:00:00Z", dhistogram(bins, new long[] {0, 80, 55, 300})),
                AggrPoints.point("2010-04-24T00:00:15Z", dhistogram(bins, new long[] {0, 30, 48, 150})),
                AggrPoints.point("2010-04-24T00:00:30Z", dhistogram(bins, new long[] {0, 10, 32, 0})),
                AggrPoints.point("2010-04-24T00:00:33Z", dhistogram(bins, new long[] {0, 20, 80, 150}))
        );

        var result = execExpr("group_lines('sum', graphData);", first, second);
        assertEquals(expected, result.getAggrGraphDataArrayList());
        assertEquals(MetricType.HIST, result.getType());
        assertEquals(MetricTypeConverter.toNotNullProto(MetricType.HIST), result.getDataType());
    }

    @Test
    public void combineByLabel() {
        String ts1 = "2017-12-01T00:00:00Z";
        String ts2 = "2017-12-01T00:01:00Z";

        NamedGraphData[] source = {
            graph(Labels.of("host", "test-1", "sensor", "freeSpace", "disk", "/dev/sda1"), point(ts1, 100), point(ts2, 142)),
            graph(Labels.of("host", "test-2", "sensor", "freeSpace", "disk", "/dev/sda1"), point(ts1, 108), point(ts2, 2000)),
            graph(Labels.of("host", "test-2", "sensor", "freeSpace", "disk", "/dev/sda2"), point(ts1, 123), point(ts2, 321)),
            graph(Labels.of("host", "test-1", "sensor", "freeSpace", "disk", "/dev/sdb"), point(ts1, 1015), point(ts2, 2411))
        };

        GraphData[] result = ProgramTestSupport.expression("group_lines('sum', 'host', graphData);")
            .onMultipleLines(source)
            .exec()
            .getAsMultipleLines();

        assertThat(result, allOf(
            arrayWithSize(2),
            hasItemInArray(GraphData.of(point(ts1, 100 + 1015), point(ts2, 142 + 2411))),
            hasItemInArray(GraphData.of(point(ts1, 108 + 123), point(ts2, 2000 + 321)))
        ));
    }

    @Test
    public void combineByLabelAlias() {
        String ts1 = "2017-12-01T00:00:00Z";
        String ts2 = "2017-12-01T00:01:00Z";

        NamedGraphData[] source = {
                graph(Labels.of("host", "test-1", "sensor", "freeSpace", "disk", "/dev/sda1"), point(ts1, 100), point(ts2, 142)),
                graph(Labels.of("host", "test-2", "sensor", "freeSpace", "disk", "/dev/sda1"), point(ts1, 108), point(ts2, 2000)),
                graph(Labels.of("host", "test-2", "sensor", "freeSpace", "disk", "/dev/sda2"), point(ts1, 123), point(ts2, 321)),
                graph(Labels.of("host", "test-1", "sensor", "freeSpace", "disk", "/dev/sdb"), point(ts1, 1015), point(ts2, 2411))
        };

        GraphData[] result = ProgramTestSupport.expression("series_sum('host', graphData);")
                .onMultipleLines(source)
                .exec()
                .getAsMultipleLines();

        assertThat(result, allOf(
                arrayWithSize(2),
                hasItemInArray(GraphData.of(point(ts1, 100 + 1015), point(ts2, 142 + 2411))),
                hasItemInArray(GraphData.of(point(ts1, 108 + 123), point(ts2, 2000 + 321)))
        ));
    }

    @Test
    public void combineByLabels() {
        String ts1 = "2017-12-01T00:00:00Z";
        String ts2 = "2017-12-01T00:01:00Z";

        NamedGraphData[] source = {
            graph(Labels.of("host", "test-1", "sensor", "freeSpace", "disk", "/dev/sda1"), point(ts1, 100), point(ts2, 142)),
            graph(Labels.of("host", "test-2", "sensor", "freeSpace", "disk", "/dev/sda1"), point(ts1, 108), point(ts2, 2000)),
            graph(Labels.of("host", "test-2", "sensor", "freeSpace", "disk", "/dev/sda2"), point(ts1, 123), point(ts2, 321)),
            graph(Labels.of("host", "test-1", "sensor", "freeSpace", "disk", "/dev/sdb"), point(ts1, 1015), point(ts2, 2411))
        };

        GraphData[] result = ProgramTestSupport.expression("group_lines('sum', as_vector('host', 'sensor'), graphData);")
            .onMultipleLines(source)
            .exec()
            .getAsMultipleLines();

        assertThat(result, allOf(
            arrayWithSize(2),
            hasItemInArray(GraphData.of(point(ts1, 100 + 1015), point(ts2, 142 + 2411))),
            hasItemInArray(GraphData.of(point(ts1, 108 + 123), point(ts2, 2000 + 321)))
        ));
    }

    @Test
    public void combineByLabelsAlias() {
        String ts1 = "2017-12-01T00:00:00Z";
        String ts2 = "2017-12-01T00:01:00Z";

        NamedGraphData[] source = {
                graph(Labels.of("host", "test-1", "sensor", "freeSpace", "disk", "/dev/sda1"), point(ts1, 100), point(ts2, 142)),
                graph(Labels.of("host", "test-2", "sensor", "freeSpace", "disk", "/dev/sda1"), point(ts1, 108), point(ts2, 2000)),
                graph(Labels.of("host", "test-2", "sensor", "freeSpace", "disk", "/dev/sda2"), point(ts1, 123), point(ts2, 321)),
                graph(Labels.of("host", "test-1", "sensor", "freeSpace", "disk", "/dev/sdb"), point(ts1, 1015), point(ts2, 2411))
        };

        GraphData[] result = ProgramTestSupport.expression("series_sum(['host', 'sensor'], graphData);")
                .onMultipleLines(source)
                .exec()
                .getAsMultipleLines();

        assertThat(result, allOf(
                arrayWithSize(2),
                hasItemInArray(GraphData.of(point(ts1, 100 + 1015), point(ts2, 142 + 2411))),
                hasItemInArray(GraphData.of(point(ts1, 108 + 123), point(ts2, 2000 + 321)))
        ));
    }

    @Test
    public void spuriousLabelsRemoved() {
        String ts1 = "2017-12-01T00:00:00Z";

        NamedGraphData[] source = {
                graph(Labels.of("host", "a", "disk", "/dev/sda1"), point(ts1, 100)),
                graph(Labels.of("host", "a", "disk", "/dev/sda1"), point(ts1, 101)),
                graph(Labels.of("host", "b", "disk", "/dev/sda1"), point(ts1, 108)),
                graph(Labels.of("host", "b", "disk", "/dev/sda2"), point(ts1, 123)),
        };

        NamedGraphData[] result = ProgramTestSupport.expression("series_sum('host', graphData);")
                .onMultipleLines(source)
                .exec()
                .getAsNamedMultipleLines();

        Arrays.stream(result)
                .map(NamedGraphData::getLabels)
                .forEach(labels -> {
                    assertThat(labels.size(), equalTo(1));
                    assertThat(labels.findByKey("host"), notNullValue());
                });
    }

    @Test
    public void returnTypeByVersion() {
        GraphData source = GraphData.of(
                point("2010-04-24T00:00:00Z", 42),
                point("2010-04-24T00:00:15Z", 43),
                point("2010-04-24T00:00:30Z", 44)
        );

        SelValue scalar = execExpr(SelVersion.BASIC_1, "group_lines('sum', graphData);", source);
        SelValue vector = execExpr(SelVersion.GROUP_LINES_RETURN_VECTOR_2, "group_lines('sum', graphData);", source);

        Assert.assertEquals(SelTypes.GRAPH_DATA, scalar.type());
        Assert.assertEquals(SelTypes.GRAPH_DATA_VECTOR, vector.type());
        Assert.assertEquals(scalar, vector.castToVector().item(0));
    }

    private NamedGraphData graph(Labels labels, DataPoint... points) {
        return NamedGraphData.of(MetricType.DGAUGE, labels, points);
    }

    private SelValue execExpr(SelVersion version, String expression, GraphData... source) {
        Interval interval = Arrays.stream(source)
                .filter(gd -> !gd.isEmpty())
                .map(GraphData::getTimeline)
                .map(Timeline::interval)
                .reduce(Interval::convexHull)
                .orElseGet(() -> Interval.before(Instant.now(), Duration.ofDays(1)));

        return ProgramTestSupport.expression(expression)
                .forTimeInterval(interval)
                .onMultipleLines(source)
                .exec(version)
                .getAsSelValue();
    }

    private GraphData execExpr(String expression, GraphData... source) {
        Interval interval = Arrays.stream(source)
            .filter(gd -> !gd.isEmpty())
            .map(GraphData::getTimeline)
            .map(Timeline::interval)
            .reduce(Interval::convexHull)
            .orElseGet(() -> Interval.before(Instant.now(), Duration.ofDays(1)));

        return ProgramTestSupport.expression(expression)
            .forTimeInterval(interval)
            .onMultipleLines(source)
            .exec(ProgramTestSupport.ExecResult::getAsSingleLine);
    }

    private NamedGraphData execExpr(String expression, NamedGraphData... source) {
        Interval interval = Arrays.stream(source)
            .map(NamedGraphData::getAggrGraphDataArrayList)
            .filter(gd -> !gd.isEmpty())
            .map(list -> {
                long from = list.getTsMillis(0);
                long to = list.getTsMillis(list.length() - 1);
                return Interval.millis(from, to);
            })
            .reduce(Interval::convexHull)
            .orElseGet(() -> Interval.before(Instant.now(), Duration.ofDays(1)));

        return ProgramTestSupport.expression(expression)
            .forTimeInterval(interval)
            .onMultipleLines(source)
            .exec(ProgramTestSupport.ExecResult::getAsNamedSingleLine);
    }

    private DataPoint point(String time, double value) {
        return new DataPoint(Instant.parse(time), value);
    }
}
