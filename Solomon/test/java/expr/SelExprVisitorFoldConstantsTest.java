package ru.yandex.solomon.expression.expr;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.solomon.expression.compile.CompileContext;
import ru.yandex.solomon.expression.compile.SelCompiler;
import ru.yandex.solomon.expression.test.ForEachSelVersionRunner;
import ru.yandex.solomon.expression.test.VersionedSelTestBase;
import ru.yandex.solomon.expression.type.SelTypes;
import ru.yandex.solomon.expression.value.SelValueDouble;
import ru.yandex.solomon.expression.version.SelVersion;

/**
 * @author Stepan Koltsov
 */
@RunWith(ForEachSelVersionRunner.class)
public class SelExprVisitorFoldConstantsTest extends VersionedSelTestBase {

    private final SelCompiler compiler;

    public SelExprVisitorFoldConstantsTest(SelVersion version) {
        super(version);
        this.compiler = new SelCompiler(version);
    }

    @Test
    public void foldConst() {
        SelExpr expr = compiler.compileExpr("x + 2 * 3", new CompileContext(Map.of("x", SelTypes.DOUBLE)));
        SelExpr folded = expr.visit(new SelExprVisitorFoldConstants(
            List.of(new SelExprVisitorFoldConstants.ParamMaybeKnownValue("x", SelTypes.DOUBLE)), version));

        SelExpr expected = compiler.compileExpr("x + 6", new CompileContext(Map.of("x", SelTypes.DOUBLE)));

        assertEquals(expected, folded);
    }

    @Test
    public void foldParam() {
        SelExpr expr = compiler.compileExpr("x + 2 * 3", new CompileContext(Map.of("x", SelTypes.DOUBLE)));
        SelExpr folded = expr.visit(new SelExprVisitorFoldConstants(
            List.of(new SelExprVisitorFoldConstants.ParamMaybeKnownValue("x", new SelValueDouble(1))), version));

        SelExpr expected = compiler.compileExpr("7", new CompileContext(Map.of("x", SelTypes.DOUBLE)));

        assertEquals(expected, folded);
    }

    @Test
    public void foldObject() {
        SelExpr expr = compiler.compileExpr("{ result: x + 2 * 3 }", new CompileContext(Map.of("x", SelTypes.DOUBLE)));
        SelExpr folded = expr.visit(new SelExprVisitorFoldConstants(
            List.of(new SelExprVisitorFoldConstants.ParamMaybeKnownValue("x", new SelValueDouble(1))), version));

        SelExpr expected = compiler.compileExpr("{ result: 7 }", new CompileContext(Map.of("x", SelTypes.DOUBLE)));

        assertEquals(expected, folded);
    }

    private void assertEquals(SelExpr expected, SelExpr folded) {
        Assert.assertEquals(expected, folded);
    }
}
