package ru.yandex.solomon.yasm.expression;

import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import ru.yandex.solomon.yasm.expression.grammar.YasmSelRenderer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class YasmSelRendererTest {

    @Test
    public void signal() {
        YasmSelRenderer.RenderResult converted = RenderTestSupport.renderExpression("saas_unistat-frontend-fueling-count_attx");
        String expected = "{signal='saas_unistat-frontend-fueling-count_attx'}";
        assertEquals(expected, converted.expression);
        assertTrue(converted.constants.isEmpty());
    }

    @Test
    public void simpleTags() {
        String using = RenderTestSupport.renderTags(Map.of("itype", "rtyserver"));
        assertEquals("{project='yasm_rtyserver'}", using);
    }

    @Test
    public void manyTags() {
        String using = RenderTestSupport.renderTags(Map.of("itype", "fusion", "ctype", "prod", "prj", "web-refresh-10day"));
        assertEquals("{project='yasm_fusion', ctype='prod', prj='web-refresh-10day'}", using);
    }

    @Test
    public void divToNormal() {
        var yasm = "div(or(balancer_report-report-service_total-outgoing_1xx_summ,balancer_report-report-service_total-1xx-externalunknown_summ),normal())";
        var converted = RenderTestSupport.renderExpression(yasm);
        String expected = "delta_to_rate(fallback({signal='balancer_report-report-service_total-outgoing_1xx_summ'}, {signal='balancer_report-report-service_total-1xx-externalunknown_summ'}))";
        assertEquals(expected, converted.expression);
        assertTrue(converted.constants.isEmpty());
    }

    @Test
    public void manySignals() {
        var yasm = Map.of("itype", "yasmsrv", "hosts", "ASEARCH", "signal", "unistat-*_ammm", "prj", "yasm|yasm-lines");
        var converted = RenderTestSupport.renderTags(yasm);
        assertEquals("series_sum('signal', {project='yasm_yasmsrv', hosts='ASEARCH', prj='yasm|yasm-lines', signal='unistat-*_ammm'})", converted);
    }

    @Test
    public void manyProjects() {
        var yasm = Map.of("itype", "yasmsrv", "hosts", "ASEARCH", "signal", "counter-instance_tmmv", "prj", "yasm|yasm-lines");
        var converted = RenderTestSupport.renderTags(yasm);
        assertEquals("series_sum('signal', {project='yasm_yasmsrv', hosts='ASEARCH', prj='yasm|yasm-lines', signal='counter-instance_tmmv'})", converted);
    }

    @Test
    public void hgramManyProjects(){
        var yasm = Map.of("itype", "yasmsrv", "hosts", "ASEARCH", "signal", "counter-instance_hgram", "prj", "yasm|yasm-lines");
        var converted = RenderTestSupport.renderTags(yasm);
        assertEquals("series_sum('signal', {project='yasm_yasmsrv', hosts='ASEARCH', prj='yasm|yasm-lines', signal='counter-instance_hgram'})", converted);
    }
}
