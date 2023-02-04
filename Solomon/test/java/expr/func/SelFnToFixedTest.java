package ru.yandex.solomon.expression.expr.func;

import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.solomon.expression.test.ForEachSelVersionRunner;
import ru.yandex.solomon.expression.test.SelCompilerTestBase;
import ru.yandex.solomon.expression.value.SelValueString;
import ru.yandex.solomon.expression.version.SelVersion;

/**
 * @author Stepan Koltsov
 */
@RunWith(ForEachSelVersionRunner.class)
public class SelFnToFixedTest extends SelCompilerTestBase {

    public SelFnToFixedTest(SelVersion version) {
        super(version);
    }

    @Test
    public void toFixed() {
        test("12.13", 2, 12.131);
        test("-3", 0, -3);
        test("-3.0", 1, -3);
        test("-3.00", 2, -3);
        test("-3.125", 3, -3.125);
        test("-3.0", 1, -3.001);
        test("-3.00", 2, -3.001);
        test("-3.001", 3, -3.001);
        test("-3.0010", 4, -3.001);

        for (int i = 0; i < 1000; ++i) {
            test(Integer.toString(i), 0, i);
            test(Integer.toString(i) + ".0", 1, i);
            test(Integer.toString(i) + ".00", 2, i);

            test(Integer.toString(i), 0, i + .4);
            test(Integer.toString(i) + ".5", 1, i + .5);
            test(Integer.toString(i) + ".50", 2, i + .5);
        }
    }

    @Test
    public void toFixedRound() {
        test("0.6", 1, 0.59687);
        test("1", 0, 1.4);
        test("1", 0, 1.49);
        test("2", 0, 1.5);

        test("-3", 0, -3.005);
        test("-3.0", 1, -3.005);
        test("-3.01", 2, -3.005);
        test("-3.005", 3, -3.005);
        test("-3.0050", 4, -3.005);
    }

    public void test(String expected, int digits, double value) {
        testEval(new SelValueString(expected), "to_fixed(" + value + ", " + digits + ")");
    }
}
