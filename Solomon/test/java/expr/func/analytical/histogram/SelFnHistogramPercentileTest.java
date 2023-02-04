package ru.yandex.solomon.expression.expr.func.analytical.histogram;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.LongStream;

import org.junit.Test;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.expression.NamedGraphData;
import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.expression.exceptions.EvaluationException;
import ru.yandex.solomon.expression.type.SelTypes;
import ru.yandex.solomon.expression.value.SelValue;
import ru.yandex.solomon.expression.value.SelValueGraphData;
import ru.yandex.solomon.expression.value.SelValueVector;
import ru.yandex.solomon.expression.version.SelVersion;
import ru.yandex.solomon.model.point.AggrPoints;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.protobuf.MetricType;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.model.type.LogHistogram;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.solomon.expression.version.SelVersion.HISTOGRAM_FUNCTIONS_DONT_MERGE_3;
import static ru.yandex.solomon.model.point.AggrPoints.dhistogram;

/**
 * @author Vladimir Gordiychuk
 */
public class SelFnHistogramPercentileTest {

    @Test
    public void emptyNoPoints() {
        GraphData result = ProgramTestSupport.expression("histogram_percentile(99.9, graphData);")
            .onMultipleLines(
                NamedGraphData.of(Labels.of("sensor", "responseTime", "bin", "100")),
                NamedGraphData.of(Labels.of("sensor", "responseTime", "bin", "200")),
                NamedGraphData.of(Labels.of("sensor", "responseTime", "bin", "300")))
            .exec(ProgramTestSupport.ExecResult::getAsSingleLine);

        assertThat(result, equalTo(GraphData.empty));
    }

    @Test
    public void emptyNoPointsWithUnknownDtype() {
        var UNKNOWN = ru.yandex.monlib.metrics.MetricType.UNKNOWN;
        NamedGraphData[] lines = new NamedGraphData[] {
                NamedGraphData.of(UNKNOWN, Labels.of("sensor", "responseTime", "host", "pre-gate-00")),
                NamedGraphData.of(UNKNOWN, Labels.of("sensor", "responseTime", "host", "pre-gate-01"))
        };

        GraphData[] result = ProgramTestSupport.expression("histogram_percentile(99.9, '', graphData);")
                .onMultipleLines(lines)
                .exec(ProgramTestSupport.ExecResult::getAsMultipleLines, HISTOGRAM_FUNCTIONS_DONT_MERGE_3::since);

        assertThat(result[0], equalTo(GraphData.empty));
        assertThat(result[1], equalTo(GraphData.empty));
    }

    @Test
    public void emptyNoMetrics() {
        GraphData result = ProgramTestSupport.expression("histogram_percentile(99.9, group_by_time(15s, 'sum', graphData));")
            .onMultipleLines(new NamedGraphData[0])
            .exec(ProgramTestSupport.ExecResult::getAsSingleLine);

        assertThat(result, equalTo(GraphData.empty));
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

        GraphData result = ProgramTestSupport.expression("histogram_percentile(80, graphData);")
            .onMultipleLines(bin100, bin200, bin300, bin999)
            .exec(ProgramTestSupport.ExecResult::getAsSingleLine);

        GraphData expected = GraphData.of(
            point(ts1, 99.2),
            point(ts2, 167.0),
            point(ts3, 278.8)
        );

        assertEquals(expected, result);
    }

    @Test
    public void useCustomBinLabel() {
        Labels labels = Labels.of("subsystem", "metabase", "sensor", "responseTime", "endpoint", "find");
        String ts1 = "2017-12-01T00:00:00Z";

        NamedGraphData bin100 = NamedGraphData.of(labels.add("bucket", "100"), point(ts1, 12d));
        NamedGraphData bin200 = NamedGraphData.of(labels.add("bucket", "200"), point(ts1, 50d));
        NamedGraphData bin300 = NamedGraphData.of(labels.add("bucket", "300"), point(ts1, Double.NaN));
        NamedGraphData binInf = NamedGraphData.of(labels.add("bucket", "inf"), point(ts1, 0d));

        GraphData result = ProgramTestSupport.expression("histogram_percentile(80, 'bucket', graphData);")
            .onMultipleLines(bin100, bin200, bin300, binInf)
            .exec(ProgramTestSupport.ExecResult::getAsSingleLine);

        GraphData expected = GraphData.of(point(ts1, 175.2));
        assertEquals(expected, result);
    }

    @Test
    public void parseBinSizeFromLabel() {
        Labels labels = Labels.of("subsystem", "metabase", "sensor", "responseTime", "endpoint", "find");
        String ts1 = "2017-12-01T00:00:00Z";

        NamedGraphData bin100 = NamedGraphData.of(labels.add("bin", "100ms"), point(ts1, 12d));
        NamedGraphData bin200 = NamedGraphData.of(labels.add("bin", "200ms"), point(ts1, 50d));
        NamedGraphData bin300 = NamedGraphData.of(labels.add("bin", "300ms"), point(ts1, Double.NaN));
        NamedGraphData binInf = NamedGraphData.of(labels.add("bin", "inf"), point(ts1, 0d));

        GraphData result = ProgramTestSupport.expression("histogram_percentile(80, graphData);")
            .onMultipleLines(bin100, bin200, bin300, binInf)
            .exec(ProgramTestSupport.ExecResult::getAsSingleLine);

        GraphData expected = GraphData.of(point(ts1, 175.2));
        assertEquals(expected, result);
    }

    @Test
    public void percentileForOneBucket() {
        Labels labels = Labels.of("subsystem", "metabase", "sensor", "responseTime", "endpoint", "find");
        String ts1 = "2017-12-01T00:00:00Z";

        NamedGraphData binInf = NamedGraphData.of(labels.add("bin", "inf"), point(ts1, 0d));

        GraphData result = ProgramTestSupport.expression("histogram_percentile(99, graphData);")
                .onMultipleLines(binInf)
                .exec(ProgramTestSupport.ExecResult::getAsSingleLine);

        GraphData expected = GraphData.of(point(ts1, 0));
        assertEquals(expected, result);
    }

    @Test
    public void useCommonTimeLine() {
        Labels labels = Labels.of("subsystem", "metabase", "sensor", "responseTime", "endpoint", "find");

        String ts1 = "2017-12-01T00:00:00Z";
        String ts2 = "2017-12-01T00:01:00Z";
        String ts3 = "2017-12-01T00:02:00Z";

        NamedGraphData bin100 =
            NamedGraphData.of(
                labels.add("bin", "100"),
                point(ts1, 10d),
                point(ts2, 8d),
                point(ts3, 1d));

        NamedGraphData bin200 =
            NamedGraphData.of(
                labels.add("bin", "200"),
                point(ts1, 2d),
                // ts2 absent by any reason, function should process it as zero count without interpolate
                point(ts3, 5d));

        NamedGraphData bin300 = NamedGraphData.of(
            labels.add("bin", "300"),
            point(ts1, 1d),
            point(ts2, 10d),
            point(ts3, 1d));

        GraphData result = ProgramTestSupport.expression("histogram_percentile(60, graphData);")
            .onMultipleLines(bin100, bin200, bin300)
            .exec(ProgramTestSupport.ExecResult::getAsSingleLine);

        GraphData expected = GraphData.of(
            point(ts1, 78.0),
            point(ts2, 228.0),
            point(ts3, 164.0)
        );

        assertEquals(expected, result);
    }

    @Test
    public void safeCommonLabels() {
        Labels commonLabels = Labels.of("subsystem", "stockpile", "sensor", "responseTime", "endpoint", "readCompressedOne");

        String ts1 = "2017-12-01T00:00:00Z";
        NamedGraphData[] source = LongStream.of(10, 20, 50, 100)
            .mapToObj(bucket -> NamedGraphData.of(
                    commonLabels.add("bucket", String.valueOf(bucket)),
                    point(ts1, ThreadLocalRandom.current().nextLong(0, 100)))
            ).toArray(NamedGraphData[]::new);

        var expr = ProgramTestSupport.expression("histogram_percentile(80, 'bucket', graphData);")
            .onMultipleLines(source);

        {
            NamedGraphData result = expr.exec(
                    ProgramTestSupport.ExecResult::getAsNamedSingleLine,
                    HISTOGRAM_FUNCTIONS_DONT_MERGE_3::before
            );

            assertThat(result.getLabels(), equalTo(commonLabels));
            assertThat(result.getAlias(), equalTo("p80.0"));
        }

        {
            NamedGraphData result = expr.exec(
                    ProgramTestSupport.ExecResult::getAsNamedSingleLine,
                    HISTOGRAM_FUNCTIONS_DONT_MERGE_3::since
            );

            assertThat(result.getLabels(), equalTo(commonLabels.add("percentile", "80.0")));
            assertThat(result.getAlias(), equalTo(""));
        }
    }

    @Test
    public void ignoreSeriesWithoutBucketLabel() {
        Labels labels = Labels.of("subsystem", "metabase", "sensor", "responseTime", "endpoint", "find");

        String ts1 = "2017-12-01T00:00:00Z";

        NamedGraphData bin100 = NamedGraphData.of(labels.add("kind", "histogram").add("le", "100"), point(ts1, 25d));
        NamedGraphData bin200 = NamedGraphData.of(labels.add("kind", "histogram").add("le", "200"), point(ts1, 4d));
        NamedGraphData max = NamedGraphData.of(labels.add("kind", "max"), point(ts1, 50d));
        NamedGraphData min = NamedGraphData.of(labels.add("kind", "min"), point(ts1, 2d));

        // selector can looks like responseTime{endpoint='find', subsystem='metabase'}
        GraphData result = ProgramTestSupport.expression("histogram_percentile(80, 'le', graphData);")
            .onMultipleLines(bin100, bin200, max, min)
            .exec(ProgramTestSupport.ExecResult::getAsSingleLine);

        assertEquals(GraphData.of(point(ts1, 92.8)), result);
    }

    @Test
    public void ignoreSeriesWithNotValidBucketValue() {
        Labels labels = Labels.of("subsystem", "metabase", "sensor", "responseTime", "endpoint", "find");

        String ts1 = "2017-12-01T00:00:00Z";

        NamedGraphData bin100 = NamedGraphData.of(labels.add("bin", "100"), point(ts1, 10d));
        NamedGraphData bin200 = NamedGraphData.of(labels.add("bin", "200"), point(ts1, 8d));
        NamedGraphData max = NamedGraphData.of(labels.add("bin", "max"), point(ts1, 50d));
        NamedGraphData min = NamedGraphData.of(labels.add("bin", "min"), point(ts1, 2d));

        // selector can looks like responseTime{endpoint='find', subsystem='metabase'}
        GraphData result = ProgramTestSupport.expression("histogram_percentile(90, graphData);")
            .onMultipleLines(bin100, bin200, max, min)
            .exec(ProgramTestSupport.ExecResult::getAsSingleLine);

        assertEquals(GraphData.of(point(ts1, 177.5)), result);
    }

    @Test
    public void infBucket() {
        Labels labels = Labels.of("subsystem", "metabase", "sensor", "responseTime", "endpoint", "find");
        String ts1 = "2017-12-01T00:00:00Z";

        NamedGraphData bin100 = NamedGraphData.of(labels.add("bucket", "100"), point(ts1, 200d));
        NamedGraphData bin200 = NamedGraphData.of(labels.add("bucket", "200"), point(ts1, 50d));
        NamedGraphData bin300 = NamedGraphData.of(labels.add("bucket", "300"), point(ts1, Double.NaN));
        NamedGraphData binInf = NamedGraphData.of(labels.add("bucket", "inf"), point(ts1, 250d));

        var expr = ProgramTestSupport.expression("histogram_percentile(100 - as_vector(25, 12.5, 6.25), 'bucket', graphData);")
            .onMultipleLines(bin100, bin200, bin300, binInf);

        {
            GraphData[] results = expr.exec(HISTOGRAM_FUNCTIONS_DONT_MERGE_3::before)
                    .getAsMultipleLines();

            for (int i = 0; i < 3; i++) {
                GraphData result = results[i];
                assertThat(result.getTimestamps().length(), equalTo(1));
                assertThat(result.getTimestamps().at(0), equalTo(Instant.parse(ts1).toEpochMilli()));
                assertThat(result.getValues().at(0), equalTo(300d));
            }
        }
        {
            GraphData[] results = expr.exec(HISTOGRAM_FUNCTIONS_DONT_MERGE_3::since)
                    .getAsMultipleLines();

            for (int i = 0; i < 3; i++) {
                GraphData result = results[i];
                assertThat(result.getTimestamps().length(), equalTo(1));
                assertThat(result.getTimestamps().at(0), equalTo(Instant.parse(ts1).toEpochMilli()));
                assertThat(result.getValues().at(0), equalTo(300d));
            }
        }
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

        GraphData result = ProgramTestSupport.expression("histogram_percentile(80, graphData);")
            .onMultipleLines(source)
            .exec(ProgramTestSupport.ExecResult::getAsSingleLine);

        GraphData expected = GraphData.of(point(ts, 276));
        assertThat(result, equalTo(expected));
    }

    @Test
    public void differentReturnTypesInVersions() {
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

        SelValue resultBasic = ProgramTestSupport.expression("histogram_percentile(80, graphData);")
                .onMultipleLines(source)
                .exec(SelVersion.BASIC_1)
                .getAsSelValue();

        assertThat(resultBasic.type(), equalTo(SelTypes.GRAPH_DATA));

        SelValue resultVectored = ProgramTestSupport.expression("histogram_percentile(80, graphData);")
                .onMultipleLines(source)
                .exec(SelVersion.GROUP_LINES_RETURN_VECTOR_2)
                .getAsSelValue();

        assertThat(resultVectored.type(), equalTo(SelTypes.GRAPH_DATA_VECTOR));
    }

    @Test
    public void severalPercentiles() {
        String ts = "2019-05-01T00:00:00Z";

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

        var expr = ProgramTestSupport.expression("histogram_percentile(as_vector(80, 90), graphData);")
            .onMultipleLines(source);
        {
            NamedGraphData[] result = expr.exec(HISTOGRAM_FUNCTIONS_DONT_MERGE_3::before)
                    .getAsNamedMultipleLines();

            NamedGraphData[] expected = new NamedGraphData[]{
                    NamedGraphData.of(GraphData.of(point(ts, 276)), "", Labels.of(), "p80.0"),
                    NamedGraphData.of(GraphData.of(point(ts, 288)), "", Labels.of(), "p90.0")
            };

            assertThat(result, equalTo(expected));
        }
        {
            NamedGraphData[] result = expr.exec(HISTOGRAM_FUNCTIONS_DONT_MERGE_3::since)
                    .getAsNamedMultipleLines();

            NamedGraphData[] expected = new NamedGraphData[]{
                    NamedGraphData.of(GraphData.of(point(ts, 276)), "", Labels.of("percentile", "80.0"), ""),
                    NamedGraphData.of(GraphData.of(point(ts, 288)), "", Labels.of("percentile", "90.0"), "")
            };

            assertThat(result, equalTo(expected));
        }
    }

    @Test
    public void singleLineIsOkIfHist() {
        String ts = "2019-05-01T00:00:00Z";

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

        var expr = ProgramTestSupport.expression("histogram_percentile(as_vector(80, 90), graphData);")
                .onSingleLine(source);
        {
            NamedGraphData[] result = expr.exec(HISTOGRAM_FUNCTIONS_DONT_MERGE_3::before)
                .getAsNamedMultipleLines();

            NamedGraphData[] expected = new NamedGraphData[]{
                    NamedGraphData.of(GraphData.of(point(ts, 276)), "", Labels.of(), "p80.0"),
                    NamedGraphData.of(GraphData.of(point(ts, 288)), "", Labels.of(), "p90.0")
            };

            assertThat(result, equalTo(expected));
        }

        {
            NamedGraphData[] result = expr.exec(HISTOGRAM_FUNCTIONS_DONT_MERGE_3::since)
                    .getAsNamedMultipleLines();

            NamedGraphData[] expected = new NamedGraphData[]{
                    NamedGraphData.of(GraphData.of(point(ts, 276)), "", Labels.of("percentile", "80.0"), ""),
                    NamedGraphData.of(GraphData.of(point(ts, 288)), "", Labels.of("percentile", "90.0"), "")
            };

            assertThat(result, equalTo(expected));
        }
    }

    @Test
    public void groupedHistIsOk() {
        String ts = "2019-05-01T00:00:00Z";

        NamedGraphData first = new NamedGraphData(
                "",
                ru.yandex.monlib.metrics.MetricType.HIST,
                "histogram1",
                Labels.of(),
                MetricType.HIST,
                AggrGraphDataArrayList.of(
                        AggrPoints.point(ts, dhistogram(new double[] {256, 512, 1024}, new long[] {0, 50, 250}))
                )
        );

        NamedGraphData second = new NamedGraphData(
                "",
                ru.yandex.monlib.metrics.MetricType.LOG_HISTOGRAM,
                "histogram1",
                Labels.of(),
                MetricType.LOG_HISTOGRAM,
                AggrGraphDataArrayList.of(
                        AggrPoints.point(ts, LogHistogram.newBuilder()
                                .setBase(2)
                                .setStartPower(7)
                                .setCountZero(100)
                                .addBucket(100)
                                .addBucket(200)
                                .addBucket(300)
                                .build())
                )
        );

        var expr = ProgramTestSupport
                .expression("histogram_percentile(as_vector(10, 89), group_lines('sum', graphData));")
                .onMultipleLines(first, second);

        {
            NamedGraphData[] result = expr.exec(HISTOGRAM_FUNCTIONS_DONT_MERGE_3::before)
                    .getAsNamedMultipleLines();

            NamedGraphData[] expected = new NamedGraphData[]{
                    NamedGraphData.of(GraphData.of(point(ts, 0.01)), "", Labels.of(), "p10.0"),
                    NamedGraphData.of(GraphData.of(point(ts, 921.6)), "", Labels.of(), "p89.0")
            };

            assertThat(result, equalTo(expected));
        }

        {
            NamedGraphData[] result = expr.exec(HISTOGRAM_FUNCTIONS_DONT_MERGE_3::since)
                    .getAsNamedMultipleLines();

            NamedGraphData[] expected = new NamedGraphData[]{
                    NamedGraphData.of(GraphData.of(point(ts, 0.01)), "", Labels.of("percentile", "10.0"), ""),
                    NamedGraphData.of(GraphData.of(point(ts, 921.6)), "", Labels.of("percentile", "89.0"), "")
            };

            assertThat(result, equalTo(expected));
        }
    }

    @Test
    public void singleLineIsOkIfEmptyHist() {
        NamedGraphData source = new NamedGraphData(
                "",
                ru.yandex.monlib.metrics.MetricType.HIST_RATE,
                "histogram",
                Labels.of(),
                AggrGraphDataArrayList.empty()
        );

        var expr = ProgramTestSupport.expression("histogram_percentile(as_vector(80, 90), graphData);")
                .onSingleLine(source);

        {
            NamedGraphData[] result = expr.exec(HISTOGRAM_FUNCTIONS_DONT_MERGE_3::before)
                    .getAsNamedMultipleLines();

            NamedGraphData[] expected = new NamedGraphData[]{
                    NamedGraphData.of(GraphData.empty, "", Labels.of(), "p80.0"),
                    NamedGraphData.of(GraphData.empty, "", Labels.of(), "p90.0")
            };

            assertThat(result, equalTo(expected));
        }

        {
            NamedGraphData[] result = expr.exec(HISTOGRAM_FUNCTIONS_DONT_MERGE_3::since)
                    .getAsNamedMultipleLines();

            NamedGraphData[] expected = new NamedGraphData[]{
                    NamedGraphData.of(GraphData.empty, "", Labels.of("percentile", "80.0"), ""),
                    NamedGraphData.of(GraphData.empty, "", Labels.of("percentile", "90.0"), "")
            };

            assertThat(result, equalTo(expected));
        }
    }

    @Test(expected = EvaluationException.class)
    public void singleLineWithGaugeIsNotOk() {
        long ts = Instant.parse("2019-05-01T00:00:00Z").toEpochMilli();

        NamedGraphData source = new NamedGraphData(
                "",
                ru.yandex.monlib.metrics.MetricType.RATE,
                "histogram",
                Labels.of(),
                MetricType.RATE,
                AggrGraphDataArrayList.of(AggrPoints.lpoint(ts, 42))
        );

        ProgramTestSupport.expression("histogram_percentile(as_vector(80, 90), graphData);")
                .onSingleLine(source)
                .exec();
    }

    @Test
    public void severalPercentilesEmpty() {
        var expr = ProgramTestSupport.expression("histogram_percentile(as_vector(80, 90), 'bin', graphData);")
                .onMultipleLines(new NamedGraphData[0]);

        {
            SelValue result = expr.exec(HISTOGRAM_FUNCTIONS_DONT_MERGE_3::before)
                .getAsSelValue();

            SelValue expected = new SelValueVector(SelTypes.GRAPH_DATA, new SelValueGraphData[]{
                    new SelValueGraphData(NamedGraphData.of(GraphData.empty, "", Labels.of(), "p80.0")),
                    new SelValueGraphData(NamedGraphData.of(GraphData.empty, "", Labels.of(), "p90.0"))
            });

            assertThat(result, equalTo(expected));
        }

        {
            SelValue result = expr.exec(HISTOGRAM_FUNCTIONS_DONT_MERGE_3::since)
                    .getAsSelValue();

            SelValue expected = new SelValueVector(SelTypes.GRAPH_DATA, new SelValueGraphData[0]);

            assertThat(result, equalTo(expected));
        }
    }

    @Test
    public void magicBucketLabelKeyCalculation() {
        Labels labels = Labels.of("subsystem", "metabase", "sensor", "responseTime", "endpoint", "find");

        String ts1 = "2019-12-16T00:00:00Z";
        String ts2 = "2019-12-16T00:01:00Z";
        String ts3 = "2019-12-16T00:02:00Z";

        NamedGraphData bin100 = NamedGraphData.of(labels.add("bucket", "100"), point(ts1, 40d), point(ts2, 30d), point(ts3, 20d));
        NamedGraphData bin200 = NamedGraphData.of(labels.add("bucket", "200"), point(ts1, 5d), point(ts2, 10d), point(ts3, 17d));
        NamedGraphData bin300 = NamedGraphData.of(labels.add("bucket", "300"), point(ts1, 5d), point(ts2, 10d), point(ts3, 3d));
        NamedGraphData bin999 = NamedGraphData.of(labels.add("bucket", "999"), point(ts1, 0d), point(ts2, 0d), point(ts3, 10d));

        GraphData result = ProgramTestSupport.expression("histogram_percentile(80, graphData);")
            .onMultipleLines(bin100, bin200, bin300, bin999)
            .exec(ProgramTestSupport.ExecResult::getAsSingleLine);

        GraphData expected = GraphData.of(
            point(ts1, 100.0),
            point(ts2, 200.0),
            point(ts3, 300.0)
        );

        assertEquals(expected, result);
    }

    @Test
    public void severalHistogramsMixed() {
        Labels labels = Labels.of("subsystem", "metabase");

        String ts1 = "2019-12-16T00:00:00Z";

        Labels first = labels.add("sensor", "responseTime");
        Labels second = labels.add("sensor", "gcTime");

        NamedGraphData first100 = NamedGraphData.of(first.add("bin", "100"), point(ts1, 10d));
        NamedGraphData first200 = NamedGraphData.of(first.add("bin", "200"), point(ts1, 20d));
        NamedGraphData first300 = NamedGraphData.of(first.add("bin", "300"), point(ts1, 20d));

        NamedGraphData second100 = NamedGraphData.of(second.add("bin", "100"), point(ts1, 10d));
        NamedGraphData second200 = NamedGraphData.of(second.add("bin", "200"), point(ts1, 30d));
        NamedGraphData second300 = NamedGraphData.of(second.add("bin", "300"), point(ts1, 10d));


        var expr = ProgramTestSupport.expression("histogram_percentile(76, graphData);")
                .onMultipleLines(first100, first200, second300, second100, second200, first300);
        {
            GraphData result = expr.exec(ProgramTestSupport.ExecResult::getAsSingleLine, HISTOGRAM_FUNCTIONS_DONT_MERGE_3::before);

            GraphData expected = GraphData.of(point(ts1, 220.0));

            assertEquals(expected, result);
        }

        {
            NamedGraphData[] result = expr.exec(ProgramTestSupport.ExecResult::getAsNamedMultipleLines,
                    HISTOGRAM_FUNCTIONS_DONT_MERGE_3::since);

            Labels commonLabels = Labels.of("percentile", "76.0", "subsystem", "metabase");
            assertEquals(commonLabels.add("sensor", "responseTime"), result[0].getLabels());
            assertEquals(commonLabels.add("sensor", "gcTime"), result[1].getLabels());

            GraphData expected = GraphData.of(point(ts1, 240.0));
            assertEquals(expected, result[0].getGraphData(MetricType.DGAUGE));
        }
    }

    @Test
    public void exponentialBuckets() {
        Labels labels = Labels.of("subsystem", "metabase", "sensor", "responseTime", "endpoint", "find");

        String ts = "2019-12-16T00:00:00Z";

        NamedGraphData bin250us = NamedGraphData.of(labels.add("bin", "2.5E-4"), point(ts, 40d));
        NamedGraphData bin500us = NamedGraphData.of(labels.add("bin", "5.0E-4"), point(ts, 5d));
        NamedGraphData bin1000us = NamedGraphData.of(labels.add("bin", "1E-3"), point(ts, 5d));
        // Ignored
        NamedGraphData bin10000us = NamedGraphData.of(labels.add("bucket", "1E-2"), point(ts, 5d));

        GraphData result = ProgramTestSupport.expression("histogram_percentile(80, graphData);")
                .onMultipleLines(bin250us, bin500us, bin1000us, bin10000us)
                .exec(ProgramTestSupport.ExecResult::getAsSingleLine);

        GraphData expected = GraphData.of(point(ts, 2.5e-4));

        assertEquals(expected, result);
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

        var expr = ProgramTestSupport
                .expression("histogram_percentile(as_vector(0, 10, 20, 50, 80, 90, 100), graphData);")
                .onMultipleLines(logHist);

        Labels commonLabels = Labels.of("signal", "resp-time_hgram");
        GraphData p0 = GraphData.of(
                DataPoint.point(1000, 0),
                DataPoint.point(1500, 0),
                DataPoint.point(2000, 0),
                DataPoint.point(2500, Math.pow(1.5, 3)));
        GraphData p10 = GraphData.of(
                DataPoint.point(1000, 0),
                DataPoint.point(1500, 0),
                DataPoint.point(2000, 0),
                DataPoint.point(2500, Math.pow(1.5, 3.6)));
        GraphData p20 = GraphData.of(
                DataPoint.point(1000, 0),
                DataPoint.point(1500, 0),
                DataPoint.point(2000, Math.pow(2, 3)),
                DataPoint.point(2500, Math.pow(1.5, 4.1) + 9e-16));
        GraphData p50 = GraphData.of(
                DataPoint.point(1000, 0),
                DataPoint.point(1500, 0),
                DataPoint.point(2000, 16),
                DataPoint.point(2500, Math.pow(1.5, 5)));
        GraphData p80 = GraphData.of(
                DataPoint.point(1000, 0),
                DataPoint.point(1500, 0),
                DataPoint.point(2000, Math.pow(2, 4.75)),
                DataPoint.point(2500, Math.pow(1.5, 5.6)));
        GraphData p90 = GraphData.of(
                DataPoint.point(1000, 0),
                DataPoint.point(1500, 0),
                DataPoint.point(2000, 32),
                DataPoint.point(2500, Math.pow(1.5, 5.8) + 2e-15));
        GraphData p100 = GraphData.of(
                DataPoint.point(1000, 0),
                DataPoint.point(1500, 0),
                DataPoint.point(2000, 128),
                DataPoint.point(2500, Math.pow(1.5, 6)));

        Supplier<NamedGraphData.Builder> newNgd = () -> NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setLabels(commonLabels);
        {
            NamedGraphData[] result = expr.exec(HISTOGRAM_FUNCTIONS_DONT_MERGE_3::before)
                    .getAsNamedMultipleLines();

            assertThat(result[0], equalTo(newNgd.get().setAlias("p0.0").setGraphData(p0).build()));
            assertThat(result[1], equalTo(newNgd.get().setAlias("p10.0").setGraphData(p10).build()));
            assertThat(result[2], equalTo(newNgd.get().setAlias("p20.0").setGraphData(p20).build()));
            assertThat(result[3], equalTo(newNgd.get().setAlias("p50.0").setGraphData(p50).build()));
            assertThat(result[4], equalTo(newNgd.get().setAlias("p80.0").setGraphData(p80).build()));
            assertThat(result[5], equalTo(newNgd.get().setAlias("p90.0").setGraphData(p90).build()));
            assertThat(result[6], equalTo(newNgd.get().setAlias("p100.0").setGraphData(p100).build()));
        }
        {
            NamedGraphData[] result = expr.exec(HISTOGRAM_FUNCTIONS_DONT_MERGE_3::since)
                    .getAsNamedMultipleLines();

            assertThat(result[0], equalTo(newNgd.get().setLabels(commonLabels.add("percentile", "0.0")).setGraphData(p0).build()));
            assertThat(result[1], equalTo(newNgd.get().setLabels(commonLabels.add("percentile", "10.0")).setGraphData(p10).build()));
            assertThat(result[2], equalTo(newNgd.get().setLabels(commonLabels.add("percentile", "20.0")).setGraphData(p20).build()));
            assertThat(result[3], equalTo(newNgd.get().setLabels(commonLabels.add("percentile", "50.0")).setGraphData(p50).build()));
            assertThat(result[4], equalTo(newNgd.get().setLabels(commonLabels.add("percentile", "80.0")).setGraphData(p80).build()));
            assertThat(result[5], equalTo(newNgd.get().setLabels(commonLabels.add("percentile", "90.0")).setGraphData(p90).build()));
            assertThat(result[6], equalTo(newNgd.get().setLabels(commonLabels.add("percentile", "100.0")).setGraphData(p100).build()));
        }
    }

    private DataPoint point(String time, double value) {
        return new DataPoint(Instant.parse(time), value);
    }
}
