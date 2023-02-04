package ru.yandex.solomon.expression;

import java.util.Map;

import org.junit.Test;

import ru.yandex.monlib.metrics.MetricType;
import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.expression.analytics.Program;
import ru.yandex.solomon.expression.exceptions.CompilerException;
import ru.yandex.solomon.expression.exceptions.EvaluationException;
import ru.yandex.solomon.expression.exceptions.ParserException;
import ru.yandex.solomon.expression.version.SelVersion;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.solomon.model.type.LogHistogram;
import ru.yandex.solomon.util.time.Interval;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Ivan Tsybulin
 */
public class ErrorsFormattingTest {
    @Test
    public void oneLinerEof() {
        String code = "let foo = 42";
        String message = "" +
            "At 1:13 parsing error: Expected PUNCT{\";\"} token but EOF{\"\"} found\n" +
            "  1 | let foo = 42\n" +
            "                  ^";

        try {
            Program.fromSource(code).compile();
            fail("This code should fail");
        } catch (ParserException e) {
            String explained = Program.explainError(code, e);
            assertThat(explained, equalTo(message));
        }
    }

    @Test
    public void oneLinerMid() {
        String code = "let foo = 42 * 'x' + 42;";
        String message = "" +
            "At 1:14 compiling error: Not valid operator arguments, was double * String, but expected [double * double, " +
            "double * vector of double, vector of double * double, double * GraphData, GraphData * double, double * vector " +
            "of GraphData, vector of GraphData * double, vector of double * vector of double, GraphData * GraphData, GraphData " +
            "* vector of GraphData, vector of GraphData * GraphData, vector of GraphData * vector of GraphData]\n" +
            "  1 | let foo = 42 * 'x' + 42;\n" +
            "                   ^";

        try {
            Program.fromSource(code).compile();
            fail("This code should fail");
        } catch (CompilerException e) {
            String explained = Program.explainError(code, e);
            assertThat(explained, equalTo(message));
        }
    }

    @Test
    public void oneLinerLongMid() {
        String code = "let foo = group_lines('xyz', {project=solomon});";
        String message = "" +
            "At 1:23-27 evaluation error: Unknown aggregation function: xyz\n" +
            "  1 | let foo = group_lines('xyz', {project=solomon});\n" +
            "                            ^~~~~";

        try {
            Program.fromSource(code)
                .compile()
                .prepare(Interval.seconds(1000, 2000))
                .evaluate(request -> new NamedGraphData[0], Map.of());
            fail("This code should fail");
        } catch (EvaluationException e) {
            String explained = Program.explainError(code, e);
            assertThat(explained, equalTo(message));
        }
    }

    @Test
    public void multiline() {
        String code = "" +
            "let data = 2 * {\n" +
            "  project=yasm_upper,\n" +
            "  cluster=group_0,\n" +
            "  service=yasm,\n" +
            "  signal=requests\n" +
            "};\n\n" +
            "data";

        String message = "" +
            "At 1:12-6:1 evaluation error: UnsupportedOperationException: Not able convert from LOG_HISTOGRAM to " +
            "DGAUGE\n" +
            "  1 | let data = 2 * {\n" +
            "                 ^~~~~\n" +
            "  2 |   project=yasm_upper,\n" +
            "      ~~~~~~~~~~~~~~~~~~~~~\n" +
            "  3 |   cluster=group_0,\n" +
            "      ~~~~~~~~~~~~~~~~~~\n" +
            "  4 |   service=yasm,\n" +
            "      ~~~~~~~~~~~~~~~\n" +
            "  5 |   signal=requests\n" +
            "      ~~~~~~~~~~~~~~~~~\n" +
            "  6 | };\n" +
            "      ~";

        try {
            LogHistogram hist = LogHistogram.newBuilder()
                .setStartPower(1)
                .setBase(1.5)
                .setMaxBucketsSize(10)
                .setCountZero(10)
                .addBucket(100)
                .addBucket(10)
                .addBucket(20)
                .build();
            NamedGraphData ngd = new NamedGraphData(
                "", MetricType.LOG_HISTOGRAM, "", Labels.of(),
                AggrGraphDataArrayList.of(AggrPoint.shortPoint(1500_000, hist))
            );
            var result = Program.fromSourceWithReturn(SelVersion.MAX, code, false)
                .compile()
                .prepare(Interval.seconds(1000, 2000))
                .evaluate(request -> new NamedGraphData[] { ngd }, Map.of());
            fail("This code should fail");
        } catch (EvaluationException e) {
            String explained = Program.explainError(code, e);
            assertThat(explained, equalTo(message));
        }
    }

    @Test
    public void multiline2() {
        String code = "" +
            "let threshold = 42;\n\n" +
            "let data = single({\n" +
            "  project=solomon,\n" +
            "  cluster=production,\n" +
            "  service=alerting,\n" +
            "  sensor=errors\n" +
            "});\n\n" +
            "alarm_if(sum(data) > threshold);";

        String message = "At 3:19-8:1 evaluation error: no lines found for request: {{project='solomon', " +
            "cluster='production', service='alerting', sensor='errors'}}\n" +
            "  3 | let data = single({\n" +
            "                        ^\n" +
            "  4 |   project=solomon,\n" +
            "      ~~~~~~~~~~~~~~~~~~\n" +
            "  5 |   cluster=production,\n" +
            "      ~~~~~~~~~~~~~~~~~~~~~\n" +
            "  6 |   service=alerting,\n" +
            "      ~~~~~~~~~~~~~~~~~~~\n" +
            "  7 |   sensor=errors\n" +
            "      ~~~~~~~~~~~~~~~\n" +
            "  8 | });\n" +
            "      ~";

        try {
            Program.fromSource(code)
                .compile()
                .prepare(Interval.seconds(1000, 2000))
                .evaluate(request -> new NamedGraphData[0], Map.of());
            fail("This code should fail");
        } catch (EvaluationException e) {
            String explained = Program.explainError(code, e);
            assertThat(explained, equalTo(message));
        }
    }
}
