package ru.yandex.solomon.yasm.expression.functions;

import org.junit.Test;

import ru.yandex.solomon.yasm.expression.RenderTestSupport;
import ru.yandex.solomon.yasm.expression.grammar.YasmSelRenderer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Ivan Tsybulin
 */
public class DivTest {
    @Test
    public void divSignal() {
        YasmSelRenderer.RenderResult converted = RenderTestSupport.renderExpression(
                "div(fusion_nonrunnable_nodes-non_runnable_tmmm,fusion_nonrunnable_nodes-total_tmmm)");
        String expected = "{signal='fusion_nonrunnable_nodes-non_runnable_tmmm'} / " +
                "{signal='fusion_nonrunnable_nodes-total_tmmm'}";
        assertEquals(expected, converted.expression);
        assertTrue(converted.constants.isEmpty());
    }

    @Test
    public void divNormal() {
        YasmSelRenderer.RenderResult converted = RenderTestSupport.renderExpression("div(req_5xx_dmmm, normal())");
        String expected = "delta_to_rate({signal='req_5xx_dmmm'})";
        assertEquals(expected, converted.expression);
        assertTrue(converted.constants.isEmpty());
    }

}
