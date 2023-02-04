package ru.yandex.solomon.expression.expr.func;

import java.util.List;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.solomon.expression.exceptions.CompilerException;
import ru.yandex.solomon.expression.exceptions.InternalCompilerException;
import ru.yandex.solomon.expression.type.SelTypes;
import ru.yandex.solomon.expression.value.SelValueDouble;
import ru.yandex.solomon.expression.version.SelVersion;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

/**
 * @author Vladimir Gordiychuk
 */
public class SelOpRegistryTest {
    private SelOpRegistry registry;

    @Before
    public void setUp() throws Exception {
        registry = new SelOpRegistry();
    }

    @Test(expected = CompilerException.class)
    public void notExists() {
        registry.get(SelVersion.MAX, "+", List.of(SelTypes.DOUBLE, SelTypes.DOUBLE));
        fail("When requested operator not exists should be throw correspond exception");
    }

    @Test
    public void registered() {
        var op = SelOp.newBuilder()
            .name("plus")
            .operator("+")
            .args(SelTypes.DOUBLE, SelTypes.DOUBLE)
            .returnType(SelTypes.DOUBLE)
            .handler(params -> params.get(0))
            .build();

        registry.add(op);
        var result = registry.get(SelVersion.MAX, op.getOperator(), op.getArgs());
        assertSame(op, result);
    }

    @Test
    public void overload() {
        var opOne = SelOp.newBuilder()
            .name("plus")
            .operator("+")
            .args(SelTypes.DOUBLE, SelTypes.DOUBLE)
            .returnType(SelTypes.DOUBLE)
            .handler(params -> params.get(0))
            .build();

        var opTwo = SelOp.newBuilder()
            .name("concat")
            .operator("+")
            .args(SelTypes.STRING, SelTypes.STRING)
            .returnType(SelTypes.STRING)
            .handler(params -> params.get(0))
            .build();

        registry.add(opOne);
        registry.add(opTwo);

        for (var expected : List.of(opOne, opTwo)) {
            var result = registry.get(SelVersion.MAX, expected.getOperator(), expected.getArgs());
            assertSame(expected, result);
        }
    }

    @Test
    public void versioned() {
        Assume.assumeTrue(SelVersion.MAX != SelVersion.MIN);

        var opOne = SelOp.newBuilder()
                .name("plus")
                .operator("+")
                .args(SelTypes.DOUBLE, SelTypes.DOUBLE)
                .returnType(SelTypes.DOUBLE)
                .handler(params -> params.get(0))
                .supportedVersions(SelVersion.MAX::before)
                .build();

        var opTwo = SelOp.newBuilder()
                .name("concat")
                .operator("+")
                .args(SelTypes.STRING, SelTypes.STRING)
                .returnType(SelTypes.STRING)
                .handler(params -> params.get(0))
                .supportedVersions(SelVersion.MAX::since)
                .build();

        registry.add(opOne);
        registry.add(opTwo);

        assertSame(opOne, registry.get(SelVersion.MIN, opOne.getOperator(), opOne.getArgs()));
        assertSame(opTwo, registry.get(SelVersion.MAX, opTwo.getOperator(), opTwo.getArgs()));

        try {
            registry.get(SelVersion.MAX, opOne.getOperator(), opOne.getArgs());
            fail("opOne is deleted in MAX version");
        } catch (CompilerException ignore) {
        }

        try {
            registry.get(SelVersion.MIN, opTwo.getOperator(), opTwo.getArgs());
            fail("opTwo is deleted in supported in MAX version only");
        } catch (CompilerException ignore) {
        }
    }

    @Test(expected = InternalCompilerException.class)
    public void collision() {
        var opOne = SelOp.newBuilder()
            .name("plus")
            .operator("+")
            .args(SelTypes.DOUBLE, SelTypes.DOUBLE)
            .returnType(SelTypes.DOUBLE)
            .handler(params -> params.get(0))
            .build();

        var opTwo = SelOp.newBuilder()
            .name("plus")
            .operator("+")
            .args(SelTypes.DOUBLE, SelTypes.DOUBLE)
            .returnType(SelTypes.DOUBLE)
            .handler(params -> new SelValueDouble(42))
            .build();

        registry.add(opOne);
        registry.add(opTwo);
        fail("not able register operator with same function twice");
    }
}
