package ru.yandex.solomon.expression.expr.func;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.solomon.expression.test.ForEachSelVersionRunner;
import ru.yandex.solomon.expression.test.SelCompilerTestBase;
import ru.yandex.solomon.expression.type.SelTypes;
import ru.yandex.solomon.expression.value.SelValueDouble;
import ru.yandex.solomon.expression.value.SelValueGraphData;
import ru.yandex.solomon.expression.value.SelValueVector;
import ru.yandex.solomon.expression.version.SelVersion;
import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.model.timeseries.SortedOrCheck;
import ru.yandex.solomon.model.timeseries.Timeline;

/**
 * @author Stepan Koltsov
 */
@RunWith(ForEachSelVersionRunner.class)
public class DoubleSelFnTest extends SelCompilerTestBase {

    public DoubleSelFnTest(SelVersion version) {
        super(version);
    }

    private GraphData makeGraphData(double[] param) {
        long[] tss = new long[param.length];
        for (int i = 0; i < tss.length; i++) {
            tss[i] = 15_000L * (i + 1);
        }
        Timeline timeline = new Timeline(tss, SortedOrCheck.SORTED_UNIQUE);
        return new GraphData(timeline, param);
    }

    private void testGraphDataFunc(String name, double[] param, double[] expected) {
        GraphData inputGd = makeGraphData(param);
        GraphData expectedGd = new GraphData(inputGd.getTimeline(), expected);

        Assert.assertEquals(expectedGd, eval(name + "(gd)",
                "gd", new SelValueGraphData(inputGd)).castToGraphData().getGraphData());
        Assert.assertEquals(expectedGd, eval(name + "(gd)",
                "gd", new SelValueVector(SelTypes.GRAPH_DATA, new SelValueGraphData[] {
                        new SelValueGraphData(inputGd)
                })).castToGraphData().getGraphData());
    }

    private void testDoubleArrayFunc(String name, double[] param, double[] expected) {
        Assert.assertEquals(new SelValueVector(expected), eval(name + "(xx)",
                "xx", new SelValueVector(param)));
    }

    private void testDoubleFunc(String name, double param, double expected) {
        Assert.assertEquals(new SelValueDouble(expected), eval(name + "(xx)",
                "xx", new SelValueDouble(param)));
    }

    private void testGraphDataBiFunc(String name, double p1, double[] param, double[] expected) {
        GraphData inputGd = makeGraphData(param);
        GraphData expectedGd = new GraphData(inputGd.getTimeline(), expected);

        Assert.assertEquals(expectedGd, eval(name + "(gd, p1)",
                "gd", new SelValueGraphData(inputGd),
                "p1", new SelValueDouble(p1)
            ).castToGraphData().getGraphData());
        Assert.assertEquals(expectedGd, eval(name + "(gd, p1)",
                "gd", new SelValueVector(SelTypes.GRAPH_DATA, new SelValueGraphData[] {
                        new SelValueGraphData(inputGd)
                }),
                "p1", new SelValueDouble(p1)
            ).castToGraphData().getGraphData());
    }

    private void testDoubleArrayBiFunc(String name, double p1, double[] param, double[] expected) {
        Assert.assertEquals(new SelValueVector(expected), eval(name + "(xx, p1)",
                "xx", new SelValueVector(param),
                "p1", new SelValueDouble(p1)));
    }

    private void testDoubleBiFunc(String name, double p1, double param, double expected) {
        Assert.assertEquals(new SelValueDouble(expected), eval(name + "(xx, p1)",
                "xx", new SelValueDouble(param),
                "p1", new SelValueDouble(p1)));
    }

    private void testFunc(String func, double... xy) {
        Assert.assertEquals(0, xy.length % 2);
        int n = xy.length / 2;
        double[] x = new double[n];
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = xy[2 * i];
            y[i] = xy[2 * i + 1];
            testDoubleFunc(func, x[i], y[i]);
        }
        testDoubleArrayFunc(func, x, y);
        testGraphDataFunc(func, x, y);
    }

    private void testBiFunc(String func, double p1, double... xy) {
        Assert.assertEquals(0, xy.length % 2);
        int n = xy.length / 2;
        double[] x = new double[n];
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = xy[2 * i];
            y[i] = xy[2 * i + 1];
            testDoubleBiFunc(func, p1, x[i], y[i]);
        }
        testDoubleArrayBiFunc(func, p1, x, y);
        testGraphDataBiFunc(func, p1, x, y);
    }

    @Test
    public void sqr() {
        testFunc("sqr", 0, 0,
                3, 9,
                4, 16,
                -3, 9,
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    @Test
    public void sqrt() {
        testFunc("sqrt", 0, 0,
                4, 2,
                -4, Double.NaN,
                Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    @Test
    public void sqrViaPow() {
        testBiFunc("pow", 2, 0, 0,
                3, 9,
                4, 16,
                -3, 9,
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    @Test
    public void sqrtViaPow() {
        testBiFunc("pow", 0.5, 0, 0,
                4, 2,
                -4, Double.NaN,
                Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    @Test
    public void mod() {
        testBiFunc("mod", 5, 0, 0,
                42, 2,
                -4, -4,
                Double.POSITIVE_INFINITY, Double.NaN,
                Double.NEGATIVE_INFINITY, Double.NaN);
    }

    @Test
    public void modZero() {
        testBiFunc("mod", 0, 0, Double.NaN,
                42, Double.NaN,
                -4, Double.NaN,
                Double.POSITIVE_INFINITY, Double.NaN,
                Double.NEGATIVE_INFINITY, Double.NaN);
    }

    @Test
    public void abs() {
        testFunc("abs", 0, 0,
                4, 4,
                -4, 4,
                Double.NaN, Double.NaN,
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    @Test
    public void sign() {
        testFunc("sign", 0, 0,
                3, 1,
                -4, -1,
                1, 1,
                Double.NaN, Double.NaN,
                Double.POSITIVE_INFINITY, 1,
                Double.NEGATIVE_INFINITY, -1);
    }

    @Test
    public void heaviside() {
        testFunc("heaviside", 0, 0,
                3, 1,
                -4, 0,
                1, 1,
                Double.NaN, Double.NaN,
                Double.POSITIVE_INFINITY, 1,
                Double.NEGATIVE_INFINITY, 0);
    }

    @Test
    public void ramp() {
        testFunc("ramp", 0, 0,
                3, 3,
                -4, 0,
                1, 1,
                Double.NaN, Double.NaN,
                Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, 0);
    }

    @Test
    public void exp() {
        testFunc("exp", 0, 1,
                3, Math.exp(3),
                -4, Math.exp(-4),
                1, Math.E,
                Double.NaN, Double.NaN,
                Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, 0);
    }

    @Test
    public void log() {
        testFunc("log", 0, Double.NEGATIVE_INFINITY,
                3, Math.log(3),
                -4, Double.NaN,
                Math.E, 1,
                1, 0,
                Double.NaN, Double.NaN,
                Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, Double.NaN);
    }

    @Test
    public void floor() {
        testFunc("floor", 0, 0,
                Math.PI, 3,
                42, 42,
                42.5, 42,
                -Math.E, -3,
                Double.NaN, Double.NaN,
                Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
    }

    @Test
    public void ceil() {
        testFunc("ceil", 0, 0,
                Math.PI, 4,
                42, 42,
                42.5, 43,
                -Math.E, -2,
                Double.NaN, Double.NaN,
                Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
    }

    @Test
    public void round() {
        testFunc("round", 0, 0,
                Math.PI, 3,
                42, 42,
                42.5, 43,
                -Math.E, -3,
                Double.NaN, Double.NaN,
                Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
    }

    @Test
    public void fract() {
        testFunc("fract", 0, 0,
                Math.PI, Math.PI - 3,
                42, 0,
                42.5, 0.5,
                -Math.E, -(Math.E - 2),
                Double.NaN, Double.NaN,
                Double.POSITIVE_INFINITY, 0, // this is how Python modf works
                Double.NEGATIVE_INFINITY, 0);
    }

    @Test
    public void trunc() {
        testFunc("trunc", 0, 0,
                Math.PI, 3,
                42, 42,
                42.5, 42,
                -Math.E, -2,
                -3, -3,
                Double.NaN, Double.NaN,
                Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
    }

    @Test
    public void testMultipleGraphData() {
        GraphData first = makeGraphData(new double[] { -3, -2, -1, 0, 1, 2, 3 });
        GraphData second = makeGraphData(new double[] { Double.NEGATIVE_INFINITY, Double.NaN, Double.POSITIVE_INFINITY });

        GraphData firstExpected = makeGraphData(new double[] { 9, 4, 1, 0, 1, 4, 9 });
        GraphData secondExpected = makeGraphData(new double[] { Double.POSITIVE_INFINITY, Double.NaN, Double.POSITIVE_INFINITY });

        SelValueVector input = new SelValueVector(SelTypes.GRAPH_DATA, new SelValueGraphData[] { new SelValueGraphData(first), new SelValueGraphData(second) });
        SelValueVector expected = new SelValueVector(SelTypes.GRAPH_DATA, new SelValueGraphData[] { new SelValueGraphData(firstExpected), new SelValueGraphData(secondExpected) });
        Assert.assertEquals(expected, eval("sqr(gd)", "gd", input));
    }

}
