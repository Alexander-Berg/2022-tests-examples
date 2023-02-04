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
public class SelFuncRegistryTest {
    private SelFuncRegistry registry;

    @Before
    public void setUp() throws Exception {
        registry = new SelFuncRegistry();
    }

    @Test(expected = CompilerException.class)
    public void notExists() {
        registry.get(SelVersion.MAX, "magic", List.of(SelTypes.GRAPH_DATA_VECTOR));
        fail("When requested function not exists should be throw correspond exception");
    }

    @Test
    public void registered() {
        var fn = SelFunc.newBuilder()
            .name("magic")
            .help("make magic")
            .category(SelFuncCategory.OTHER)
            .args(SelTypes.GRAPH_DATA_VECTOR)
            .returnType(SelTypes.GRAPH_DATA_VECTOR)
            .handler(params -> params.get(0))
            .build();

        registry.add(fn);
        var result = registry.get(SelVersion.MAX, fn.getName(), fn.getArgsType());
        assertSame(fn, result);
    }

    @Test
    public void overload() {
        var fnOne = SelFunc.newBuilder()
            .name("magic")
            .help("make magic")
            .category(SelFuncCategory.OTHER)
            .args(SelTypes.GRAPH_DATA_VECTOR)
            .returnType(SelTypes.GRAPH_DATA_VECTOR)
            .handler(params -> params.get(0))
            .build();

        var fnTwo = SelFunc.newBuilder()
            .name("magic")
            .help("make magic")
            .args(SelTypes.GRAPH_DATA)
            .category(SelFuncCategory.OTHER)
            .returnType(SelTypes.GRAPH_DATA)
            .handler(params -> params.get(0))
            .build();

        registry.add(fnOne);
        registry.add(fnTwo);

        for (var expected : List.of(fnOne, fnTwo)) {
            var result = registry.get(SelVersion.MAX, expected.getName(), expected.getArgsType());
            assertSame(expected, result);
        }
    }

    @Test(expected = InternalCompilerException.class)
    public void collision() {
        var fnOne = SelFunc.newBuilder()
            .name("magic")
            .help("make magic")
            .category(SelFuncCategory.OTHER)
            .args(SelTypes.GRAPH_DATA_VECTOR)
            .returnType(SelTypes.GRAPH_DATA_VECTOR)
            .handler(params -> params.get(0))
            .build();

        var fnTwo = SelFunc.newBuilder()
            .name("magic")
            .help("make magic")
            .category(SelFuncCategory.OTHER)
            .args(SelTypes.GRAPH_DATA_VECTOR)
            .returnType(SelTypes.DOUBLE)
            .handler(params -> new SelValueDouble(42))
            .build();

        registry.add(fnOne);
        registry.add(fnTwo);
        fail("not able register function with same function twice");
    }


    @Test
    public void versioned() {
        Assume.assumeTrue(SelVersion.MAX != SelVersion.MIN);

        var fnOne = SelFunc.newBuilder()
                .name("magic")
                .help("make magic")
                .category(SelFuncCategory.OTHER)
                .args(SelTypes.GRAPH_DATA_VECTOR)
                .returnType(SelTypes.GRAPH_DATA_VECTOR)
                .handler(params -> params.get(0))
                .supportedVersions(SelVersion.MAX::before)
                .build();

        var fnTwo = SelFunc.newBuilder()
                .name("magic")
                .help("make magic")
                .args(SelTypes.GRAPH_DATA)
                .category(SelFuncCategory.OTHER)
                .returnType(SelTypes.GRAPH_DATA)
                .handler(params -> params.get(0))
                .supportedVersions(SelVersion.MAX::since)
                .build();

        registry.add(fnOne);
        registry.add(fnTwo);

        assertSame(fnOne, registry.get(SelVersion.MIN, fnOne.getName(), fnOne.getArgsType()));
        assertSame(fnTwo, registry.get(SelVersion.MAX, fnTwo.getName(), fnTwo.getArgsType()));

        try {
            registry.get(SelVersion.MAX, fnOne.getName(), fnOne.getArgsType());
            fail("fnOne is deleted in MAX version");
        } catch (CompilerException ignore) {
        }

        try {
            registry.get(SelVersion.MIN, fnTwo.getName(), fnTwo.getArgsType());
            fail("fnTwo is deleted in supported in MAX version only");
        } catch (CompilerException ignore) {
        }
    }
}
