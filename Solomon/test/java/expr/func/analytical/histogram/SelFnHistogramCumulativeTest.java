package ru.yandex.solomon.expression.expr.func.analytical.histogram;

import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.monlib.metrics.histogram.Histograms;
import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.expression.NamedGraphData;
import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.expression.exceptions.EvaluationException;
import ru.yandex.solomon.expression.test.ForEachSelVersionRunner;
import ru.yandex.solomon.expression.test.VersionedSelTestBase;
import ru.yandex.solomon.expression.version.SelVersion;
import ru.yandex.solomon.model.point.AggrPoints;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.protobuf.MetricType;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.model.type.Histogram;
import ru.yandex.solomon.model.type.LogHistogram;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Ivan Tsybulin
 */
@RunWith(ForEachSelVersionRunner.class)
public class SelFnHistogramCumulativeTest extends VersionedSelTestBase {
    public SelFnHistogramCumulativeTest(SelVersion version) {
        super(version);
    }

    @Test
    public void emptyInput() {
        List<String> variants = List.of(
                "histogram_count(150, graphData);",
                "histogram_count(150, inf(), graphData);",
                "histogram_count(-inf(), inf(), graphData);",
                "histogram_count([100], [200], graphData);",

                "histogram_cdfp(150, graphData);",
                "histogram_cdfp(150, inf(), graphData);",
                "histogram_cdfp(-inf(), inf(), graphData);",
                "histogram_cdfp([100], [200], graphData);"
        );
        for (var variant : variants) {
            GraphData[] result = ProgramTestSupport.expression(variant)
                    .onMultipleLines(new NamedGraphData[0])
                    .exec(ProgramTestSupport.ExecResult::getAsMultipleLines);

            assertThat(result.length, equalTo(0));
        }
    }

    @Test
    public void emptyNoPoints() {
        List<String> variants = List.of(
                "histogram_count(150, graphData);",
                "histogram_count(150, inf(), graphData);",
                "histogram_count(-inf(), inf(), graphData);",
                "histogram_count([100], [200], graphData);",

                "histogram_cdfp(150, graphData);",
                "histogram_cdfp(150, inf(), graphData);",
                "histogram_cdfp(-inf(), inf(), graphData);",
                "histogram_cdfp([100], [200], graphData);"
        );
        for (var variant : variants) {
            GraphData result = ProgramTestSupport.expression(variant)
                    .onMultipleLines(
                            NamedGraphData.of(Labels.of("sensor", "responseTime", "bin", "100")),
                            NamedGraphData.of(Labels.of("sensor", "responseTime", "bin", "200")),
                            NamedGraphData.of(Labels.of("sensor", "responseTime", "bin", "300")))
                    .exec(ProgramTestSupport.ExecResult::getAsSingleLine);

            assertThat(result, equalTo(GraphData.empty));
        }
    }

    @Test
    public void emptyNoPointsManyLines() {
        List<String> variants = List.of(
                "histogram_count([100, 150], graphData);",
                "histogram_count([150, 200], inf(), graphData);",
                "histogram_count(-inf(), [100, 150], graphData);",
                "histogram_count([100, 200], [200, 250], graphData);",

                "histogram_cdfp([100, 150], graphData);",
                "histogram_cdfp([150, 200], inf(), graphData);",
                "histogram_cdfp(-inf(), [100, 150], graphData);",
                "histogram_cdfp([100, 200], [200, 250], graphData);"
        );
        for (var variant : variants) {
            GraphData[] results = ProgramTestSupport.expression(variant)
                    .onMultipleLines(
                            NamedGraphData.of(Labels.of("sensor", "responseTime", "bin", "100")),
                            NamedGraphData.of(Labels.of("sensor", "responseTime", "bin", "200")),
                            NamedGraphData.of(Labels.of("sensor", "responseTime", "bin", "300")))
                    .exec(ProgramTestSupport.ExecResult::getAsMultipleLines);

            assertThat(results, equalTo(new GraphData[] {GraphData.empty, GraphData.empty}));
        }
    }

    @Test
    public void legalParams() {
        List<String> variants = List.of(
                "-inf()",
                "inf()",
                "0, 0",
                "-inf(), 0",
                "-inf(), inf()",
                "0, inf()",
                "inf(), inf()",
                "[-inf(), 0, inf()], inf()",
                "-inf(), [-inf(), 42, inf()]",
                "[-inf(), 0, inf()], [-inf(), 42, inf()]"
        );
        for (var variant : variants) {
            ProgramTestSupport
                    .expression("histogram_cdfp(" + variant + ", graphData);")
                    .onMultipleLines(
                            NamedGraphData.of(Labels.of("sensor", "responseTime", "bin", "100")),
                            NamedGraphData.of(Labels.of("sensor", "responseTime", "bin", "200")),
                            NamedGraphData.of(Labels.of("sensor", "responseTime", "bin", "300")))
                    .exec(version);
        }
    }

    @Test
    public void illegalParams() {
        List<String> variants = List.of(
                "1, 0",
                "0, -inf()",
                "inf(), 0",
                "0, [-inf(), 0, inf()]",
                "[-inf(), 0, inf()], 42",
                "[1, 2, 3], [4, 5]"
        );
        for (var variant : variants) {
            boolean failed = false;
            try {
                ProgramTestSupport
                        .expression("histogram_cdfp(" + variant + ", graphData);")
                        .onMultipleLines(
                                NamedGraphData.of(Labels.of("sensor", "responseTime", "bin", "100")),
                                NamedGraphData.of(Labels.of("sensor", "responseTime", "bin", "200")),
                                NamedGraphData.of(Labels.of("sensor", "responseTime", "bin", "300")))
                        .exec(version);
            } catch (EvaluationException e) {
                failed = true;
            }
            Assert.assertTrue("Must have failed with args: " + variant, failed);
        }
    }

    private double randomBoundary(Random random, double scale) {
        double[] magics = new double[] {Double.NEGATIVE_INFINITY, 0, Double.POSITIVE_INFINITY};
        if (random.nextBoolean()) {
            return magics[random.nextInt(magics.length)];
        }
        return random.nextGaussian() * scale;
    }

    private String format(double val) {
        if (Double.isInfinite(val)) {
            return val > 0 ? "inf()" : "-inf()";
        }
        return Double.toString(val);
    }

    @Test
    public void fuzzyLegacy() {
        Random random = new Random(42);
        final int RETRIES = 100;

        Labels labels = Labels.of("subsystem", "metabase", "sensor", "responseTime", "endpoint", "find");
        String ts1 = "2017-12-01T00:00:00Z";

        NamedGraphData bin100 = NamedGraphData.of(labels.add("bucket", "100"), DataPoint.point(ts1, 200d));
        NamedGraphData bin200 = NamedGraphData.of(labels.add("bucket", "200"), DataPoint.point(ts1, 50d));
        NamedGraphData bin300 = NamedGraphData.of(labels.add("bucket", "300"), DataPoint.point(ts1, Double.NaN));
        NamedGraphData binInf = NamedGraphData.of(labels.add("bucket", "inf"), DataPoint.point(ts1, 250d));

        for (int i = 0; i < RETRIES; i++) {
            double a = randomBoundary(random, 300);
            double b = (random.nextDouble() < 0.1) ? a : randomBoundary(random, 300);
            String lower = format(Math.min(a, b));
            String upper = format(Math.max(a, b));

            double result1 = ProgramTestSupport
                    .expression("last(histogram_cdfp(" + lower + ", " + upper + ", 'bucket', graphData) / 100);")
                    .onMultipleLines(bin100, bin200, bin300, binInf)
                    .exec(version)
                    .getAsSelValue()
                    .castToScalar()
                    .getValue();
            double result2 = ProgramTestSupport
                    .expression("last(histogram_count(" + lower + ", " + upper + ", 'bucket', graphData) / 500);")
                    .onMultipleLines(bin100, bin200, bin300, binInf)
                    .exec(version)
                    .getAsSelValue()
                    .castToScalar()
                    .getValue();

            assertEquals("For args: " + lower + ", " + upper, result1, result2,
                    1e-14 * (Math.abs(result1) + Math.abs(result2)));
        }
    }

    @Test
    public void fuzzyHist() {
        Random random = new Random(42);
        final int RETRIES = 100;

        NamedGraphData hist = NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.HIST)
                .setGraphData(MetricType.HIST_RATE, AggrGraphDataArrayList.of(
                        AggrPoints.point(2500, Histogram.newInstance(
                                new double[] {100, 200, 300, 500, 1000, 2000, Histograms.INF_BOUND},
                                new long[]   { 10,  20,  10,  40,   15,    3,                    2}
                        ))))
                .build();

        for (int i = 0; i < RETRIES; i++) {
            double a = randomBoundary(random, 2000);
            double b = (random.nextDouble() < 0.1) ? a : randomBoundary(random, 2000);
            String lower = format(Math.min(a, b));
            String upper = format(Math.max(a, b));

            double result1 = ProgramTestSupport
                    .expression("last(histogram_cdfp(" + lower + ", " + upper + ", graphData) / 100);")
                    .onMultipleLines(hist)
                    .exec(version)
                    .getAsSelValue()
                    .castToScalar()
                    .getValue();
            double result2 = ProgramTestSupport
                    .expression("last(histogram_count(" + lower + ", " + upper + ", graphData) / 100);")
                    .onMultipleLines(hist)
                    .exec(version)
                    .getAsSelValue()
                    .castToScalar()
                    .getValue();

            assertEquals("For args: " + lower + ", " + upper, result1, result2,
                    1e-14 * (Math.abs(result1) + Math.abs(result2)));
        }
    }

    @Test
    public void fuzzyLogHist() {
        Random random = new Random(42);
        final int RETRIES = 100;

        NamedGraphData logHist = NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.LOG_HISTOGRAM)
                .setGraphData(MetricType.LOG_HISTOGRAM, AggrGraphDataArrayList.of(
                        AggrPoints.point(2000, LogHistogram.newBuilder()
                                .setBase(1.5)
                                .setStartPower(-6)
                                .setCountZero(10)
                                .setBuckets(new double[] {0, 22, 63, 7072, 28732, 1320, 38, 34, 33, 1, 1})
                                .build())))
                .build();

        for (int i = 0; i < RETRIES; i++) {
            double a = randomBoundary(random, 10);
            double b = (random.nextDouble() < 0.1) ? a : randomBoundary(random, 10);
            String lower = format(Math.min(a, b));
            String upper = format(Math.max(a, b));

            double result1 = ProgramTestSupport
                    .expression("last(histogram_cdfp(" + lower + ", " + upper + ", graphData) / 100);")
                    .onMultipleLines(logHist)
                    .exec(version)
                    .getAsSelValue()
                    .castToScalar()
                    .getValue();
            double result2 = ProgramTestSupport
                    .expression("last(histogram_count(" + lower + ", " + upper + ", graphData) / 37326);")
                    .onMultipleLines(logHist)
                    .exec(version)
                    .getAsSelValue()
                    .castToScalar()
                    .getValue();

            assertEquals("For args: " + lower + ", " + upper, result1, result2,
                    1e-14 * (Math.abs(result1) + Math.abs(result2)));
        }
    }

    @Test
    public void legacyHistogram() {
        Assume.assumeTrue(SelVersion.HISTOGRAM_FUNCTIONS_DONT_MERGE_3.before(version));
        Labels labels = Labels.of("subsystem", "metabase", "sensor", "responseTime", "endpoint", "find");
        String ts1 = "2017-12-01T00:00:00Z";

        NamedGraphData bin100 = NamedGraphData.of(labels.add("bucket", "100"), DataPoint.point(ts1, 200d));
        NamedGraphData bin200 = NamedGraphData.of(labels.add("bucket", "200"), DataPoint.point(ts1, 50d));
        NamedGraphData bin300 = NamedGraphData.of(labels.add("bucket", "300"), DataPoint.point(ts1, Double.NaN));
        NamedGraphData binInf = NamedGraphData.of(labels.add("bucket", "inf"), DataPoint.point(ts1, 250d));

        NamedGraphData[] resultCdfp = ProgramTestSupport
                .expression("histogram_cdfp([50, 500, inf()], 'bucket', graphData);")
                .onMultipleLines(bin100, bin200, bin300, binInf)
                .exec(version)
                .getAsNamedMultipleLines();
        NamedGraphData[] resultCount = ProgramTestSupport
                .expression("histogram_count([50, 500, inf()], 'bucket', graphData);")
                .onMultipleLines(bin100, bin200, bin300, binInf)
                .exec(version)
                .getAsNamedMultipleLines();

        assertThat(resultCdfp[0], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setAlias("from -Infinity to 50.0")
                .setLabels(labels)
                .setGraphData(GraphData.of(DataPoint.point(ts1, 20d)))
                .build()));
        assertThat(resultCount[0], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setAlias("from -Infinity to 50.0")
                .setLabels(labels)
                .setGraphData(GraphData.of(DataPoint.point(ts1, 100d)))
                .build()));

        assertThat(resultCdfp[1], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setAlias("from -Infinity to 500.0")
                .setLabels(labels)
                .setGraphData(GraphData.of(DataPoint.point(ts1, 50d)))
                .build()));
        assertThat(resultCount[1], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setAlias("from -Infinity to 500.0")
                .setLabels(labels)
                .setGraphData(GraphData.of(DataPoint.point(ts1, 250d)))
                .build()));

        assertThat(resultCdfp[2], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setAlias("from -Infinity to Infinity")
                .setLabels(labels)
                .setGraphData(GraphData.of(DataPoint.point(ts1, 100d)))
                .build()));
        assertThat(resultCount[2], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setAlias("from -Infinity to Infinity")
                .setLabels(labels)
                .setGraphData(GraphData.of(DataPoint.point(ts1, 500d)))
                .build()));

        NamedGraphData total = ProgramTestSupport
                .expression("histogram_count('bucket', graphData);")
                .onMultipleLines(bin100, bin200, bin300, binInf)
                .exec(version)
                .getAsNamedSingleLine();
        assertThat(total, equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setAlias("from -Infinity to Infinity")
                .setLabels(labels)
                .setGraphData(GraphData.of(DataPoint.point(ts1, 500d)))
                .build()));
    }

    @Test
    public void legacyHistogramNoMerge() {
        Assume.assumeTrue(SelVersion.HISTOGRAM_FUNCTIONS_DONT_MERGE_3.since(version));
        Labels labels = Labels.of("subsystem", "metabase", "sensor", "responseTime", "endpoint", "find");
        String ts1 = "2017-12-01T00:00:00Z";

        NamedGraphData bin100 = NamedGraphData.of(labels.add("bucket", "100"), DataPoint.point(ts1, 200d));
        NamedGraphData bin200 = NamedGraphData.of(labels.add("bucket", "200"), DataPoint.point(ts1, 50d));
        NamedGraphData bin300 = NamedGraphData.of(labels.add("bucket", "300"), DataPoint.point(ts1, Double.NaN));
        NamedGraphData binInf = NamedGraphData.of(labels.add("bucket", "inf"), DataPoint.point(ts1, 250d));

        NamedGraphData[] resultCdfp = ProgramTestSupport
                .expression("histogram_cdfp([50, 500, inf()], 'bucket', graphData);")
                .onMultipleLines(bin100, bin200, bin300, binInf)
                .exec(version)
                .getAsNamedMultipleLines();
        NamedGraphData[] resultCount = ProgramTestSupport
                .expression("histogram_count([50, 500, inf()], 'bucket', graphData);")
                .onMultipleLines(bin100, bin200, bin300, binInf)
                .exec(version)
                .getAsNamedMultipleLines();


        assertThat(resultCdfp[0], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setLabels(labels.add("upper_bound", "50.0"))
                .setGraphData(GraphData.of(DataPoint.point(ts1, 20d)))
                .build()));
        assertThat(resultCount[0], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setLabels(labels.add("upper_bound", "50.0"))
                .setGraphData(GraphData.of(DataPoint.point(ts1, 100d)))
                .build()));

        assertThat(resultCdfp[1], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setLabels(labels.add("upper_bound", "500.0"))
                .setGraphData(GraphData.of(DataPoint.point(ts1, 50d)))
                .build()));
        assertThat(resultCount[1], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setLabels(labels.add("upper_bound", "500.0"))
                .setGraphData(GraphData.of(DataPoint.point(ts1, 250d)))
                .build()));

        assertThat(resultCdfp[2], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setLabels(labels)
                .setGraphData(GraphData.of(DataPoint.point(ts1, 100d)))
                .build()));
        assertThat(resultCount[2], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setLabels(labels)
                .setGraphData(GraphData.of(DataPoint.point(ts1, 500d)))
                .build()));

        NamedGraphData total = ProgramTestSupport
                .expression("histogram_count('bucket', graphData);")
                .onMultipleLines(bin100, bin200, bin300, binInf)
                .exec(version)
                .getAsNamedSingleLine();
        assertThat(total, equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setLabels(labels)
                .setGraphData(GraphData.of(DataPoint.point(ts1, 500d)))
                .build()));
    }

    @Test
    public void nativeHistogram() {
        Assume.assumeTrue(SelVersion.HISTOGRAM_FUNCTIONS_DONT_MERGE_3.before(version));
        NamedGraphData hist = NamedGraphData.newBuilder()
                .setLabels(Labels.of("sensor", "responseTimeMs"))
                .setType(ru.yandex.monlib.metrics.MetricType.HIST)
                .setGraphData(MetricType.HIST_RATE, AggrGraphDataArrayList.of(
                        AggrPoints.point(1000, (Histogram) null),
                        AggrPoints.point(1500, Histogram.newInstance(
                                new double[0], new long[0]
                        )),
                        AggrPoints.point(2000, Histogram.newInstance(
                                new double[] {100, 200, 300, 500, 1000, 2000},
                                new long[]   { 10,  20,  10,  40,   15,    5}
                        )),
                        AggrPoints.point(2500, Histogram.newInstance(
                                new double[] {100, 200, 300, 500, 1000, 2000, Histograms.INF_BOUND},
                                new long[]   { 10,  20,  10,  40,   15,    3,                    2}
                        ))))
                .build();

        NamedGraphData[] result = ProgramTestSupport
                .expression("histogram_cdfp([-inf(), 0, 300, -inf(), 400, 400], [50, 500, 400, 10, 2000, inf()], graphData);")
                .onMultipleLines(hist)
                .exec(version)
                .getAsNamedMultipleLines();

        assertThat(result[0], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setAlias("from -Infinity to 50.0")
                .setLabels(Labels.of("sensor", "responseTimeMs"))
                .setGraphData(GraphData.of(
                        DataPoint.point(1000, Double.NaN),
                        DataPoint.point(1500, Double.NaN),
                        DataPoint.point(2000, 5d),
                        DataPoint.point(2500, 5d)
                ))
                .build()));
        assertThat(result[1], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setAlias("from 0.0 to 500.0")
                .setLabels(Labels.of("sensor", "responseTimeMs"))
                .setGraphData(GraphData.of(
                        DataPoint.point(1000, Double.NaN),
                        DataPoint.point(1500, Double.NaN),
                        DataPoint.point(2000, 80d),
                        DataPoint.point(2500, 80d)
                ))
                .build()));
        assertThat(result[2], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setAlias("from 300.0 to 400.0")
                .setLabels(Labels.of("sensor", "responseTimeMs"))
                .setGraphData(GraphData.of(
                        DataPoint.point(1000, Double.NaN),
                        DataPoint.point(1500, Double.NaN),
                        DataPoint.point(2000, 20d),
                        DataPoint.point(2500, 20d)
                ))
                .build()));
        assertThat(result[3], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setAlias("from -Infinity to 10.0")
                .setLabels(Labels.of("sensor", "responseTimeMs"))
                .setGraphData(GraphData.of(
                        DataPoint.point(1000, Double.NaN),
                        DataPoint.point(1500, Double.NaN),
                        DataPoint.point(2000, 1d),
                        DataPoint.point(2500, 1d)
                ))
                .build()));
        assertThat(result[4], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setAlias("from 400.0 to 2000.0")
                .setLabels(Labels.of("sensor", "responseTimeMs"))
                .setGraphData(GraphData.of(
                        DataPoint.point(1000, Double.NaN),
                        DataPoint.point(1500, Double.NaN),
                        DataPoint.point(2000, 40d),
                        DataPoint.point(2500, 38d)
                ))
                .build()));
        assertThat(result[5], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setAlias("from 400.0 to Infinity")
                .setLabels(Labels.of("sensor", "responseTimeMs"))
                .setGraphData(GraphData.of(
                        DataPoint.point(1000, Double.NaN),
                        DataPoint.point(1500, Double.NaN),
                        DataPoint.point(2000, 40d),
                        DataPoint.point(2500, 40d)
                ))
                .build()));
    }

    @Test
    public void nativeHistogramNoMerge() {
        Assume.assumeTrue(SelVersion.HISTOGRAM_FUNCTIONS_DONT_MERGE_3.since(version));

        Labels labels = Labels.of("sensor", "responseTimeMs");

        NamedGraphData hist = NamedGraphData.newBuilder()
                .setLabels(labels)
                .setType(ru.yandex.monlib.metrics.MetricType.HIST)
                .setGraphData(MetricType.HIST_RATE, AggrGraphDataArrayList.of(
                        AggrPoints.point(1000, (Histogram) null),
                        AggrPoints.point(1500, Histogram.newInstance(
                                new double[0], new long[0]
                        )),
                        AggrPoints.point(2000, Histogram.newInstance(
                                new double[] {100, 200, 300, 500, 1000, 2000},
                                new long[]   { 10,  20,  10,  40,   15,    5}
                        )),
                        AggrPoints.point(2500, Histogram.newInstance(
                                new double[] {100, 200, 300, 500, 1000, 2000, Histograms.INF_BOUND},
                                new long[]   { 10,  20,  10,  40,   15,    3,                    2}
                        ))))
                .build();

        NamedGraphData[] result = ProgramTestSupport
                .expression("histogram_cdfp([-inf(), 0, 300, -inf(), 400, 400], [50, 500, 400, 10, 2000, inf()], graphData);")
                .onMultipleLines(hist)
                .exec(version)
                .getAsNamedMultipleLines();

        assertThat(result[0], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setLabels(labels.add("upper_bound", "50.0"))
                .setGraphData(GraphData.of(
                        DataPoint.point(1000, Double.NaN),
                        DataPoint.point(1500, Double.NaN),
                        DataPoint.point(2000, 5d),
                        DataPoint.point(2500, 5d)
                ))
                .build()));
        assertThat(result[1], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setLabels(labels.add("lower_bound", "0.0").add("upper_bound", "500.0"))
                .setGraphData(GraphData.of(
                        DataPoint.point(1000, Double.NaN),
                        DataPoint.point(1500, Double.NaN),
                        DataPoint.point(2000, 80d),
                        DataPoint.point(2500, 80d)
                ))
                .build()));
        assertThat(result[2], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setLabels(labels.add("lower_bound", "300.0").add("upper_bound", "400.0"))
                .setGraphData(GraphData.of(
                        DataPoint.point(1000, Double.NaN),
                        DataPoint.point(1500, Double.NaN),
                        DataPoint.point(2000, 20d),
                        DataPoint.point(2500, 20d)
                ))
                .build()));
        assertThat(result[3], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setLabels(labels.add("upper_bound", "10.0"))
                .setGraphData(GraphData.of(
                        DataPoint.point(1000, Double.NaN),
                        DataPoint.point(1500, Double.NaN),
                        DataPoint.point(2000, 1d),
                        DataPoint.point(2500, 1d)
                ))
                .build()));
        assertThat(result[4], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setLabels(labels.add("lower_bound", "400.0").add("upper_bound", "2000.0"))
                .setGraphData(GraphData.of(
                        DataPoint.point(1000, Double.NaN),
                        DataPoint.point(1500, Double.NaN),
                        DataPoint.point(2000, 40d),
                        DataPoint.point(2500, 38d)
                ))
                .build()));
        assertThat(result[5], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setLabels(labels.add("lower_bound", "400.0"))
                .setGraphData(GraphData.of(
                        DataPoint.point(1000, Double.NaN),
                        DataPoint.point(1500, Double.NaN),
                        DataPoint.point(2000, 40d),
                        DataPoint.point(2500, 40d)
                ))
                .build()));
    }

    @Test
    public void logHistogram() {
        Assume.assumeTrue(SelVersion.HISTOGRAM_FUNCTIONS_DONT_MERGE_3.before(version));
        NamedGraphData logHist = NamedGraphData.newBuilder()
                .setLabels(Labels.of("signal", "resp-time_hgram"))
                .setType(ru.yandex.monlib.metrics.MetricType.LOG_HISTOGRAM)
                .setGraphData(MetricType.LOG_HISTOGRAM, AggrGraphDataArrayList.of(
                        AggrPoints.point(1000, (LogHistogram) null),
                        AggrPoints.point(1500, LogHistogram.newBuilder().build()),
                        AggrPoints.point(2000, LogHistogram.newBuilder()
                                .setBase(2)
                                .setStartPower(1)
                                .setCountZero(2)
                                .setBuckets(new double[] {0, 3, 4, 0, 1})
                                .build()),
                        AggrPoints.point(2500, LogHistogram.newBuilder()
                                .setBase(4)
                                .setStartPower(1)
                                .setCountZero(0)
                                .setBuckets(new double[] {1, 2, 3, 4})
                                .build())))
                .build();

        NamedGraphData[] result = ProgramTestSupport
                .expression("histogram_cdfp([0, 0, 1, -inf(), 0, 0], [0, 1, 8, 4, 32, inf()], graphData);")
                .onMultipleLines(logHist)
                .exec(version)
                .getAsNamedMultipleLines();

        assertThat(result[0], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setAlias("from 0.0 to 0.0")
                .setLabels(Labels.of("signal", "resp-time_hgram"))
                .setGraphData(GraphData.of(
                        DataPoint.point(1000, Double.NaN),
                        DataPoint.point(1500, Double.NaN),
                        DataPoint.point(2000, 20d),
                        DataPoint.point(2500, 0d)
                ))
                .build()));
        assertThat(result[1], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setAlias("from 0.0 to 1.0")
                .setLabels(Labels.of("signal", "resp-time_hgram"))
                .setGraphData(GraphData.of(
                        DataPoint.point(1000, Double.NaN),
                        DataPoint.point(1500, Double.NaN),
                        DataPoint.point(2000, 20d),
                        DataPoint.point(2500, 0d)
                ))
                .build()));
        assertThat(result[2], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setAlias("from 1.0 to 8.0")
                .setLabels(Labels.of("signal", "resp-time_hgram"))
                .setGraphData(GraphData.of(
                        DataPoint.point(1000, Double.NaN),
                        DataPoint.point(1500, Double.NaN),
                        DataPoint.point(2000, 30d),
                        DataPoint.point(2500, 5d)
                ))
                .build()));
        assertThat(result[3], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setAlias("from -Infinity to 4.0")
                .setLabels(Labels.of("signal", "resp-time_hgram"))
                .setGraphData(GraphData.of(
                        DataPoint.point(1000, Double.NaN),
                        DataPoint.point(1500, Double.NaN),
                        DataPoint.point(2000, 20d),
                        DataPoint.point(2500, 0d)
                ))
                .build()));
        assertThat(result[4], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setAlias("from 0.0 to 32.0")
                .setLabels(Labels.of("signal", "resp-time_hgram"))
                .setGraphData(GraphData.of(
                        DataPoint.point(1000, Double.NaN),
                        DataPoint.point(1500, Double.NaN),
                        DataPoint.point(2000, 90d),
                        DataPoint.point(2500, 20d)
                ))
                .build()));
        assertThat(result[5], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setAlias("from 0.0 to Infinity")
                .setLabels(Labels.of("signal", "resp-time_hgram"))
                .setGraphData(GraphData.of(
                        DataPoint.point(1000, Double.NaN),
                        DataPoint.point(1500, Double.NaN),
                        DataPoint.point(2000, 100d),
                        DataPoint.point(2500, 100d)
                ))
                .build()));
    }

    @Test
    public void logHistogramNoMerge() {
        Labels labels = Labels.of("signal", "resp-time_hgram");

        Assume.assumeTrue(SelVersion.HISTOGRAM_FUNCTIONS_DONT_MERGE_3.since(version));
        NamedGraphData logHist = NamedGraphData.newBuilder()
                .setLabels(labels)
                .setType(ru.yandex.monlib.metrics.MetricType.LOG_HISTOGRAM)
                .setGraphData(MetricType.LOG_HISTOGRAM, AggrGraphDataArrayList.of(
                        AggrPoints.point(1000, (LogHistogram) null),
                        AggrPoints.point(1500, LogHistogram.newBuilder().build()),
                        AggrPoints.point(2000, LogHistogram.newBuilder()
                                .setBase(2)
                                .setStartPower(1)
                                .setCountZero(2)
                                .setBuckets(new double[] {0, 3, 4, 0, 1})
                                .build()),
                        AggrPoints.point(2500, LogHistogram.newBuilder()
                                .setBase(4)
                                .setStartPower(1)
                                .setCountZero(0)
                                .setBuckets(new double[] {1, 2, 3, 4})
                                .build())))
                .build();

        NamedGraphData[] result = ProgramTestSupport
                .expression("histogram_cdfp([0, 0, 1, -inf(), 0, 0], [0, 1, 8, 4, 32, inf()], graphData);")
                .onMultipleLines(logHist)
                .exec(version)
                .getAsNamedMultipleLines();

        assertThat(result[0], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setLabels(labels.add("lower_bound", "0.0").add("upper_bound", "0.0"))
                .setGraphData(GraphData.of(
                        DataPoint.point(1000, Double.NaN),
                        DataPoint.point(1500, Double.NaN),
                        DataPoint.point(2000, 20d),
                        DataPoint.point(2500, 0d)
                ))
                .build()));
        assertThat(result[1], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setLabels(labels.add("lower_bound", "0.0").add("upper_bound", "1.0"))
                .setGraphData(GraphData.of(
                        DataPoint.point(1000, Double.NaN),
                        DataPoint.point(1500, Double.NaN),
                        DataPoint.point(2000, 20d),
                        DataPoint.point(2500, 0d)
                ))
                .build()));
        assertThat(result[2], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setLabels(labels.add("lower_bound", "1.0").add("upper_bound", "8.0"))
                .setGraphData(GraphData.of(
                        DataPoint.point(1000, Double.NaN),
                        DataPoint.point(1500, Double.NaN),
                        DataPoint.point(2000, 30d),
                        DataPoint.point(2500, 5d)
                ))
                .build()));
        assertThat(result[3], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setLabels(labels.add("upper_bound", "4.0"))
                .setGraphData(GraphData.of(
                        DataPoint.point(1000, Double.NaN),
                        DataPoint.point(1500, Double.NaN),
                        DataPoint.point(2000, 20d),
                        DataPoint.point(2500, 0d)
                ))
                .build()));
        assertThat(result[4], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setLabels(labels.add("lower_bound", "0.0").add("upper_bound", "32.0"))
                .setGraphData(GraphData.of(
                        DataPoint.point(1000, Double.NaN),
                        DataPoint.point(1500, Double.NaN),
                        DataPoint.point(2000, 90d),
                        DataPoint.point(2500, 20d)
                ))
                .build()));
        assertThat(result[5], equalTo(NamedGraphData.newBuilder()
                .setType(ru.yandex.monlib.metrics.MetricType.DGAUGE)
                .setLabels(labels.add("lower_bound", "0.0"))
                .setGraphData(GraphData.of(
                        DataPoint.point(1000, Double.NaN),
                        DataPoint.point(1500, Double.NaN),
                        DataPoint.point(2000, 100d),
                        DataPoint.point(2500, 100d)
                ))
                .build()));
    }
}
