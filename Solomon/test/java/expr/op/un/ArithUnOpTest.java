package ru.yandex.solomon.expression.expr.op.un;

import java.time.Duration;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.solomon.expression.test.ForEachSelVersionRunner;
import ru.yandex.solomon.expression.test.SelCompilerTestBase;
import ru.yandex.solomon.expression.value.SelValueDouble;
import ru.yandex.solomon.expression.value.SelValueDuration;
import ru.yandex.solomon.expression.version.SelVersion;

/**
 * @author Oleg Baryshnikov
 */
@ParametersAreNonnullByDefault
@RunWith(ForEachSelVersionRunner.class)
public class ArithUnOpTest extends SelCompilerTestBase {

    public ArithUnOpTest(SelVersion version) {
        super(version);
    }

    @Test
    public void plus() {
        testEval(new SelValueDouble(0), "+0");
        testEval(new SelValueDouble(10), "+10");
        testEval(new SelValueDouble(Double.NaN), "+a", "a", new SelValueDouble(Double.NaN));
        testEval(new SelValueDuration(Duration.ZERO), "+a", "a", new SelValueDuration(Duration.ZERO));
        testEval(new SelValueDuration(Duration.ofHours(1)), "+a", "a", new SelValueDuration(Duration.ofHours(1)));
    }

    @Test
    public void minus() {
        // Because result of evaluation is -0.0
        Assert.assertEquals(0, eval("-0").castToScalar().getValue(), Double.MIN_VALUE);
        testEval(new SelValueDouble(-10), "-10");
        testEval(new SelValueDouble(Double.NaN), "-a", "a", new SelValueDouble(Double.NaN));
        testEval(new SelValueDuration(Duration.ZERO), "-a", "a", new SelValueDuration(Duration.ZERO));
        testEval(new SelValueDuration(Duration.ofHours(1).negated()), "-a", "a", new SelValueDuration(Duration.ofHours(1)));
    }
}
