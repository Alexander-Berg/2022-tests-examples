package ru.yandex.solomon.expression.expr.op.bin;

import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.solomon.expression.test.ForEachSelVersionRunner;
import ru.yandex.solomon.expression.test.SelCompilerTestBase;
import ru.yandex.solomon.expression.value.SelValueBoolean;
import ru.yandex.solomon.expression.value.SelValueDouble;
import ru.yandex.solomon.expression.value.SelValueString;
import ru.yandex.solomon.expression.version.SelVersion;

/**
 * @author Maksim Leonov
 */
@RunWith(ForEachSelVersionRunner.class)
public class StringBinOpTest extends SelCompilerTestBase {

    public StringBinOpTest(SelVersion version) {
        super(version);
    }

    @Test
    public void plus() {
        testEval(new SelValueString("ab"), "a + b", "a", new SelValueString("a"), "b", new SelValueString("b"));
        testEval(new SelValueString("a b"), "a + ' ' + b", "a", new SelValueString("a"), "b", new SelValueString("b"));
        testEval(new SelValueString("a 0.0"), "a + b", "a", new SelValueString("a "), "b", new SelValueDouble(0));
        testEval(new SelValueString("true b"), "a + b", "a", SelValueBoolean.TRUE, "b", new SelValueString(" b"));
    }
}
