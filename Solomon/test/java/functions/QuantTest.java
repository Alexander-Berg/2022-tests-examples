package ru.yandex.solomon.yasm.expression.functions;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.yasm.expression.RenderTestSupport;
import ru.yandex.solomon.yasm.expression.grammar.YasmSelRenderer;

/**
 * @author Ivan Tsybulin
 */
public class QuantTest {
    private static String convert(String expression) {
        YasmSelRenderer.RenderResult converted = RenderTestSupport.renderExpression(expression);
        return converted.expression;
    }

    private static void test(String yasmLevel, String solomonLevel) {
        String converted;
        if (yasmLevel.isEmpty()) {
            converted = convert("quant(wizard_eventlog-wizard_time_hgram)");
        } else {
            converted = convert("quant(wizard_eventlog-wizard_time_hgram, " + yasmLevel + ")");
        }
        String expected = "histogram_percentile(" + solomonLevel + ", '', {signal='wizard_eventlog-wizard_time_hgram'})";
        Assert.assertEquals(expected, converted);
    }

    @Test
    public void translateLevel() {
        test("0.95", "95");
        test("95", "95");
        test("999", "99.9");
        test("8", "80");
        test("08", "8");
        test("100", "10");
        test("0.01", "1");
        test("90", "90");
        test("min", "0");
        test("max", "100");
        test("med", "50");
        test("", "50");
    }
}
