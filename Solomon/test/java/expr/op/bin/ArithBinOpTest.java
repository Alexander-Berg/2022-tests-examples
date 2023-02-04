package ru.yandex.solomon.expression.expr.op.bin;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.monlib.metrics.MetricType;
import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.expression.NamedGraphData;
import ru.yandex.solomon.expression.analytics.GraphDataLoaderStub;
import ru.yandex.solomon.expression.analytics.Program;
import ru.yandex.solomon.expression.exceptions.CompilerException;
import ru.yandex.solomon.expression.test.ForEachSelVersionRunner;
import ru.yandex.solomon.expression.test.SelCompilerTestBase;
import ru.yandex.solomon.expression.type.SelTypes;
import ru.yandex.solomon.expression.value.SelValue;
import ru.yandex.solomon.expression.value.SelValueDouble;
import ru.yandex.solomon.expression.value.SelValueGraphData;
import ru.yandex.solomon.expression.value.SelValueVector;
import ru.yandex.solomon.expression.version.SelVersion;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.AggrPoints;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.model.timeseries.SortedOrCheck;
import ru.yandex.solomon.util.time.Interval;

/**
 * @author Stepan Koltsov
 */
@RunWith(ForEachSelVersionRunner.class)
public class ArithBinOpTest extends SelCompilerTestBase {
    public ArithBinOpTest(SelVersion version) {
        super(version);
    }

    @Test
    public void plus() {
        testEval(new SelValueDouble(10), "a + b", "a", new SelValueDouble(7), "b", new SelValueDouble(3));
        testEval(new SelValueDouble(11), "a + b + c",
                "a", new SelValueDouble(7),
                "b", new SelValueDouble(3),
                "c", new SelValueDouble(1));
        testEval(new SelValueDouble(10), "a + b",
            "a", new SelValueDouble(10),
            "b", new SelValueDouble(Double.NaN));
        testEval(new SelValueDouble(10), "a + b",
            "a", new SelValueDouble(Double.NaN),
            "b", new SelValueDouble(10));
        testEval(new SelValueDouble(Double.NaN), "a + b",
            "a", new SelValueDouble(Double.NaN),
            "b", new SelValueDouble(Double.NaN));
    }

    @Test
    public void minus() {
        testEval(new SelValueVector(new double[]{ 3, 6 }), "a - b",
                "a", new SelValueVector(new double[]{ 10, 8 }),
                "b", new SelValueVector(new double[]{ 7, 2 }));
        testEval(new SelValueDouble(10), "a - b",
            "a", new SelValueDouble(10),
            "b", new SelValueDouble(Double.NaN));
        testEval(new SelValueDouble(-10), "a - b",
            "a", new SelValueDouble(Double.NaN),
            "b", new SelValueDouble(10));
        testEval(new SelValueDouble(Double.NaN), "a - b",
            "a", new SelValueDouble(Double.NaN),
            "b", new SelValueDouble(Double.NaN));
    }

    @Test
    public void mul() {
        testEval(new SelValueDouble(10), "a * b",
                "a", new SelValueDouble(2),
                "b", new SelValueDouble(5));
        testEval(new SelValueVector(new double[] { 10, 15 }), "a * b",
                "a", new SelValueVector(new double[] { 2, 3}),
                "b", new SelValueDouble(5));
        testEval(new SelValueVector(new double[] { 10, 15 }), "a * b",
                "a", new SelValueDouble(5),
                "b", new SelValueVector(new double[] { 2, 3}));
        testEval(new SelValueGraphData(
                        new GraphData(new long[] { 1000, 2000 }, new double[] { 10, 15 }, SortedOrCheck.CHECK)),
                "a * b",
                "a", new SelValueDouble(5),
                "b", new SelValueGraphData(new GraphData(new long[]{ 1000, 2000 }, new double[] { 2, 3 }, SortedOrCheck.CHECK)));
    }

    @Test
    public void graphDataAndGraphData() {
        SelValue source1 = namedGraphData("errors", Labels.of("method", "get", "code", "500"), 24);
        SelValue source2 = namedGraphData("errors", Labels.of("method", "get"), 120);
        SelValue expected = namedGraphData("errors", Labels.of("method", "get"), 0.2);
        testEval(expected, "a / b", "a", source1, "b", source2);
    }

    @Test
    public void graphDataAndScalar() {
        SelValue source1 = namedGraphData("errors", Labels.of("method", "get"), 24);
        SelValue source2 = new SelValueDouble(10);
        SelValue expected = namedGraphData("errors", Labels.of("method", "get"), 240);
        testEval(expected, "a * b", "a", source1, "b", source2);
    }

    @Test(expected = CompilerException.class)
    public void graphDataAndScalarVector() {
        SelValue source1 = namedGraphData("errors", Labels.of("method", "get"), 24);
        SelValue source2 = new SelValueVector(new double[]{10, 20});
        SelValue expected = namedGraphData("", Labels.of(), 0);
        testEval(expected, "a * b", "a", source1, "b", source2);
    }

    @Test
    public void graphDataVectorAndScalar() {
        SelValue source1 = graphDataVector(namedGraphData("errors", Labels.of("method", "get"), 24));
        SelValue source2 = new SelValueDouble(5);
        SelValue expected = graphDataVector(namedGraphData("errors", Labels.of("method", "get"), 120));
        testEval(expected, "a * b", "a", source1, "b", source2);
    }

    @Test(expected = CompilerException.class)
    public void graphDataVectorAndScalarVector() {
        SelValue source1 = graphDataVector(namedGraphData("errors", Labels.of("method", "get"), 24));
        SelValue source2 = new SelValueVector(new double[]{10, 20});
        SelValue expected = namedGraphData("", Labels.of(), 0);
        testEval(expected, "a * b", "a", source1, "b", source2);
    }

    @Test
    public void graphDataAndGraphDataAsVectors() {
        SelValue source1 = graphDataVector(namedGraphData("errors", Labels.of("method", "get", "code", "500"), 24));
        SelValue source2 = graphDataVector(namedGraphData("errors", Labels.of("method", "get"), 120));
        SelValue expected = graphDataVector(namedGraphData("errors", Labels.of("method", "get"), 0.2));
        testEval(expected, "a / b", "a", source1, "b", source2);
    }

    @Test
    public void graphAndGraphNotSameWithVectorsBeforeV2() {
        SelValue source1 = namedGraphData("errors", Labels.of("foo", "value"), 24);
        SelValue source2 = namedGraphData("errors", Labels.of("bar", "other"), 120);
        SelValue expected = namedGraphData("errors", Labels.of(), 0.2);

        boolean thrown = false;
        try {
            testEval(expected, "a / b", "a", source1, "b", source2);
            testEval(graphDataVector(expected), "a / b", "a", graphDataVector(source1), "b", source2);
            testEval(graphDataVector(expected), "a / b", "a", source1, "b", graphDataVector(source2));
            testEval(graphDataVector(expected), "a / b", "a",
                    graphDataVector(source1), "b", graphDataVector(source2));
        } catch (AssertionError e) {
            e.printStackTrace();
            thrown = true;
        }

        boolean hasBug = SelVersion.GROUP_LINES_RETURN_VECTOR_2.before(version);
        Assert.assertEquals(thrown, hasBug);
    }

    @Test
    public void oneToManyGraphDatasMatching() {
        SelValueVector source1 = new SelValueVector(SelTypes.GRAPH_DATA, new SelValue[] {
          namedGraphData("totalRequests", Labels.of(), 240),
        });

        SelValueVector source2 = new SelValueVector(SelTypes.GRAPH_DATA, new SelValue[] {
          namedGraphData("requests", Labels.of("method", "get"), 120),
          namedGraphData("requests", Labels.of("method", "post"), 60),
          namedGraphData("requests", Labels.of("method", "put"), 60),
        });

        SelValueVector expected = new SelValueVector(SelTypes.GRAPH_DATA, new SelValue[] {
            namedGraphData("requests", Labels.of("method", "get"), 2),
            namedGraphData("requests", Labels.of("method", "post"), 4),
            namedGraphData("requests", Labels.of("method", "put"), 4),
        });

        testEval(expected, "a / b", "a", source1, "b", source2);
        testEval(expected, "a / b", "a", source1.valueArray()[0], "b", source2);
    }

    @Test
    public void manyToOneGraphDatasMatching() {
        SelValueVector source1 = new SelValueVector(SelTypes.GRAPH_DATA, new SelValue[] {
            namedGraphData("requests", Labels.of("method", "get"), 120),
            namedGraphData("requests", Labels.of("method", "post"), 60),
            namedGraphData("requests", Labels.of("method", "put"), 60),
        });

        SelValueVector source2 = new SelValueVector(SelTypes.GRAPH_DATA, new SelValue[] {
          namedGraphData("totalRequests", Labels.of(), 240),
        });

        SelValueVector expected = new SelValueVector(SelTypes.GRAPH_DATA, new SelValue[] {
            namedGraphData("requests", Labels.of("method", "get"), 0.5),
            namedGraphData("requests", Labels.of("method", "post"), 0.25),
            namedGraphData("requests", Labels.of("method", "put"), 0.25),
        });

        testEval(expected, "a / b", "a", source1, "b", source2);
        testEval(expected, "a / b", "a", source1, "b", source2.valueArray()[0]);
    }

    /**
     * @see OneToOneMatcher
     */
    @Test
    public void oneToOneGraphDatasMatching() {
        SelValue source1 = graphDataVector(
            namedGraphData("errors", Labels.of("method", "get"), 24),
            namedGraphData("errors", Labels.of("method", "post"), 6)
        );

        SelValue source2 = graphDataVector(
            namedGraphData("requests", Labels.of("method", "get"), 120),
            namedGraphData("requests", Labels.of("method", "post"), 60),
            namedGraphData("requests", Labels.of("method", "put"), 10)
        );

        SelValue expected = graphDataVector(
            namedGraphData("", Labels.of("method", "get"), 0.2),
            namedGraphData("", Labels.of("method", "post"), 0.1)
        );

        testEval(expected, "a / b", "a", source1, "b", source2);
    }

    @Test
    public void longToDouble() {
        NamedGraphData ngd = eval("0.5 - source", "source", new SelValueGraphData(NamedGraphData.newBuilder()
                .setType(MetricType.IGAUGE)
                .setGraphData(AggrGraphDataArrayList.of(
                        AggrPoints.lpoint(1000, -3),
                        AggrPoints.lpoint(2000, -2),
                        AggrPoints.lpoint(3000, -1),
                        AggrPoints.lpoint(4000, 0),
                        AggrPoints.lpoint(5000, 1),
                        AggrPoints.lpoint(6000, 2),
                        AggrPoints.lpoint(7000, 3)
                ))
                .build()))
                .castToGraphData()
                .getNamedGraphData();

        NamedGraphData expected = NamedGraphData.newBuilder()
                .setType(MetricType.DGAUGE)
                .setGraphData(AggrGraphDataArrayList.of(
                        AggrPoints.dpoint(1000, 3.5),
                        AggrPoints.dpoint(2000, 2.5),
                        AggrPoints.dpoint(3000, 1.5),
                        AggrPoints.dpoint(4000, 0.5),
                        AggrPoints.dpoint(5000, -0.5),
                        AggrPoints.dpoint(6000, -1.5),
                        AggrPoints.dpoint(7000, -2.5)
                ))
                .build();

        Assert.assertEquals(expected, ngd);
    }

    @Test
    public void metricTypeIsSpecified() {
        GraphDataLoaderStub loader = new GraphDataLoaderStub();
        loader.putSelectorValue("{line='*'}", GraphData.empty, GraphData.empty);
        var evaluationResult = Program.fromSourceWithReturn(version, "sum({line='*'}) by junk + constant_line(0, 30s)", false)
                .compile()
                .prepare(Interval.seconds(2000, 3000))
                .evaluate(loader, Map.of());

        NamedGraphData result = evaluationResult.get("external$0").castToGraphData().getNamedGraphData();
        Assert.assertNotEquals(ru.yandex.solomon.model.protobuf.MetricType.METRIC_TYPE_UNSPECIFIED, result.getDataType());
    }

    @Test
    public void addWithConstantLine() {
        GraphDataLoaderStub loader = new GraphDataLoaderStub();
        loader.putSelectorValue("{host='*'}", NamedGraphData.newBuilder()
                .setLabels(Labels.of("host", "solomon"))
                .setGraphData(GraphData.empty)
                .build());
        var evaluationResult = Program.fromSourceWithReturn(version,
                "{host='*'} + constant_line(0, 30s)", false)
                .compile()
                .prepare(Interval.seconds(2000, 3000))
                .evaluate(loader, Map.of());

        NamedGraphData result = evaluationResult.get("external$0").castToGraphData().getNamedGraphData();
        Assert.assertNotEquals(Labels.of(), result.getLabels());
    }

    private static SelValue graphDataVector(SelValue... values) {
        return new SelValueVector(SelTypes.GRAPH_DATA, values);
    }

    private static SelValue namedGraphData(String metricName, Labels labels, double constantValue) {
        return new SelValueGraphData(new NamedGraphData(
            "",
            MetricType.DGAUGE,
            metricName,
            labels,
            AggrGraphDataArrayList.of(
                AggrPoint.shortPoint(10, constantValue),
                AggrPoint.shortPoint(20, constantValue),
                AggrPoint.shortPoint(30, constantValue),
                AggrPoint.shortPoint(40, constantValue),
                AggrPoint.shortPoint(50, constantValue)
            )
        ));
    }
}
