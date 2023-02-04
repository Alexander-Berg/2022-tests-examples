package ru.yandex.solomon.expression;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import ru.yandex.solomon.expression.ast.Ast;
import ru.yandex.solomon.expression.ast.AstAnonymous;
import ru.yandex.solomon.expression.ast.AstAssignment;
import ru.yandex.solomon.expression.ast.AstBinOp;
import ru.yandex.solomon.expression.ast.AstCall;
import ru.yandex.solomon.expression.ast.AstIdent;
import ru.yandex.solomon.expression.ast.AstInterpolatedString;
import ru.yandex.solomon.expression.ast.AstLambda;
import ru.yandex.solomon.expression.ast.AstObject;
import ru.yandex.solomon.expression.ast.AstOp;
import ru.yandex.solomon.expression.ast.AstSelector;
import ru.yandex.solomon.expression.ast.AstSelectors;
import ru.yandex.solomon.expression.ast.AstTernaryOp;
import ru.yandex.solomon.expression.ast.AstUnaryOp;
import ru.yandex.solomon.expression.ast.AstUse;
import ru.yandex.solomon.expression.ast.AstValue;
import ru.yandex.solomon.expression.ast.AstValueDouble;
import ru.yandex.solomon.expression.ast.AstValueDuration;
import ru.yandex.solomon.expression.ast.AstValueString;
import ru.yandex.solomon.expression.ast.serialization.AstMappingContext;
import ru.yandex.solomon.expression.exceptions.ParserException;
import ru.yandex.solomon.labels.InterpolatedString;
import ru.yandex.solomon.labels.query.Selector;
import ru.yandex.solomon.labels.query.SelectorType;
import ru.yandex.solomon.labels.query.Selectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.fail;
import static ru.yandex.solomon.expression.PositionRange.UNKNOWN;

/**
 * @author Stepan Koltsov
 */
public class SelParserTest {

    private void impl(String string, Ast expected, boolean astRangesEnabled) {
        System.out.println(string);
        if (string.length() > 20) {
            System.out.print("         ");
            for (int i = 9; i < string.length(); i++) {
                System.out.print(Character.isWhitespace(string.charAt(i)) ? ' ' : Integer.toString(((i + 1) / 10) % 10));
            }
            System.out.println();
        }
        for (int i = 0; i < string.length(); i++) {
            System.out.print(Character.isWhitespace(string.charAt(i)) ? ' ' : Integer.toString((i + 1) % 10));
        }
        System.out.println("\n");
        Ast actual = parse(string, astRangesEnabled);
        assertThat(actual, equalTo(expected));
    }

    private void impl(String string, Ast expected) {
        impl(string, expected, true);
    }

    private Ast parse(String string, boolean astRangesEnabled) {
        return new SelParser(string, astRangesEnabled).parseExpr();
    }

    private PositionRange range(int cBeg, int cEnd) {
        return PositionRange.of(cBeg, cEnd);
    }

    private AstIdent id(String val, int cBeg, int cEnd) {
        return new AstIdent(range(cBeg, cEnd), val);
    }

    private AstOp op(String op, int cBeg, int cEnd) {
        return new AstOp(range(cBeg, cEnd), op);
    }

    private AstValueString str(String val, int cBeg, int cEnd) {
        return new AstValueString(range(cBeg, cEnd), val);
    }

    private AstValueDouble num(double val, int cBeg, int cEnd) {
        return new AstValueDouble(range(cBeg, cEnd), val);
    }

    private AstValueDuration dur(Duration val, int cBeg, int cEnd) {
        return new AstValueDuration(range(cBeg, cEnd), val);
    }

    @Test
    public void ident() {
        impl("foo", id("foo", 1, 3));
    }

    @Test
    public void wrongPosInfo() {
        Ast actual = parse("foo", true);
        Ast unexpected1 = id("foo", 1, 42);
        Ast unexpected2 = id("foo", 2, 3);
        assertThat(actual, not(equalTo(unexpected1)));
        assertThat(actual, not(equalTo(unexpected2)));
    }

    @Test
    public void wrongPosInfoWhenDisabled() {
        Ast actual = parse("foo", false);
        Ast expected = new AstIdent(UNKNOWN, "foo");
        assertThat(actual, equalTo(expected));
    }


    @Test
    public void call() {
        impl("foo()", new AstCall(range(1, 5), id("foo", 1, 3), List.of(
                )));
        impl("foo(bar)", new AstCall(range(1, 8), id("foo", 1, 3), List.of(
                new AstIdent(range(5, 7), "bar"))));
        impl("foo(bar, baz)", new AstCall(range(1, 13), id("foo", 1, 3), List.of(
                new AstIdent(range(5, 7), "bar"),
                new AstIdent(range(10, 12), "baz"))));
    }

    @Test
    public void binOp() {
        impl("foo / bar", new AstBinOp(
                range(1, 9),
                id("foo", 1, 3),
                id("bar", 7, 9),
                op("/", 5, 5)));
    }

    @Test
    public void binOpPrio() {
        impl("foo / bar + baz", new AstBinOp(
                range(1, 15),
                new AstBinOp(
                    range(1, 9),
                    id("foo", 1, 3),
                    id("bar", 7, 9),
                    op("/", 5, 5)),
                id("baz", 13, 15),
                op("+", 11, 11)));
        impl("foo + bar / baz", new AstBinOp(
                range(1, 15),
                id("foo", 1, 3),
                new AstBinOp(
                    range(7, 15),
                    id("bar", 7, 9),
                    id("baz", 13, 15),
                    op("/", 11, 11)),
                op("+", 5, 5)));
    }

    @Test
    public void parens() {
        impl("foo / (bar + baz)", new AstBinOp(
                range(1, 17),
                id("foo", 1, 3),
                new AstBinOp(
                    range(7, 17),
                    id("bar", 8, 10),
                    id("baz", 14, 16),
                    op("+", 12, 12)
                ),
                op("/", 5, 5)));
    }

    @Test
    public void lambda() {
        impl("p -> p + 1", new AstLambda(range(1, 10), List.of("p"),
                new AstBinOp(
                    range(6, 10),
                    id("p", 6, 6),
                    num(1, 10, 10),
                    op("+", 8, 8))));
    }

    @Test
    public void ternary() {
        impl("p ? a : b", new AstTernaryOp(
                range(1, 9),
                id("p", 1, 1),
                id("a", 5, 5),
                id("b", 9, 9)));

        impl("p + 1 ? a : b", new AstTernaryOp(
                range(1, 13),
                new AstBinOp(
                    range(1, 5),
                    id("p", 1, 1),
                    num(1, 5, 5),
                    op("+", 3, 3)),
                id("a", 9, 9),
                id("b", 13, 13)));

        impl("p > 1 ? a : b", new AstTernaryOp(
                range(1, 13),
                new AstBinOp(
                    range(1, 5),
                    id("p", 1, 1),
                    num(1, 5, 5),
                    op(">", 3, 3)),
                id("a", 9, 9),
                id("b", 13, 13)));


        impl("!p ? a : b", new AstTernaryOp(
                range(1, 10),
                new AstUnaryOp(
                    range(1, 2),
                    id("p", 2, 2),
                    op("!", 1, 1)),
                id("a", 6, 6),
                id("b", 10, 10)));

        impl("a || b ? a : b", new AstTernaryOp(
                range(1, 14),
                new AstBinOp(
                    range(1, 6),
                    id("a", 1, 1),
                    id("b", 6, 6),
                    op("||", 3, 4)),
                id("a", 10, 10),
                id("b", 14, 14)));

        impl("a && b || c ? a : b", new AstTernaryOp(
                range(1, 19),
                new AstBinOp(
                    range(1, 11),
                    new AstBinOp(
                        range(1, 6),
                        id("a", 1, 1),
                        id("b", 6, 6),
                        op("&&", 3, 4)),
                    id("c", 11, 11),
                    op("||", 8, 9)),
                id("a", 15, 15),
                id("b", 19, 19))
            );
    }

    @Test
    public void selectors() throws Exception {
        AstSelectors expected = new AstSelectors(UNKNOWN, "", Arrays.asList(
                new AstSelector(UNKNOWN, literal("sensor"), literal("response_time"), SelectorType.GLOB),
                new AstSelector(UNKNOWN, literal("project"), literal("yt"), SelectorType.GLOB),
                new AstSelector(UNKNOWN, literal("bin"), literal("*"), SelectorType.GLOB),
                new AstSelector(UNKNOWN, literal("user"), literal("test"), SelectorType.GLOB)
        ));

        String[] aliases = {
                "{sensor='response_time', project='yt', bin='*', user='test'}",
                "{\"sensor\"=\"response_time\", \"project\"=\"yt\", \"bin\"=\"*\", \"user\"=\"test\"}",
        };

        for (String alias: aliases) {
            impl(alias, expected, false);
        }
    }

    @Test
    public void selectorsWithNamePositionRange() {
        //                       11111111112 22 222 22233333333 344444444445  5555555 566666666667
        //              12345678901234567890 12 345 78901234567 901234567890  2345678 901234567890
        String query = "flatten({foo=x,bar==\"2\"}, foo{bar=y}, 'foo.bar'{}, \"foobar\"{baz=quux})";

        impl(query, new AstCall(
                range(1, 70),
                id("flatten", 1, 7),
                List.of(
                    new AstSelectors(range(9, 24), "", List.of(
                        new AstSelector(range(10, 14), literal("foo", 10, 12), literal("x", 14, 14), SelectorType.GLOB),
                        new AstSelector(range(16, 23), literal("bar", 16, 18), literal("2", 21, 23), SelectorType.EXACT)
                    )),
                    new AstSelectors(range(27, 36), "foo", List.of(
                        new AstSelector(range(31, 35), literal("bar", 31, 33), literal("y", 35, 35), SelectorType.GLOB)
                    )),
                    new AstSelectors(range(39, 49), "foo.bar", List.of()),
                    new AstSelectors(range(52, 69), "foobar", List.of(
                        new AstSelector(range(61, 68), literal("baz", 61, 63), literal("quux", 65, 68), SelectorType.GLOB)
                    ))
                )
            ));
    }

    @Test
    public void selectorsWithName() throws Exception {
        AstSelectors expected = new AstSelectors(UNKNOWN, "module.response_time", List.of());

        String[] aliases = {
                "module.response_time{}",
                "'module.response_time'{}",
                "\"module.response_time\"{}",
        };

        for (String alias: aliases) {
            impl(alias, expected, false);
        }
    }

    @Test
    public void selectorsStringRepresentSame() {
        for (SelectorType type : SelectorType.values()) {
            if (type == SelectorType.ANY || type == SelectorType.ABSENT)  {
                continue;
            }

            Selector selector = type.create("a", "b");
            int opLen = type.getOperator().length();
            int astLen = 1 + 1 + opLen + 3 + 1;
            AstSelectors expectedAst = new AstSelectors(range(1, astLen), "",
                List.of(new AstSelector(
                    range(2, 5 + opLen),
                    literal(selector.getKey(), 2, 2),
                    literal(selector.getValue(), 3 + opLen, 5 + opLen),
                    selector.getType())));
            String str = Selectors.format(Selectors.of(selector));

            try {
                impl(str, expectedAst);
            } catch (Throwable e) {
                throw new RuntimeException("Failed parse selector: " + str, e);
            }
        }
    }

    @Test
    public void aliasOverLabelGroup() throws Exception {
        String expr = "group_lines('sum', 'host', {sensor=test})";
        String[] aliases = {
                "sum({sensor=test}) by host",
                "sum({sensor='test'}) by 'host'",
                "sum({sensor=\"test\"}) by \"host\"",
                "sum({sensor=test}) by (host)",
                "sum({sensor=test}) by ('host')",
                "sum({sensor=test}) by (\"host\")",
        };

        assertThatSameAst(aliases, expr);
    }

    @Test
    public void aliasOverMultiLabelGroup() throws Exception {
        String expr = "group_lines('max', as_vector('host', 'group'), {sensor='test'})";
        String[] aliases = {
                "max({sensor='test'}) by (host, group)",
                "max({sensor=test}) by ('host', 'group')",
                "max({sensor=test}) by (\"host\", \"group\")",
        };

        assertThatSameAst(aliases, expr);
    }

    @Test
    public void aliasOverTimeGroup() throws Exception {
        String expr = "group_by_time(1h, 'avg', {host='*'})";
        String[] aliases = {
                "avg({host='*'}) by 1h",
                "avg({host=\"*\"}) by 1h",
        };

        assertThatSameAst(aliases, expr);
    }

    @Test
    public void aliasOverTimeAndOverLineGroup() throws Exception {
        String expr = "group_lines('sum', 'host', group_by_time(30m, 'sum', RxBytes{}))";

        String[] aliases = {
                "group_lines('sum', 'host', group_by_time(30m, 'sum', RxBytes{}))",
                "group_lines('sum', 'host', sum(RxBytes{}) by 30m)",
                "sum(sum(RxBytes{}) by 30m) by host",
                "sum(sum(RxBytes{}) by 30m) by 'host'",
                "sum(sum(RxBytes{}) by 30m) by \'host\'",
                "sum(sum(RxBytes{}) by 30m) by (host)",
                "sum(sum(RxBytes{}) by 30m) by host",
                "sum(group_by_time(30m, 'sum', RxBytes{})) by host",
        };

        assertThatSameAst(aliases, expr);
    }

    private void assertThatSameAst(String[] aliases, String expect) {
        Ast expectAst = parse(expect, false);
        for (String alias : aliases) {
            Ast result = parse(alias, false);
            assertThat(alias + " == " + expect, result, equalTo(expectAst));
        }
    }

    @Test
    public void parseAstSelectorOperators() {
        String expr = "{sensor=='requestLatency', bin=~'\\d+', method='GET|POST', endpoint='{{externalVar}}', host!='Man|Myt'}";
        //                      11111111112222222 2233333 3333 444444444455555555 566666666667777777777888888 8889999999999000
        //             12345678901234567890123456 8901234 5678 012345678901234567 901234567890123456789012345 7890123456789012
        AstSelectors expected = new AstSelectors(range(1, 102), "", Arrays.asList(
                new AstSelector(range(2, 25), literal("sensor", 2, 7), literal("requestLatency", 10, 25), SelectorType.EXACT),
                new AstSelector(range(28, 37), literal("bin", 28, 30), literal("\\d+", 33, 37), SelectorType.REGEX),
                new AstSelector(range(40, 56), literal("method", 40, 45), literal("GET|POST", 47, 56), SelectorType.GLOB),
                new AstSelector(range(59, 84), literal("endpoint", 59, 66), literal("{{externalVar}}", 68, 84), SelectorType.GLOB),
                new AstSelector(range(87, 101), literal("host", 87, 90), literal("Man|Myt", 93, 101), SelectorType.NOT_GLOB)
        ));
        impl(expr, expected);
    }

    private AstValue literal(String value, PositionRange pos) {
        if (InterpolatedString.isInterpolatedString(value)) {
            return new AstInterpolatedString(pos, InterpolatedString.parse(value));
        }
        return new AstValueString(pos, value);
    }

    private Ast literal(String value, int cBeg, int cEnd) {
        return literal(value, range(cBeg, cEnd));
    }

    private Ast literal(String value) {
        return literal(value, UNKNOWN);
    }

    @Test
    public void parseEmptyAstObject() {
        String expr = "{}";
        AstObject expected = new AstObject(range(1, 2), Collections.emptyMap());
        impl(expr, expected);
    }

    @Test
    public void parseAstObject() {
        String expr = "{ scalar: 1, bool: true, str: 'string', duration: 1h, graphData: {sensor=requests} }";
        //                       11 11111 22222 2222 333333333 444444444 555 5555566666 66667777777777888 8
        //             1 3456789 12 45678 01234 6789 123456789 123456789 123 5678901234 67890123456789012 4

        Map<String, Ast> expectedObject = new HashMap<>();
        expectedObject.put("scalar", num(1, 11, 11));
        expectedObject.put("bool", id("true", 20, 23));
        expectedObject.put("str", str("string", 31, 38));
        expectedObject.put("duration", dur(Duration.ofHours(1), 51, 52));
        expectedObject.put("graphData", new AstSelectors(range(66, 82), "", Collections.singletonList(
            new AstSelector(
                range(67, 81),
                str("sensor", 67, 72),
                str("requests", 74, 81),
                SelectorType.GLOB)
        )));

        AstObject expected = new AstObject(range(1, 84), expectedObject);

        impl(expr, expected);
    }

    @Test
    public void programWithReturn() {
        String codeFullForm = ""
            + "let foo = avg({project='yt'}) by 5m;"
            + "let bar = group_lines('sum', foo);"
            + "return last(bar);";
        String codeShortForm = ""
            + "let foo = avg({project='yt'}) by 5m;"
            + "let bar = group_lines('sum', foo);"
            + "last(bar)";

        var programFullForm = new SelParser(codeFullForm).parseProgramWithReturn();
        var programShortForm = new SelParser(codeShortForm).parseProgramWithReturn();

        assertThat(programFullForm.getExpr().stripRanges(), equalTo(programShortForm.getExpr().stripRanges()));
    }

    @Test(expected = ParserException.class)
    public void programNoReturn() {
        String code = ""
            + "let foo = avg({project='yt'}) by 5m;"
            + "let bar = group_lines('sum', foo);";

        new SelParser(code).parseProgramWithReturn();
        fail("Parsing must fail");
    }

    @Test
    public void programWithUse() {
        String code = ""
            + "use {host=='solomon-alerting-00', service=alerting, cluster!=\"production\"};\n"
            + "let data = group_lines('sum', {sensor=uptimeMillis});\n"
            + "alarm_if(avg(data) > 42);";

        var parsed = new SelParser(code).parseBlock();
        assertThat(parsed.get(0), instanceOf(AstUse.class));
        assertThat(parsed.get(1), instanceOf(AstAssignment.class));
        assertThat(parsed.get(2), instanceOf(AstAnonymous.class));
    }

    @Test
    public void programWithReturnAndUse() {
        String code = ""
                + "use {host=='solomon-alerting-00', service=alerting, cluster!=\"production\"};\n"
                + "let data = group_lines('sum', {sensor=uptimeMillis});\n"
                + "return data;";

        var parsed = new SelParser(code).parseProgramWithReturn().getStatements();
        assertThat(parsed.get(0), instanceOf(AstUse.class));
        assertThat(parsed.get(1), instanceOf(AstAssignment.class));
    }

    @Test
    public void astWrapper() {
        String expr = "'foo' + ('bar' + 'baz')";

        var expected = new AstBinOp(
            range(1, 23),
            str("foo", 1, 5),
            new AstBinOp(
                range(9, 23),
                str("bar", 10, 14),
                str("baz", 18, 22),
                new AstOp(range(16, 16), "+")
            ),
            new AstOp(range(7, 7), "+")
        );

        impl(expr, expected);
        assertThat(expected.getRange(), equalTo(range(1, expr.length())));
    }

    @Test
    public void astSelectorsParsed() {
        String expected = "series_sum({'project'='solomon', 'host'='001'})";
        Ast parse = parse(expected, false);
        AstMappingContext mappingContext = new AstMappingContext(false);
        String actual = mappingContext.renderToString(parse);
        assertThat(actual, equalTo(expected));
    }
}
