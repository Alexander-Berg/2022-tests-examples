package ru.yandex.solomon.expression.expr.func.analytical.histogram;

import java.time.Instant;

import org.junit.Test;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.expression.NamedGraphData;
import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.expression.type.SelTypes;
import ru.yandex.solomon.expression.value.SelValue;
import ru.yandex.solomon.model.point.AggrPoints;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.protobuf.MetricType;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.model.type.LogHistogram;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.solomon.model.point.AggrPoints.dhistogram;

/**
 * @author Ivan Tsybulin
 */
public class SelFnHistogramAggrTest {

    @Test
    public void emptyNoPoints() {
        NamedGraphData[] lines = new NamedGraphData[] {
                NamedGraphData.of(Labels.of("sensor", "responseTime", "bin", "100")),
                NamedGraphData.of(Labels.of("sensor", "responseTime", "bin", "200")),
                NamedGraphData.of(Labels.of("sensor", "responseTime", "bin", "300"))
        };

        GraphData result = ProgramTestSupport.expression("histogram_sum(graphData);")
            .onMultipleLines(lines)
            .exec(ProgramTestSupport.ExecResult::getAsSingleLine);

        assertThat(result, equalTo(GraphData.empty));

        GraphData result2 = ProgramTestSupport.expression("histogram_avg(graphData);")
                .onMultipleLines(lines)
                .exec(ProgramTestSupport.ExecResult::getAsSingleLine);

        assertThat(result2, equalTo(GraphData.empty));
    }

    @Test
    public void emptyNoMetrics() {
        GraphData result = ProgramTestSupport.expression("histogram_sum(group_by_time(15s, 'sum', graphData));")
            .onMultipleLines(new NamedGraphData[0])
            .exec(ProgramTestSupport.ExecResult::getAsSingleLine);

        assertThat(result, equalTo(GraphData.empty));

        GraphData result2 = ProgramTestSupport.expression("histogram_avg(group_by_time(15s, 'sum', graphData));")
                .onMultipleLines(new NamedGraphData[0])
                .exec(ProgramTestSupport.ExecResult::getAsSingleLine);

        assertThat(result2, equalTo(GraphData.empty));
    }

    @Test
    public void useBinLabelAsBucketLimitByDefault() {
        Labels labels = Labels.of("subsystem", "metabase", "sensor", "responseTime", "endpoint", "find");

        String ts1 = "2017-12-01T00:00:00Z";
        String ts2 = "2017-12-01T00:01:00Z";
        String ts3 = "2017-12-01T00:02:00Z";

        NamedGraphData bin100 = NamedGraphData.of(labels.add("bin", "100"), point(ts1, 50d), point(ts2, 30d), point(ts3, 1d));
        NamedGraphData bin200 = NamedGraphData.of(labels.add("bin", "200"), point(ts1, 10d), point(ts2, 40d), point(ts3, 2d));
        NamedGraphData bin300 = NamedGraphData.of(labels.add("bin", "300"), point(ts1, 2d), point(ts2, 1d), point(ts3, 50d));
        NamedGraphData bin999 = NamedGraphData.of(labels.add("bin", "999"), point(ts1, 0d), point(ts2, 0d), point(ts3, 0d));

        GraphData result = ProgramTestSupport.expression("histogram_sum(graphData);")
            .onMultipleLines(bin100, bin200, bin300, bin999)
            .exec(ProgramTestSupport.ExecResult::getAsSingleLine);

        GraphData expected = GraphData.of(
            point(ts1, 50d * 50d + 150d * 10d + 250d *  2d),
            point(ts2, 50d * 30d + 150d * 40d + 250d *  1d),
            point(ts3, 50d *  1d + 150d *  2d + 250d * 50d)
        );

        assertEquals(expected, result);

        GraphData result2 = ProgramTestSupport.expression("histogram_avg(graphData);")
                .onMultipleLines(bin100, bin200, bin300, bin999)
                .exec(ProgramTestSupport.ExecResult::getAsSingleLine);

        GraphData expected2 = GraphData.of(
                point(ts1, (50d * 50d + 150d * 10d + 250d *  2d) / (50d + 10d + 2d)),
                point(ts2, (50d * 30d + 150d * 40d + 250d *  1d) / (30d + 40d + 1d)),
                point(ts3, (50d *  1d + 150d *  2d + 250d * 50d) / (1d + 2d + 50d))
        );

        assertEquals(expected2, result2);
    }

    @Test
    public void useCustomBinLabel() {
        Labels labels = Labels.of("subsystem", "metabase", "sensor", "responseTime", "endpoint", "find");
        String ts1 = "2017-12-01T00:00:00Z";

        NamedGraphData bin100 = NamedGraphData.of(labels.add("bucket", "100"), point(ts1, 12d));
        NamedGraphData bin200 = NamedGraphData.of(labels.add("bucket", "200"), point(ts1, 50d));
        NamedGraphData bin300 = NamedGraphData.of(labels.add("bucket", "300"), point(ts1, Double.NaN));
        NamedGraphData binInf = NamedGraphData.of(labels.add("bucket", "inf"), point(ts1, 0d));

        GraphData result = ProgramTestSupport.expression("histogram_sum('bucket', graphData);")
            .onMultipleLines(bin100, bin200, bin300, binInf)
            .exec(ProgramTestSupport.ExecResult::getAsSingleLine);

        GraphData expected = GraphData.of(point(ts1, 50d * 12d + 150d * 50d));
        assertEquals(expected, result);

        GraphData result2 = ProgramTestSupport.expression("histogram_avg('bucket', graphData);")
                .onMultipleLines(bin100, bin200, bin300, binInf)
                .exec(ProgramTestSupport.ExecResult::getAsSingleLine);

        GraphData expected2 = GraphData.of(point(ts1, (50d * 12d + 150d * 50d) / (12d + 50d)));
        assertEquals(expected2, result2);
    }

    @Test
    public void parseBinSizeFromLabel() {
        Labels labels = Labels.of("subsystem", "metabase", "sensor", "responseTime", "endpoint", "find");
        String ts1 = "2017-12-01T00:00:00Z";

        NamedGraphData bin100 = NamedGraphData.of(labels.add("bin", "100ms"), point(ts1, 12d));
        NamedGraphData bin200 = NamedGraphData.of(labels.add("bin", "200ms"), point(ts1, 50d));
        NamedGraphData bin300 = NamedGraphData.of(labels.add("bin", "300ms"), point(ts1, Double.NaN));
        NamedGraphData binInf = NamedGraphData.of(labels.add("bin", "inf"), point(ts1, 0d));

        GraphData result = ProgramTestSupport.expression("histogram_sum(graphData);")
            .onMultipleLines(bin100, bin200, bin300, binInf)
            .exec(ProgramTestSupport.ExecResult::getAsSingleLine);

        GraphData expected = GraphData.of(point(ts1, 50d * 12d + 150d * 50d));
        assertEquals(expected, result);

        GraphData result2 = ProgramTestSupport.expression("histogram_avg(graphData);")
                .onMultipleLines(bin100, bin200, bin300, binInf)
                .exec(ProgramTestSupport.ExecResult::getAsSingleLine);

        GraphData expected2 = GraphData.of(point(ts1, (50d * 12d + 150d * 50d) / (12d + 50d)));
        assertEquals(expected2, result2);
    }

    @Test
    public void infBucket() {
        Labels labels = Labels.of("subsystem", "metabase", "sensor", "responseTime", "endpoint", "find");
        String ts1 = "2017-12-01T00:00:00Z";

        NamedGraphData bin100 = NamedGraphData.of(labels.add("bucket", "100"), point(ts1, 200d));
        NamedGraphData bin200 = NamedGraphData.of(labels.add("bucket", "200"), point(ts1, 50d));
        NamedGraphData bin300 = NamedGraphData.of(labels.add("bucket", "300"), point(ts1, Double.NaN));
        NamedGraphData binInf = NamedGraphData.of(labels.add("bucket", "inf"), point(ts1, 250d));

        NamedGraphData result = ProgramTestSupport.expression("histogram_sum('bucket', graphData);")
            .onMultipleLines(bin100, bin200, bin300, binInf)
            .exec()
            .getAsNamedSingleLine();

        assertThat(result, equalTo(NamedGraphData.newBuilder()
                .setLabels(labels)
                .setGraphData(GraphData.of(point(ts1, 50d * 200d + 150d * 50d + 300d * 250d)))
                .build()));
    }

    @Test
    public void histogram() {
        String ts = "2018-11-23T00:00:00Z";

        NamedGraphData source = new NamedGraphData(
            "",
            ru.yandex.monlib.metrics.MetricType.HIST_RATE,
            "histogram",
            Labels.of(),
            AggrGraphDataArrayList.of(
                AggrPoints.point(ts, dhistogram(new double[] {100, 200, 300}, new long[] {0, 50, 250 }))
            )
        );

        GraphData result = ProgramTestSupport.expression("histogram_sum(graphData);")
            .onMultipleLines(source)
            .exec(ProgramTestSupport.ExecResult::getAsSingleLine);

        GraphData expected = GraphData.of(point(ts, 50d * 0 + 150d * 50 + 250d * 250));
        assertThat(result, equalTo(expected));

        GraphData result2 = ProgramTestSupport.expression("histogram_avg(graphData);")
                .onMultipleLines(source)
                .exec(ProgramTestSupport.ExecResult::getAsSingleLine);

        GraphData expected2 = GraphData.of(point(ts, (50d * 0 + 150d * 50 + 250d * 250) / (50 + 250)));
        assertThat(result2, equalTo(expected2));
    }

    private double mean(double base, double aPow, double bPow) {
        return Math.pow(base, 0.5 * (aPow + bPow));
    }

    @Test
    public void logHistogram() {
        NamedGraphData logHist = NamedGraphData.newBuilder()
                .setLabels(Labels.of("signal", "resp-time_hgram"))
                .setType(ru.yandex.monlib.metrics.MetricType.LOG_HISTOGRAM)
                .setGraphData(MetricType.LOG_HISTOGRAM, AggrGraphDataArrayList.of(
                        AggrPoints.point(1000, (LogHistogram) null),
                        AggrPoints.point(1500, LogHistogram.newBuilder().build()),
                        AggrPoints.point(2000, LogHistogram.newBuilder()
                                .setBase(2)
                                .setStartPower(2)
                                .setCountZero(2)
                                .setBuckets(new double[] {0, 3, 4, 0, 1})
                                .build()),
                        AggrPoints.point(2500, LogHistogram.newBuilder()
                                .setBase(1.5)
                                .setStartPower(3)
                                .setCountZero(0)
                                .setBuckets(new double[] {1, 2, 3})
                                .build())))
                .build();

        double sumFirst = 0 +
                0d * mean(2, 2, 3) +
                3d * mean(2, 3, 4) +
                4d * mean(2, 4, 5) +
                0d * mean(2, 5, 6) +
                1d * mean(2, 6, 7);
        double countFirst = 2d + 0d + 3d + 4d + 0d + 1d;
        double sumSecond = 0 +
                1d * mean(1.5, 3, 4) +
                2d * mean(1.5, 4, 5) +
                3d * mean(1.5, 5, 6);
        double countSecond = 1d + 2d + 3d;

        NamedGraphData result = ProgramTestSupport
                .expression("histogram_sum(graphData);")
                .onMultipleLines(logHist)
                .exec()
                .getAsNamedSingleLine();

        assertThat(result, equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setLabels(Labels.of("signal", "resp-time_hgram"))
                .setGraphData(GraphData.of(
                        DataPoint.point(1000, 0),
                        DataPoint.point(1500, 0),
                        DataPoint.point(2000, sumFirst),
                        DataPoint.point(2500, sumSecond)
                ))
                .build()));

        NamedGraphData result2 = ProgramTestSupport
                .expression("histogram_avg(graphData);")
                .onMultipleLines(logHist)
                .exec()
                .getAsNamedSingleLine();

        assertThat(result2, equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setLabels(Labels.of("signal", "resp-time_hgram"))
                .setGraphData(GraphData.of(
                        DataPoint.point(1000, Double.NaN),
                        DataPoint.point(1500, Double.NaN),
                        DataPoint.point(2000, sumFirst / countFirst),
                        DataPoint.point(2500, sumSecond / countSecond)
                ))
                .build()));
    }

    @Test
    public void returnType() {
        String ts = "2018-11-23T00:00:00Z";

        NamedGraphData source = new NamedGraphData(
                "",
                ru.yandex.monlib.metrics.MetricType.HIST_RATE,
                "histogram",
                Labels.of(),
                MetricType.HIST_RATE,
                AggrGraphDataArrayList.of(
                        AggrPoints.point(ts, dhistogram(new double[] {100, 200, 300}, new long[] {0, 50, 250 }))
                )
        );

        SelValue result = ProgramTestSupport.expression("histogram_sum(graphData);")
                .onMultipleLines(source)
                .exec()
                .getAsSelValue();

        assertThat(result.type(), equalTo(SelTypes.GRAPH_DATA_VECTOR));

        SelValue result2 = ProgramTestSupport.expression("histogram_avg(graphData);")
                .onMultipleLines(source)
                .exec()
                .getAsSelValue();

        assertThat(result2.type(), equalTo(SelTypes.GRAPH_DATA_VECTOR));
    }

    private DataPoint point(String time, double value) {
        return new DataPoint(Instant.parse(time), value);
    }
}
