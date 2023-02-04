package ru.yandex.solomon.yasm.expression.functions;

import org.junit.Test;

import ru.yandex.solomon.yasm.expression.RenderTestSupport;
import ru.yandex.solomon.yasm.expression.grammar.YasmSelRenderer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Ivan Tsybulin
 */
public class PercTest {
    @Test
    public void perc() {
        YasmSelRenderer.RenderResult converted = RenderTestSupport.renderExpression(
                "perc(fusion_nonrunnable_nodes-non_runnable_tmmm,fusion_nonrunnable_nodes-total_tmmm)");
        String expected = "100 * ({signal='fusion_nonrunnable_nodes-non_runnable_tmmm'} / " +
                "{signal='fusion_nonrunnable_nodes-total_tmmm'})";
        assertEquals(expected, converted.expression);
        assertTrue(converted.constants.isEmpty());
    }

    @Test
    public void percPattern() {
        YasmSelRenderer.RenderResult converted = RenderTestSupport.renderExpression("perc(req_<5xx|2xx>_tmmm)");
        String expected = "100 * ({signal='req_5xx_tmmm'} / {signal='req_2xx_tmmm'})";
        assertEquals(expected, converted.expression);
        assertTrue(converted.constants.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void percBadPattern() {
        RenderTestSupport.renderExpression("perc(req_<2xx|4xx|5xx>_tmmm)");
    }

}
