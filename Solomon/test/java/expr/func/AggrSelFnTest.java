package ru.yandex.solomon.expression.expr.func;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.expression.NamedGraphData;
import ru.yandex.solomon.expression.exceptions.EvaluationException;
import ru.yandex.solomon.expression.test.ForEachSelVersionRunner;
import ru.yandex.solomon.expression.test.SelCompilerTestBase;
import ru.yandex.solomon.expression.type.SelTypes;
import ru.yandex.solomon.expression.value.SelValueDouble;
import ru.yandex.solomon.expression.value.SelValueGraphData;
import ru.yandex.solomon.expression.value.SelValueVector;
import ru.yandex.solomon.expression.version.SelVersion;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.model.timeseries.SortedOrCheck;

/**
 * @author Stepan Koltsov
 */
@RunWith(ForEachSelVersionRunner.class)
public class AggrSelFnTest extends SelCompilerTestBase {
    public AggrSelFnTest(SelVersion version) {
        super(version);
    }

    private void testAggrFunc(String name, double[] param, double expected) {
        Assert.assertEquals(new SelValueDouble(expected), eval(name + "(xx)",
                "xx", new SelValueVector(param)));
    }

    private void testAggrFunc(String name, GraphData param, double expected) {
        Assert.assertEquals(new SelValueDouble(expected), eval(name + "(xx)",
                "xx", new SelValueGraphData(param)));
    }

    @Test
    public void minMax() {
        testAggrFunc("max", new double[]{ 5, 7, 10, -3, Double.NaN, 3 }, 10);
        testAggrFunc("min", new double[]{ 5, 7, 10, -3, Double.NaN, 3 }, -3);
    }

    @Test
    public void std() {
        testAggrFunc("std", new double[] { 3, 4, 5 }, 1);
        testAggrFunc("std", new double[] { 3 }, Double.NaN);
        testAggrFunc("std", new double[] { }, Double.NaN);
    }

    @Test
    public void sum() {
        testAggrFunc("sum", new double[] { 3, 4, 5, Double.NaN }, 12);
    }

    @Test
    public void integrate() {
        GraphData graphData = new GraphData(
            new long[]{ 1000, 2000 },
            new double[]{ 1, 3 },
            SortedOrCheck.CHECK);
        testAggrFunc("integrate", graphData, 3);
    }

    @Test
    public void median() {
        GraphData graphData = new GraphData(
                new long[]{ 1000, 2000, 4000 },
                new double[]{ 3, 4, 1 },
                SortedOrCheck.CHECK);
        double[] data = new double[] { 4, 1, 10, 7 };

        testAggrFunc("median", GraphData.empty, Double.NaN);
        testAggrFunc("median", graphData, 3);
        testAggrFunc("median", data, 5.5);
    }

    @Test
    public void iqr() {
        GraphData graphData = new GraphData(
                new long[]{ 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                new double[]{ 3, 4, 1, 8, 4, 10, 17, 6, 7 }, // [1 3 4 4] 6 [7 8 10 17]
                SortedOrCheck.CHECK);
        double[] data = new double[] { 4, 1, 10, 7 }; // [1 4 7 10]

        testAggrFunc("iqr", GraphData.empty, Double.NaN);
        testAggrFunc("iqr", graphData, 9 - 3.5);
        testAggrFunc("iqr", data, 7.5);

        Random rand = new Random(42);
        double[] sample = IntStream.range(0, 10000).mapToDouble(ignore -> rand.nextGaussian()).toArray();
        double result = eval("iqr(sample)", "sample", new SelValueVector(sample)).castToScalar().getValue();
        Assert.assertEquals(1.349, result, 0.005);
    }

    private void testPercentile(double percLevel, double[] param, double expected) {
        Assert.assertEquals(new SelValueDouble(expected), eval("percentile(" + percLevel + ", xx)",
                "xx", new SelValueVector(param)));
    }

    private void testPercentile(double percLevel, GraphData param, double expected) {
        Assert.assertEquals(new SelValueDouble(expected), eval("percentile(" + percLevel + ", xx)",
                "xx", new SelValueGraphData(param)));
    }

    @Test
    public void perncentile() {
        GraphData graphData = new GraphData(
                new long[]{ 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                new double[]{ 3, 4, 1, 8, 4, 10, 17, 6, 7 }, // [1 3 4 4] 6 [7 8 10 17]
                SortedOrCheck.CHECK);
        double[] data = new double[] { 4, 1, 10, 7 }; // [1 4 7 10]

        testPercentile(33, GraphData.empty, Double.NaN);
        testPercentile(75, graphData, 9);
        testPercentile(25, data, 1.75);
    }

    @Test
    public void aggregateNonsingletonVector() {
        List<NamedGraphData> ngds = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            ngds.add(NamedGraphData.of(Labels.of("foo", "bar" + i), DataPoint.point(1000, 42d)));
        }

        boolean catched = false;

        try {
            eval("last(gd)", "gd", new SelValueVector(SelTypes.GRAPH_DATA, ngds.stream()
                    .map(SelValueGraphData::new)
                    .toArray(SelValueGraphData[]::new)));
        } catch (EvaluationException e) {
            String message = e.getMessage();
            Assert.assertNotNull(message);
            Assert.assertTrue(message.contains("..."));
            Assert.assertFalse(message.contains("{foo='bar10'}"));
            Assert.assertTrue(message.contains("{foo='bar0'}"));
            Assert.assertTrue(message.contains("{foo='bar19'}"));
            catched = true;
        }
        Assert.assertTrue("Should throw EvaluationException", catched);
    }

}
