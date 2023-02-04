package ru.yandex.solomon.expression;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.solomon.expression.test.ForEachSelVersionRunner;
import ru.yandex.solomon.expression.test.SelCompilerTestBase;
import ru.yandex.solomon.expression.value.SelValueBoolean;
import ru.yandex.solomon.expression.value.SelValueDouble;
import ru.yandex.solomon.expression.value.SelValueGraphData;
import ru.yandex.solomon.expression.value.SelValueObject;
import ru.yandex.solomon.expression.value.SelValueString;
import ru.yandex.solomon.expression.value.SelValueVector;
import ru.yandex.solomon.expression.version.SelVersion;
import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.model.timeseries.SortedOrCheck;

/**
 * @author Stepan Koltsov
 */
@RunWith(ForEachSelVersionRunner.class)
public class ElTest extends SelCompilerTestBase {

    public ElTest(SelVersion version) {
        super(version);
    }

    @Test
    public void param() {
        Assert.assertEquals(new SelValueDouble(100), eval("xx", "xx", new SelValueDouble(100)));
    }

    @Test
    public void boolConst() {
        Assert.assertEquals(SelValueBoolean.TRUE, eval("true"));
        Assert.assertEquals(SelValueBoolean.FALSE, eval("false"));
    }

    @Test
    public void multipliers() {
        Assert.assertEquals(new SelValueDouble(50000), eval("50k"));
    }

    @Test
    public void div() {
        Assert.assertEquals(new SelValueDouble(2), eval("xx / yy",
                "xx", new SelValueDouble(100),
                "yy", new SelValueDouble(50)));
    }

    @Test
    public void comparison() {
        Assert.assertEquals(SelValueBoolean.TRUE, eval("10 < 20"));
        Assert.assertEquals(SelValueBoolean.FALSE, eval("0.2 > 2e3"));
        Assert.assertEquals(SelValueBoolean.TRUE, eval("0.2 >= 0.2"));
    }

    @Test
    public void boolNot() {
        Assert.assertEquals(SelValueBoolean.TRUE, eval("!false"));
        //Assert.assertEquals(SelValueBoolean.TRUE, eval0("!!true"));
    }

    @Test
    public void boolAndOr() {
        Assert.assertEquals(SelValueBoolean.TRUE, eval("true && true || true && false"));
    }

    @Test
    public void math() {
        Assert.assertEquals(new SelValueDouble(2), eval("xx / (yy + zz)",
                "xx", new SelValueDouble(10),
                "yy", new SelValueDouble(2),
                "zz", new SelValueDouble(3)));
    }

    @Test
    public void vecMath() {
        Assert.assertEquals(new SelValueVector(new double[]{ 10, 20 }), eval("xx + yy",
                "xx", new SelValueVector(new double[]{ 7, 12 }),
                "yy", new SelValueVector(new double[]{ 3, 8 })));
    }

    @Test
    public void random01() {
        for (double d = 0; d <= 0.9; d += 0.1) {
            double value = eval("random01()").castToScalar().getValue();
            if (value >= d && value <= d + 0.1) {
                break;
            }
        }
    }

    @Test
    public void graphData() {
        GraphData p1 = new GraphData(new long[]{ 10000, 11000 }, new double[] { 100, 200 }, SortedOrCheck.CHECK);
        GraphData p2 = new GraphData(new long[]{ 10000, 12000 }, new double[] { 100, 200 }, SortedOrCheck.CHECK);
        Assert.assertEquals(new SelValueDouble(200), eval("avg(p1 + p2)",
                "p1", new SelValueGraphData(p1),
                "p2", new SelValueGraphData(p2)));
    }

    @Test
    public void object() {
        SelValueObject expected =
            new SelValueObject(Collections.singletonMap("result", new SelValueDouble(100)));

        SelValueObject actual = eval("{ result: 100 }").castToObject();

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void minusNegative() {
        testEval(new SelValueDouble(7), "10 - 3");
    }

    @Test
    public void gtNegative() {
        testEval(SelValueBoolean.TRUE, "0 > -1");
    }

    @Test
    public void bug1() {
        testEval(SelValueBoolean.TRUE,
                "(avg(a) - avg(b)) < 100",
                "a", new SelValueVector(new double[] { 150, 100 }),
                "b", new SelValueVector(new double[] { 100 }));
    }

    @Test
    public void stringConstant() {
        testEval(new SelValueString("aabb"),
                "\"aabb\"");
    }

    @Test
    public void ternaryOp() {
        testEval(SelValueBoolean.FALSE, "true ? false : true");

        testEval(new SelValueString("pass"),
            "((avg(a) - avg(b)) > 100) ? 'fail' : 'pass'",
            "a", new SelValueVector(new double[] { 150, 100 }),
            "b", new SelValueVector(new double[] { 100 }));

        testEval(new SelValueString("fail"),
            "((avg(a) - avg(b)) < 100) ? 'fail' : 'pass'",
            "a", new SelValueVector(new double[] { 150, 100 }),
            "b", new SelValueVector(new double[] { 100 }));
    }
}
