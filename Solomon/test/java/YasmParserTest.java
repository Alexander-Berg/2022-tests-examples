package ru.yandex.solomon.yasm.expression;

import org.junit.Test;

import ru.yandex.solomon.yasm.expression.ast.YasmAst;
import ru.yandex.solomon.yasm.expression.ast.YasmAstArgList;
import ru.yandex.solomon.yasm.expression.ast.YasmAstCall;
import ru.yandex.solomon.yasm.expression.ast.YasmAstIdent;
import ru.yandex.solomon.yasm.expression.ast.YasmAstNumber;
import ru.yandex.solomon.yasm.expression.grammar.YasmExpression;

import static org.junit.Assert.assertEquals;

/**
 * @author Ivan Tsybulin
 */
public class YasmParserTest {

    private static YasmAst parse(String code) {
        return YasmExpression.parse(code);
    }

    @Test
    public void signal() {
        var ast = parse("ahservant-template_errors.p_story_desktop_dmmm");
        assertEquals(new YasmAstIdent("ahservant-template_errors.p_story_desktop_dmmm"), ast);
    }

    @Test
    public void perc() {
        var ast = parse("perc(push-disk-used_bytes_pgdata_tmmx, push-disk-total_bytes_pgdata_tmmx)");
        var expectedAst = new YasmAstCall("perc", new YasmAstArgList(
                new YasmAstIdent("push-disk-used_bytes_pgdata_tmmx"),
                new YasmAstIdent("push-disk-total_bytes_pgdata_tmmx")
        ));
        assertEquals(expectedAst, ast);
    }

    @Test
    public void complex() {
        String program = "max(portoinst-max_rss_gb_txxx,conv(div(mul(hsum(portoinst-anon_usage_slot_hgram), 5), normal()), Gi))";
        var ast = parse(program);
        var expectedAst = new YasmAstCall("max", new YasmAstArgList(
                new YasmAstIdent("portoinst-max_rss_gb_txxx"),
                new YasmAstCall("conv", new YasmAstArgList(
                        new YasmAstCall("div", new YasmAstArgList(
                                new YasmAstCall("mul", new YasmAstArgList(
                                        new YasmAstCall("hsum", new YasmAstArgList(
                                                new YasmAstIdent("portoinst-anon_usage_slot_hgram")
                                        )),
                                        new YasmAstNumber("5")
                                )),
                                new YasmAstCall("normal", new YasmAstArgList())
                        )),
                        new YasmAstIdent("Gi")
                ))
        ));
        assertEquals(expectedAst, ast);
    }

}
