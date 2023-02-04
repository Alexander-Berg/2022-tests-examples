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
public class SelFnToStringTest extends SelCompilerTestBase {

    public SelFnToStringTest(SelVersion version) {
        super(version);
    }

    @Test
    public void bool_to_string() {
        testEval(new SelValueString("true"), "to_string(true)");
        testEval(new SelValueString("false"), "to_string(false)");
    }

    @Test
    public void double_to_string() {
        testEval(new SelValueString("0.0"), "to_string(0.0)");
    }

    @Test
    public void string_to_string() {
        testEval(new SelValueString("aa"), "to_string(\"aa\")");
    }
}
