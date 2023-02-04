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
public class ConvTest {
    @Test
    public void convTwoArg() {
        YasmSelRenderer.RenderResult converted = RenderTestSupport.renderExpression(
                "conv(memory_bytes_tmmm, Gi)"
        );
        String expected = "{signal='memory_bytes_tmmm'} / Gi";
        assertEquals(expected, converted.expression);
        assertEquals(Map.of("Gi", new SelValueDouble(Math.pow(2, 30))), converted.constants);
    }

    @Test
    public void convNumber() {
        YasmSelRenderer.RenderResult converted = RenderTestSupport.renderExpression(
                "conv(memory_bytes_tmmm, 100500)"
        );
        String expected = "{signal='memory_bytes_tmmm'} / 100500";
        assertEquals(expected, converted.expression);
        assertTrue(converted.constants.isEmpty());
    }

    @Test
    public void convFromTo() {
        YasmSelRenderer.RenderResult converted = RenderTestSupport.renderExpression(
                "conv(memory_mb_tmmm, Mi, Gi)"
        );
        String expected = "{signal='memory_mb_tmmm'} * (Mi / Gi)";
        assertEquals(expected, converted.expression);
        assertEquals(Map.of(
                "Mi", new SelValueDouble(Math.pow(2, 20)),
                "Gi", new SelValueDouble(Math.pow(2, 30))),
                converted.constants);
    }
}
