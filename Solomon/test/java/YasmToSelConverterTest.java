package ru.yandex.solomon.yasm.expression;

import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.solomon.yasm.expression.grammar.YasmToSelConverter;

@ParametersAreNonnullByDefault
public class YasmToSelConverterTest {
    private YasmToSelConverter converter;

    @Before
    public void setUp() {
        converter = new YasmToSelConverter("yasm_");
    }

    @Test
    public void basic() {
        String expr = "quant(unistat-handlers.push_signals.response_time_hgram,95)";
        String selExpr = converter.convertExpression(
                expr,
                Map.of(
                        "itype", List.of("yasmtsdb"),
                        "hosts", List.of("ASEARCH")
                        ),
                "hosts: ASEARCH; signals=" + expr,
                false
        );
        String expected = "// quant(unistat-handlers.push_signals.response_time_hgram,95)\n" +
                "alias(histogram_percentile(95, '', {project='yasm_yasmtsdb', hosts='ASEARCH', " +
                "signal='unistat-handlers.push_signals.response_time_hgram'}), " +
                "\"hosts: ASEARCH; signals=quant(unistat-handlers.push_signals.response_time_hgram,95)\")";

        Assert.assertEquals(expected, selExpr);
    }

    @Test
    public void normalize() {
        String expr = "hsum(unistat-handlers.push_signals.response_time_hgram)";
        String selExpr = converter.convertExpression(
                expr,
                Map.of(
                        "itype", List.of("yasmtsdb"),
                        "hosts", List.of("ASEARCH")
                ),
                "hosts: ASEARCH; signals=" + expr,
                true
        );
        String expected = "// hsum(unistat-handlers.push_signals.response_time_hgram)\n" +
                "alias(delta_to_rate(histogram_sum('', {project='yasm_yasmtsdb', hosts='ASEARCH', " +
                "signal='unistat-handlers.push_signals.response_time_hgram'})), " +
                "\"hosts: ASEARCH; signals=hsum(unistat-handlers.push_signals.response_time_hgram)\")";

        Assert.assertEquals(expected, selExpr);
    }

    @Test
    public void manualNormalize() {
        String expr = "div(hsum(unistat-handlers.push_signals.response_time_hgram), normal())";
        String selExpr = converter.convertExpression(
                expr,
                Map.of(
                        "itype", List.of("yasmtsdb"),
                        "hosts", List.of("ASEARCH")
                ),
                "hosts: ASEARCH; signals=" + expr,
                false
        );
        String expected = "// div(hsum(unistat-handlers.push_signals.response_time_hgram), normal())\n" +
                "alias(delta_to_rate(histogram_sum('', {project='yasm_yasmtsdb', hosts='ASEARCH', " +
                "signal='unistat-handlers.push_signals.response_time_hgram'})), " +
                "\"hosts: ASEARCH; signals=div(hsum(unistat-handlers.push_signals.response_time_hgram), normal())\")";

        Assert.assertEquals(expected, selExpr);
    }

    @Test
    public void normalUsage() {
        String expr = "mul(unistat-whatever, normal())";
        String selExpr = converter.convertExpression(
                expr,
                Map.of(
                        "itype", List.of("yasmtsdb"),
                        "hosts", List.of("ASEARCH")
                ),
                "hosts: ASEARCH; signals=" + expr,
                false
        );
        String expected = "// mul(unistat-whatever, normal())\n" +
                "alias({project='yasm_yasmtsdb', hosts='ASEARCH', signal='unistat-whatever'} * grid_step(), \"hosts: ASEARCH; signals=mul(unistat-whatever, normal())\")";

        Assert.assertEquals(expected, selExpr);
    }
}
