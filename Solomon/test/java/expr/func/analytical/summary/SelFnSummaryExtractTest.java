package ru.yandex.solomon.expression.expr.func.analytical.summary;

import java.util.DoubleSummaryStatistics;
import java.util.LongSummaryStatistics;
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.monlib.metrics.MetricType;
import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.monlib.metrics.summary.ImmutableSummaryDoubleSnapshot;
import ru.yandex.monlib.metrics.summary.ImmutableSummaryInt64Snapshot;
import ru.yandex.solomon.expression.NamedGraphData;
import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.expression.type.SelTypes;
import ru.yandex.solomon.expression.value.SelValue;
import ru.yandex.solomon.expression.value.SelValueVector;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.solomon.model.point.AggrPoints.dpoint;
import static ru.yandex.solomon.model.point.AggrPoints.lpoint;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class SelFnSummaryExtractTest {
    private NamedGraphData dsummary;
    private NamedGraphData isummary;

    private AggrPoint lsummary(long ts, long... values) {
        AggrPoint point = new AggrPoint();
        LongSummaryStatistics summary = LongStream.of(values).summaryStatistics();
        point.setTsMillis(ts);
        if (values.length == 0) {
            point.setSummaryInt64(null);
        } else {
            point.setSummaryInt64(new ImmutableSummaryInt64Snapshot(
                    summary.getCount(),
                    summary.getSum(),
                    summary.getMin(),
                    summary.getMax(),
                    values[values.length - 1]
            ));
        }
        return point;
    }

    private AggrPoint dsummary(long ts, double... values) {
        AggrPoint point = new AggrPoint();
        DoubleSummaryStatistics summary = DoubleStream.of(values).summaryStatistics();
        point.setTsMillis(ts);
        if (values.length == 0) {
            point.setSummaryDouble(null);
        } else {
            point.setSummaryDouble(new ImmutableSummaryDoubleSnapshot(
                    summary.getCount(),
                    summary.getSum(),
                    summary.getMin(),
                    summary.getMax(),
                    values[values.length - 1]
            ));
        }
        return point;
    }

    @Before
    public void setUp() {
        dsummary = NamedGraphData.newBuilder()
                .setType(MetricType.DSUMMARY)
                .setLabels(Labels.of("sensor", "dsum"))
                .setGraphData(AggrGraphDataArrayList.of(
                        dsummary(1000),
                        dsummary(1500, 42.5),
                        dsummary(2000, 1.5, 3.2, 2.8, 1.7),
                        dsummary(2500, 20d, 30d, 40d, 50d)
                ))
                .build();
        isummary = NamedGraphData.newBuilder()
                .setType(MetricType.ISUMMARY)
                .setLabels(Labels.of("sensor", "isum"))
                .setGraphData(AggrGraphDataArrayList.of(
                        lsummary(1000),
                        lsummary(1500, 42),
                        lsummary(2000, 1, 2, 3, 4, 5),
                        lsummary(2500, 100, 200, 300)
                ))
                .build();
    }

    @Test
    public void onEmptySeries() {
        NamedGraphData emptyLine = NamedGraphData.newBuilder()
                .setGraphData(AggrGraphDataArrayList.empty())
                .setLabels(Labels.of("sensor", "bar"))
                .build();

        NamedGraphData[] result = ProgramTestSupport.expression("summary_avg(graphData);")
                .onMultipleLines(emptyLine)
                .exec()
                .getAsNamedMultipleLines();

        assertThat(result[0], equalTo(NamedGraphData.newBuilder()
                .setType(MetricType.DGAUGE)
                .setLabels(emptyLine.getLabels())
                .setGraphData(ru.yandex.solomon.model.protobuf.MetricType.DGAUGE, AggrGraphDataArrayList.empty())
                .build()));

    }

    @Test
    public void onEmptyVector() {
        SelValue result = ProgramTestSupport.expression("summary_avg(graphData);")
                .exec()
                .getAsSelValue();

        assertThat(result, equalTo(new SelValueVector(SelTypes.GRAPH_DATA, new SelValue[0])));
    }

    @Test
    public void vectorize() {
        NamedGraphData[] result = ProgramTestSupport.expression("summary_avg(graphData);")
                .onMultipleLines(isummary, dsummary)
                .exec()
                .getAsNamedMultipleLines();

        assertThat(result[0], equalTo(NamedGraphData.newBuilder()
                .setType(MetricType.DGAUGE)
                .setLabels(isummary.getLabels())
                .setGraphData(AggrGraphDataArrayList.of(
                        dpoint(1000, Double.NaN),
                        dpoint(1500, 42),
                        dpoint(2000, 3),
                        dpoint(2500, 200)
                ))
                .build()));

        assertThat(result[1], equalTo(NamedGraphData.newBuilder()
                .setType(MetricType.DGAUGE)
                .setLabels(dsummary.getLabels())
                .setGraphData(AggrGraphDataArrayList.of(
                        dpoint(1000, Double.NaN),
                        dpoint(1500, 42.5),
                        dpoint(2000, 2.3),
                        dpoint(2500, 35)
                ))
                .build()));
    }

    @Test
    public void avgIntSummary() {
        NamedGraphData result = ProgramTestSupport.expression("summary_avg(graphData);")
                .onSingleLine(isummary)
                .exec()
                .getAsNamedSingleLine();

        assertThat(result, equalTo(NamedGraphData.newBuilder()
                .setType(MetricType.DGAUGE)
                .setLabels(isummary.getLabels())
                .setGraphData(AggrGraphDataArrayList.of(
                        dpoint(1000, Double.NaN),
                        dpoint(1500, 42),
                        dpoint(2000, 3),
                        dpoint(2500, 200)
                ))
                .build()));
    }

    @Test
    public void avgDoubleSummary() {
        NamedGraphData result = ProgramTestSupport.expression("summary_avg(graphData);")
                .onSingleLine(dsummary)
                .exec()
                .getAsNamedSingleLine();

        assertThat(result, equalTo(NamedGraphData.newBuilder()
                .setType(MetricType.DGAUGE)
                .setLabels(dsummary.getLabels())
                .setGraphData(AggrGraphDataArrayList.of(
                        dpoint(1000, Double.NaN),
                        dpoint(1500, 42.5),
                        dpoint(2000, 2.3),
                        dpoint(2500, 35)
                ))
                .build()));
    }

    @Test
    public void sumIntSummary() {
        NamedGraphData result = ProgramTestSupport.expression("summary_sum(graphData);")
                .onSingleLine(isummary)
                .exec()
                .getAsNamedSingleLine();

        assertThat(result, equalTo(NamedGraphData.newBuilder()
                .setType(MetricType.IGAUGE)
                .setLabels(isummary.getLabels())
                .setGraphData(AggrGraphDataArrayList.of(
                        lpoint(1000, 0),
                        lpoint(1500, 42),
                        lpoint(2000, 15),
                        lpoint(2500, 600)
                ))
                .build()));
    }

    @Test
    public void sumDoubleSummary() {
        NamedGraphData result = ProgramTestSupport.expression("summary_sum(graphData);")
                .onSingleLine(dsummary)
                .exec()
                .getAsNamedSingleLine();

        assertThat(result, equalTo(NamedGraphData.newBuilder()
                .setType(MetricType.DGAUGE)
                .setLabels(dsummary.getLabels())
                .setGraphData(AggrGraphDataArrayList.of(
                        dpoint(1000, 0),
                        dpoint(1500, 42.5),
                        dpoint(2000, 9.2),
                        dpoint(2500, 140)
                ))
                .build()));
    }

    @Test
    public void minIntSummary() {
        NamedGraphData result = ProgramTestSupport.expression("summary_min(graphData);")
                .onSingleLine(isummary)
                .exec()
                .getAsNamedSingleLine();

        assertThat(result, equalTo(NamedGraphData.newBuilder()
                .setType(MetricType.IGAUGE)
                .setLabels(isummary.getLabels())
                .setGraphData(AggrGraphDataArrayList.of(
                        lpoint(1000, Long.MAX_VALUE),
                        lpoint(1500, 42),
                        lpoint(2000, 1),
                        lpoint(2500, 100)
                ))
                .build()));
    }

    @Test
    public void minDoubleSummary() {
        NamedGraphData result = ProgramTestSupport.expression("summary_min(graphData);")
                .onSingleLine(dsummary)
                .exec()
                .getAsNamedSingleLine();

        assertThat(result, equalTo(NamedGraphData.newBuilder()
                .setType(MetricType.DGAUGE)
                .setLabels(dsummary.getLabels())
                .setGraphData(AggrGraphDataArrayList.of(
                        dpoint(1000, Double.POSITIVE_INFINITY),
                        dpoint(1500, 42.5),
                        dpoint(2000, 1.5),
                        dpoint(2500, 20)
                ))
                .build()));
    }

    @Test
    public void maxIntSummary() {
        NamedGraphData result = ProgramTestSupport.expression("summary_max(graphData);")
                .onSingleLine(isummary)
                .exec()
                .getAsNamedSingleLine();

        assertThat(result, equalTo(NamedGraphData.newBuilder()
                .setType(MetricType.IGAUGE)
                .setLabels(isummary.getLabels())
                .setGraphData(AggrGraphDataArrayList.of(
                        lpoint(1000, Long.MIN_VALUE),
                        lpoint(1500, 42),
                        lpoint(2000, 5),
                        lpoint(2500, 300)
                ))
                .build()));
    }

    @Test
    public void maxDoubleSummary() {
        NamedGraphData result = ProgramTestSupport.expression("summary_max(graphData);")
                .onSingleLine(dsummary)
                .exec()
                .getAsNamedSingleLine();

        assertThat(result, equalTo(NamedGraphData.newBuilder()
                .setType(MetricType.DGAUGE)
                .setLabels(dsummary.getLabels())
                .setGraphData(AggrGraphDataArrayList.of(
                        dpoint(1000, Double.NEGATIVE_INFINITY),
                        dpoint(1500, 42.5),
                        dpoint(2000, 3.2),
                        dpoint(2500, 50)
                ))
                .build()));
    }

    @Test
    public void countIntSummary() {
        NamedGraphData result = ProgramTestSupport.expression("summary_count(graphData);")
                .onSingleLine(isummary)
                .exec()
                .getAsNamedSingleLine();

        assertThat(result, equalTo(NamedGraphData.newBuilder()
                .setType(MetricType.IGAUGE)
                .setLabels(isummary.getLabels())
                .setGraphData(AggrGraphDataArrayList.of(
                        lpoint(1000, 0),
                        lpoint(1500, 1),
                        lpoint(2000, 5),
                        lpoint(2500, 3)
                ))
                .build()));
    }

    @Test
    public void countDoubleSummary() {
        NamedGraphData result = ProgramTestSupport.expression("summary_count(graphData);")
                .onSingleLine(dsummary)
                .exec()
                .getAsNamedSingleLine();

        assertThat(result, equalTo(NamedGraphData.newBuilder()
                .setType(MetricType.IGAUGE)
                .setLabels(dsummary.getLabels())
                .setGraphData(AggrGraphDataArrayList.of(
                        lpoint(1000, 0),
                        lpoint(1500, 1),
                        lpoint(2000, 4),
                        lpoint(2500, 4)
                ))
                .build()));
    }

    @Test
    public void lastIntSummary() {
        NamedGraphData result = ProgramTestSupport.expression("summary_last(graphData);")
                .onSingleLine(isummary)
                .exec()
                .getAsNamedSingleLine();

        assertThat(result, equalTo(NamedGraphData.newBuilder()
                .setType(MetricType.DGAUGE)
                .setLabels(isummary.getLabels())
                .setGraphData(AggrGraphDataArrayList.of(
                        dpoint(1000, Double.NaN),
                        dpoint(1500, 42),
                        dpoint(2000, 5),
                        dpoint(2500, 300)
                ))
                .build()));
    }

    @Test
    public void lastDoubleSummary() {
        NamedGraphData result = ProgramTestSupport.expression("summary_last(graphData);")
                .onSingleLine(dsummary)
                .exec()
                .getAsNamedSingleLine();

        assertThat(result, equalTo(NamedGraphData.newBuilder()
                .setType(MetricType.DGAUGE)
                .setLabels(dsummary.getLabels())
                .setGraphData(AggrGraphDataArrayList.of(
                        dpoint(1000, Double.NaN),
                        dpoint(1500, 42.5),
                        dpoint(2000, 1.7),
                        dpoint(2500, 50)
                ))
                .build()));
    }
}
