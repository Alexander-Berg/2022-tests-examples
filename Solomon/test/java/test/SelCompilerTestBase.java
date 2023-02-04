package ru.yandex.solomon.expression.test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;

import ru.yandex.solomon.expression.analytics.Program;
import ru.yandex.solomon.expression.compile.SelCompiler;
import ru.yandex.solomon.expression.exceptions.SelException;
import ru.yandex.solomon.expression.value.SelValue;
import ru.yandex.solomon.expression.version.SelVersion;

/**
 * @author Stepan Koltsov
 */
public class SelCompilerTestBase extends VersionedSelTestBase {

    private final SelCompiler compiler;

    public SelCompilerTestBase(SelVersion version) {
        super(version);
        compiler = new SelCompiler(version);
    }

    private SelValue eval(String program, Map<String, SelValue> context) {
        try {
            return compiler.eval(program, context);
        } catch (SelException e) {
            System.err.println(Program.explainError(program, e));
            throw e;
        }
    }

    protected SelValue eval(String program) {
        return eval(program, Collections.emptyMap());
    }

    protected SelValue eval(String program, String name1, SelValue value1) {
        return eval(program, Collections.singletonMap(name1, value1));
    }

    protected SelValue eval(String program,
            String name1, SelValue value1,
            String name2, SelValue value2)
    {
        HashMap<String, SelValue> params = new HashMap<>();
        params.put(name1, value1);
        params.put(name2, value2);
        return eval(program, params);
    }

    protected SelValue eval(String program,
            String name1, SelValue value1,
            String name2, SelValue value2,
            String name3, SelValue value3)
    {
        HashMap<String, SelValue> params = new HashMap<>();
        params.put(name1, value1);
        params.put(name2, value2);
        params.put(name3, value3);
        return eval(program, params);
    }

    protected void testEval(SelValue expected, String program) {
        Assert.assertEquals("Evaluating " + program, expected, eval(program));
    }

    protected void testEval(SelValue expected, String program, String p1, SelValue v1) {
        String debugInfo = program + " with " + p1 + " = " + v1;
        Assert.assertEquals(debugInfo, expected, eval(program, p1, v1));
    }

    protected void testEval(SelValue expected, String program, String p1, SelValue v1, String p2, SelValue v2) {
        String debugInfo = program + " with " + p1 + " = " + v1 + ", " + p2 + " = " + v2;
        Assert.assertEquals(debugInfo, expected, eval(program, p1, v1, p2, v2));
    }

    protected void testEval(SelValue expected, String program,
            String p1, SelValue v1, String p2, SelValue v2, String p3, SelValue v3)
    {
        String debugInfo = program + " with " + p1 + " = " + v1 + ", " + p2 + " = " + v2 + ", " + p3 + " = " + v3;
        Assert.assertEquals(debugInfo, expected, eval(program, p1, v1, p2, v2, p3, v3));
    }


}
