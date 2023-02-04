package ru.yandex.solomon.expression.test;

import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.solomon.expression.type.SelTypes;
import ru.yandex.solomon.expression.value.SelValue;
import ru.yandex.solomon.expression.value.SelValueString;
import ru.yandex.solomon.expression.value.SelValueVector;
import ru.yandex.solomon.expression.version.SelVersion;

/**
 * @author Stepan Koltsov
 */
@RunWith(ForEachSelVersionRunner.class)
public class LambdaTest extends SelCompilerTestBase {
    public LambdaTest(SelVersion version) {
        super(version);
    }

    @Test
    public void doubleToObject() {
        testEval(
            new SelValueVector(SelTypes.STRING, new SelValue[]{ new SelValueString("2.0"), new SelValueString("3.0") }),
            "map(v, x -> to_string(x))", "v", new SelValueVector(new double[]{ 2.0, 3.0 }));
    }

    @Test
    public void doubleToDouble() {
        testEval(
            new SelValueVector(new double[] { 6.0, 7.0 }),
            "map(v, (x) -> x + 1)", "v", new SelValueVector(new double[] { 5.0, 6.0 }));
    }

}
