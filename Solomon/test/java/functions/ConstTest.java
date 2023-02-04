package ru.yandex.solomon.yasm.expression.functions;

import java.util.Map;

import org.junit.Test;

import ru.yandex.solomon.expression.value.SelValueDouble;
import ru.yandex.solomon.yasm.expression.RenderTestSupport;
import ru.yandex.solomon.yasm.expression.grammar.YasmSelRenderer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Ivan Tsybulin
 */
public class ConstTest {
    @Test
    public void constTwoArg() {
        YasmSelRenderer.RenderResult converted = RenderTestSupport.renderExpression(
                "const(42, Ki)"
        );
        String expected = "constant_line(42 * Ki)";
        assertEquals(expected, converted.expression);
        assertEquals(Map.of("Ki", new SelValueDouble(Math.pow(2, 10))), converted.constants);
    }

    @Test
    public void constNumber() {
        YasmSelRenderer.RenderResult converted = RenderTestSupport.renderExpression(
                "const(100500)"
        );
        String expected = "constant_line(100500)";
        assertEquals(expected, converted.expression);
        assertTrue(converted.constants.isEmpty());
    }
}
