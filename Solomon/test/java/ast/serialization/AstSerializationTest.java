package ru.yandex.solomon.expression.ast.serialization;

import java.util.List;
import java.util.Random;
import java.util.function.Function;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.solomon.expression.SelParser;
import ru.yandex.solomon.expression.ast.Ast;
import ru.yandex.solomon.expression.ast.AstStatement;

/**
 * @author Ivan Tsybulin
 */
public class AstSerializationTest {
    private AstMappingContext mapper;

    private final int REPEAT_COUNT = 1000;

    @Before
    public void setUp() {
        mapper = new AstMappingContext(true);
    }

    private <T extends Ast> void testExpr(Function<Random, T> astGenerator) {
        for (int seed = 0; seed < REPEAT_COUNT; seed++) {
            T ast = astGenerator.apply(new Random(seed));
            String message = "For seed = " + seed;
            ObjectNode json = mapper.render(ast);
            Ast parsed = mapper.parse(json);
            Assert.assertEquals(message, parsed, ast);
            String rendered = mapper.renderToString(ast);
            Ast reparsed = new SelParser(rendered, false).parseExpr();
            Assert.assertEquals(message, reparsed, ast.stripRanges());
        }
    }

    private <T extends AstStatement> void testStatement(Function<Random, T> astGenerator) {
        for (int seed = 0; seed < REPEAT_COUNT; seed++) {
            T ast = astGenerator.apply(new Random(seed));
            String message = "For seed = " + seed;
            ObjectNode json = mapper.render(ast);
            AstStatement parsed = mapper.parse(json);
            Assert.assertEquals(message, parsed, ast);
            String rendered = mapper.renderToString(ast);
            List<AstStatement> reparsed = new SelParser(rendered, false).parseBlock();
            Assert.assertEquals(message, 1, reparsed.size());
            Assert.assertEquals(message, reparsed.get(0), ast.stripRanges());
        }
    }

    @Test
    public void testAstValue() {
        testExpr(RandomAst::randomAstValue);
    }

    @Test
    public void testAstAnon() {
        testStatement(RandomAst::randomAstAnon);
    }

    @Test
    public void testAstAssign() {
        testStatement(RandomAst::randomAstAssign);
    }

    @Test
    public void testAstUse() {
        testStatement(RandomAst::randomAstUse);
    }

    @Test
    public void testAstExpr() {
        testExpr(RandomAst::randomExpr);
    }
}
