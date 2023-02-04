package ru.yandex.solomon.expression.expr.op.bin;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.monlib.metrics.MetricType;
import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.expression.NamedGraphData;
import ru.yandex.solomon.expression.value.SelValue;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.solomon.model.timeseries.GraphData;

/**
 * @author Oleg Baryshnikov
 */
public class OneToOneMatcherTest {

    @Test
    public void emptySources() {
        NamedGraphData[] source1 = new NamedGraphData[0];
        NamedGraphData[] source2 = new NamedGraphData[0];
        NamedGraphData[] expected = new NamedGraphData[0];
        testEval(expected, source1, source2);
    }

    @Test
    public void emptySource() {
        NamedGraphData[] source1 = new NamedGraphData[]{};
        NamedGraphData[] source2 = new NamedGraphData[]{
            namedGraphData("requests", Labels.of())
        };
        NamedGraphData[] expected = new NamedGraphData[0];
        testEval(expected, source1, source2);
        testEval(expected, source2, source1);
    }

    @Test
    public void singleDifferentTimeseries() {
        NamedGraphData[] source1 = new NamedGraphData[]{
            namedGraphData("requests", Labels.of("endpoint", "a"))
        };
        NamedGraphData[] source2 = new NamedGraphData[]{
            namedGraphData("failedRequests", Labels.of("endpoint", "b"))
        };
        NamedGraphData[] expected = new NamedGraphData[] {
            namedGraphData("", Labels.of())
        };
        testEval(expected, source1, source2);
    }

    @Test
    public void singleSameTimeseries() {
        NamedGraphData[] source1 = new NamedGraphData[]{
            namedGraphData("requests", Labels.of("endpoint", "a"))
        };

        NamedGraphData[] source2 = new NamedGraphData[]{
            namedGraphData("requests", Labels.of("endpoint", "a"))
        };
        NamedGraphData[] expected = new NamedGraphData[] {
            namedGraphData("requests", Labels.of("endpoint", "a"))
        };
        testEval(expected, source1, source2);
    }

    @Test
    public void singleTimeseries() {
        NamedGraphData[] source1 = new NamedGraphData[]{
            namedGraphData("requests", Labels.of("endpoint", "a"))
        };

        NamedGraphData[] source2 = new NamedGraphData[]{
            namedGraphData("failedRequests", Labels.of("endpoint", "a"))
        };
        NamedGraphData[] expected = new NamedGraphData[] {
            namedGraphData("", Labels.of("endpoint", "a"))
        };
        testEval(expected, source1, source2);
    }

    @Test
    public void singleTimeseriesForOldMetrics() {
        NamedGraphData[] source1 = new NamedGraphData[]{
            namedGraphData("", Labels.of("sensor", "failedRequests", "endpoint", "a"))
        };

        NamedGraphData[] source2 = new NamedGraphData[]{
            namedGraphData("", Labels.of("sensor", "requests", "endpoint", "a"))
        };
        NamedGraphData[] expected = new NamedGraphData[] {
            namedGraphData("", Labels.of("endpoint", "a"))
        };
        testEval(expected, source1, source2);
    }

    @Test
    public void oneToOneMatching() {
        NamedGraphData[] source1 = new NamedGraphData[]{
            namedGraphData("errors", Labels.of("method", "get")),
            namedGraphData("errors", Labels.of("method", "post")),
        };

        NamedGraphData[] source2 = new NamedGraphData[]{
            namedGraphData("requests", Labels.of("method", "get")),
            namedGraphData("requests", Labels.of("method", "post")),
            namedGraphData("requests", Labels.of("method", "put")),
        };

        NamedGraphData[] expected = new NamedGraphData[]{
            namedGraphData("", Labels.of("method", "get")),
            namedGraphData("", Labels.of("method", "post")),
        };

        testEval(expected, source1, source2);
    }

    @Test
    public void oneToOneMatchingWithCommonLabels() {
        NamedGraphData[] source1 = new NamedGraphData[]{
            namedGraphData("errors", Labels.of("host", "solomon", "method", "get")),
            namedGraphData("errors", Labels.of("host", "solomon", "method", "post")),
        };

        NamedGraphData[] source2 = new NamedGraphData[]{
            namedGraphData("requests", Labels.of("host", "solomon", "method", "get")),
            namedGraphData("requests", Labels.of("host", "solomon", "method", "post")),
            namedGraphData("requests", Labels.of("host", "solomon", "method", "put")),
        };

        NamedGraphData[] expected = new NamedGraphData[]{
            namedGraphData("", Labels.of("host", "solomon", "method", "get")),
            namedGraphData("", Labels.of("host", "solomon", "method", "post")),
        };

        testEval(expected, source1, source2);
    }

    @Test
    public void oneToOneMatchingIgnoringCode() {
        NamedGraphData[] source1 = new NamedGraphData[]{
            namedGraphData("errors", Labels.of("code", "500", "method", "get")),
            namedGraphData("errors", Labels.of("code", "500", "method", "post")),
        };

        NamedGraphData[] source2 = new NamedGraphData[]{
            namedGraphData("requests", Labels.of("method", "get")),
            namedGraphData("requests", Labels.of("method", "post")),
            namedGraphData("requests", Labels.of("method", "put")),
        };

        NamedGraphData[] expected = new NamedGraphData[]{
            namedGraphData("", Labels.of("method", "get")),
            namedGraphData("", Labels.of("method", "post")),
        };

        testEval(expected, source1, source2);
    }

    @Test
    public void oneToOneMatchingByMethodAndCode() {
        NamedGraphData[] source1 = new NamedGraphData[]{
            namedGraphData("errors", Labels.of("code", "500", "method", "get")),
            namedGraphData("errors", Labels.of("code", "500", "method", "post")),
        };

        NamedGraphData[] source2 = new NamedGraphData[]{
            namedGraphData("requests", Labels.of("code", "500", "method", "get")),
            namedGraphData("requests", Labels.of("code", "400", "method", "post")),
            namedGraphData("requests", Labels.of("code", "500", "method", "post")),
            namedGraphData("requests", Labels.of("code", "400", "method", "put")),
        };

        NamedGraphData[] expected = new NamedGraphData[]{
            namedGraphData("", Labels.of("code", "500", "method", "get")),
            namedGraphData("", Labels.of("code", "500", "method", "post")),
        };

        testEval(expected, source1, source2);
    }

    @Test
    public void oneToOneMatchingForDifferentMetricsNames() {
        NamedGraphData[] source1 = new NamedGraphData[]{
            namedGraphData("errors200", Labels.of("method", "get")),
            namedGraphData("errors500", Labels.of("method", "get")),
        };

        NamedGraphData[] source2 = new NamedGraphData[]{
            namedGraphData("errors200", Labels.of("sensor", "requests", "method", "get")),
            namedGraphData("errors500", Labels.of("sensor", "requests", "method", "get")),
            namedGraphData("errors500", Labels.of("sensor", "requests", "method", "post")),
        };

        NamedGraphData[] expected = new NamedGraphData[]{
            namedGraphData("errors200", Labels.of("method", "get")),
            namedGraphData("errors500", Labels.of("method", "get")),
        };

        testEval(expected, source1, source2);
    }

    @Test
    public void oneToOneMatchingForDifferentMetricDimensions() {
        NamedGraphData[] source1 = new NamedGraphData[]{
            namedGraphData("nailsCount", Labels.of("pocketId", "1")),
            namedGraphData("nailsCount", Labels.of("pocketId", "2")),
        };

        NamedGraphData[] source2 = new NamedGraphData[]{
            namedGraphData("bucketsCount", Labels.of("boxId", "1")),
            namedGraphData("bucketsCount", Labels.of("boxId", "2")),
            namedGraphData("bucketsCount", Labels.of("boxId", "3")),
        };

        NamedGraphData[] expected = new NamedGraphData[0];

        testEval(expected, source1, source2);
    }

    @Test
    public void sourcesWithEmptyLabels() {
        NamedGraphData[] source1 = new NamedGraphData[]{
            namedGraphData("", Labels.of()),
            namedGraphData("", Labels.of()),
            namedGraphData("", Labels.of()),
        };

        NamedGraphData[] source2 = new NamedGraphData[]{
            namedGraphData("", Labels.of()),
            namedGraphData("", Labels.of()),
            namedGraphData("", Labels.of()),
        };

        NamedGraphData[] expected = new NamedGraphData[] {
            namedGraphData("", Labels.of()),
            namedGraphData("", Labels.of()),
            namedGraphData("", Labels.of()),
            namedGraphData("", Labels.of()),
            namedGraphData("", Labels.of()),
            namedGraphData("", Labels.of()),
            namedGraphData("", Labels.of()),
            namedGraphData("", Labels.of()),
            namedGraphData("", Labels.of()),
        };
        testEval(expected, source1, source2);
    }

    private static NamedGraphData namedGraphData(String metricName, Labels labels) {
        return new NamedGraphData(
            "",
            MetricType.DGAUGE,
            metricName,
            labels,
            AggrGraphDataArrayList.of()
        );
    }

    private void testEval(
        NamedGraphData[] expected,
        NamedGraphData[] source1,
        NamedGraphData[] source2)
    {
        SelValue[] actual =
            OneToOneMatcher.match(source1, source2, (graphData, graphData2) -> GraphData.empty);

        NamedGraphData[] actual2 = Arrays.stream(actual)
            .map(x -> x.castToGraphData().getNamedGraphData())
            .toArray(NamedGraphData[]::new);

        Assert.assertArrayEquals(expected, actual2);
    }
}
