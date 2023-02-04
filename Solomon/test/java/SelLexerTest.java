package ru.yandex.solomon.expression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.solomon.expression.SelLexer.TokenType.DOUBLE;
import static ru.yandex.solomon.expression.SelLexer.TokenType.DURATION;
import static ru.yandex.solomon.expression.SelLexer.TokenType.IDENT;
import static ru.yandex.solomon.expression.SelLexer.TokenType.KEYWORD;
import static ru.yandex.solomon.expression.SelLexer.TokenType.PUNCT;
import static ru.yandex.solomon.expression.SelLexer.TokenType.STRING;
import static ru.yandex.solomon.expression.SelLexer.TokenType.UNRECOGNIZED;

/**
 * @author Stepan Koltsov
 */
public class SelLexerTest {

    private void impl(String string, SelLexer.ScannedToken... expectedTokens) {
        List<SelLexer.ScannedToken> actual = new SelLexer(string).nextTokens();
        Assert.assertEquals(new ArrayList<>(Arrays.asList(expectedTokens)), actual);
    }

    private static class ScannedTokenMatcher extends TypeSafeMatcher<SelLexer.ScannedToken> {
        private final SelLexer.Token pattern;

        public ScannedTokenMatcher(SelLexer.Token pattern) {
            this.pattern = pattern;
        }

        @Override
        protected boolean matchesSafely(SelLexer.ScannedToken item) {
            return item.matches(pattern);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("like " + pattern);
        }
    }

    private void impl(String string, SelLexer.Token... expectedTokens) {
        List<SelLexer.ScannedToken> actual = new SelLexer(string).nextTokens();
        ScannedTokenMatcher[] matchers = Arrays.stream(expectedTokens).map(ScannedTokenMatcher::new).toArray(ScannedTokenMatcher[]::new);
        assertThat(actual, contains(matchers));
    }

    private void impl1(String string, SelLexer.TokenType tokenType) {
        impl(string, tokenType.of(string));
    }

    @Test
    public void empty() {
        impl("");
    }

    @Test
    public void whitespace() {
        impl("   \t\n");
    }

    @Test
    public void punct() {
        impl1("+", PUNCT);
        impl1(">", PUNCT);
        impl1("<=", PUNCT);
        impl1("-", PUNCT);
        impl1("?", PUNCT);
        impl1(":", PUNCT);
    }

    @Test
    public void ident() {
        impl("foo", IDENT.of("foo"));
    }

    @Test
    public void doubles() {
        impl1("10", DOUBLE);
        impl1("10.2", DOUBLE);
        impl("-10.2", PUNCT.of("-"), DOUBLE.of("10.2"));
    }

    @Test
    public void duration() throws Exception {
        impl1("1d30m", DURATION);
        impl1("15m", DURATION);
        impl1("1w2d4h30m10s", DURATION);
        impl("-12m", PUNCT.of("-"), DURATION.of("12m"));
        impl("250ms", DURATION.of("250ms"));
    }

    @Test
    public void strings() {
        // simple quotes
        impl("'qwerty'", STRING.of("qwerty"));
        impl("\"qwerty\"", STRING.of("qwerty"));

        // doble in single and single in double are OK
        impl("'\"'", STRING.of("\""));
        impl("\"'\"", STRING.of("'"));

        // backslash escapes
        impl("'x\\x'", STRING.of("x\\x"));
        impl("\"x\\x\"", STRING.of("x\\x"));
        impl("'x\"x'", STRING.of("x\"x"));
        impl("'x\\'x'", STRING.of("x'x"));
        impl("'x\nx'", STRING.of("x\nx"));

        // These tests verify that \ is parsed as is, ignoring \\ as escape sequence
        // One day this may change

        // Here last \ escapes ' resulting in unterminated literal
        impl("'\\\\'", UNRECOGNIZED.of("'\\\\'"));
        impl("'\\\\''", STRING.of("\\'"));
        impl("'\\n'", STRING.of("\\n"));

        // EOF after \ does not cause trouble
        impl("'\\", UNRECOGNIZED.of("'\\"));
    }

    @Test
    public void multipliers() {
        impl1("100k", DOUBLE);
        impl1("500G", DOUBLE);
    }

    @Test
    public void complex() {
        impl("foo(bar, baz)",
                IDENT.of("foo"),
                PUNCT.of("("),
                IDENT.of("bar"),
                PUNCT.of(","),
                IDENT.of("baz"),
                PUNCT.of(")"));
    }

    @Test
    public void keywords() {
        impl("let a", KEYWORD.of("let"), IDENT.of("a"));
        impl("leta", IDENT.of("leta"));
    }

    @Test
    public void selector() throws Exception {
        SelLexer.Token[] expects = {
                IDENT.of("metabase_response_time"),
                PUNCT.of("{"),
                IDENT.of("project"), PUNCT.of("="), STRING.of("solomon"),
                PUNCT.of(","),
                IDENT.of("service"), PUNCT.of("="), STRING.of("alerting"),
                PUNCT.of(","),
                IDENT.of("cluster"), PUNCT.of("="), STRING.of("stp-man"),
                PUNCT.of(","),
                IDENT.of("bin"), PUNCT.of("="), STRING.of("*"),
                PUNCT.of("}"),
        };

        impl("metabase_response_time{project='solomon', service='alerting', cluster='stp-man', bin='*'}", expects);
    }

    @Test
    public void selectorQuotedLabelNames() throws Exception {
        SelLexer.Token[] expects = {
                PUNCT.of("{"),
                STRING.of("name"), PUNCT.of("="), STRING.of("metabase_response_time"),
                PUNCT.of(","),
                STRING.of("service"), PUNCT.of("="), STRING.of("alerting"),
                PUNCT.of("}"),
        };

        impl("{\"name\"=\"metabase_response_time\", \"service\"=\"alerting\"}", expects);
    }

    @Test
    public void overLineGroup() throws Exception {
        SelLexer.Token[] expects = {
                IDENT.of("avg"),
                PUNCT.of("("),
                IDENT.of("dataSet"),
                PUNCT.of(")"),
                KEYWORD.of("by"),
                IDENT.of("host")
        };

        impl("avg(dataSet) by host", expects);
    }

    @Test
    public void overTimeGroup() throws Exception {
        SelLexer.Token[] expects = {
                IDENT.of("avg"),
                PUNCT.of("("),
                IDENT.of("dataSet"),
                PUNCT.of(")"),
                KEYWORD.of("by"),
                DURATION.of("10m")
        };

        impl("avg(dataSet) by 10m", expects);
    }

    @Test
    public void ignoreEndOfLineComments() {
        SelLexer.Token[] expects = {
                KEYWORD.of("let"),
                IDENT.of("a"),
                PUNCT.of("="),
                DOUBLE.of("42"),
                PUNCT.of(";")
        };

        impl("let a = 42; // it's true", expects);
        impl("let a = 42; //it's true", expects);
        impl("let a = 42;// it's true", expects);
        impl("let a = 42;\n// it's true", expects);
    }

    @Test
    public void ignoreSingleLineComment() {
        impl("// my comment");
        impl("//my comment");
        impl("//let a = 42;");
        impl("// my comment\n// another comment\n");
    }

    @Test
    public void unclosedStringLiteral() {
        impl("let message = 'hello world",
                KEYWORD.of("let"),
                IDENT.of("message"),
                PUNCT.of("="),
                UNRECOGNIZED.of("'hello world"));
    }

    @Test
    public void ignoreOnlyCommentedLine() {
        SelLexer.Token[] expects = {
                KEYWORD.of("let"),
                IDENT.of("mySelector"),
                PUNCT.of("="),
                PUNCT.of("{"),
                STRING.of("project"), PUNCT.of("="), STRING.of("solomon"),
                PUNCT.of(","),
                STRING.of("cluster"), PUNCT.of("="), STRING.of("test"),
                STRING.of("a"), PUNCT.of("="), STRING.of("b"),
                PUNCT.of("}"),
                PUNCT.of(";"),
        };

        StringBuilder source = new StringBuilder();
        source.append("let mySelector = {\n")
               .append("'project' = 'solomon',\n")
               .append("'cluster' = 'test'\n")
               .append("'a' = 'b'\n")
               .append("//a = 'c'\n")
               .append("};\n");

        impl(source.toString(), expects);
    }

    @Test
    public void junk() {
        impl("'1234", UNRECOGNIZED.of("'1234").at(PositionRange.of(1, 5)));
        impl("let .code = 1;\n",
                KEYWORD.of("let").at(PositionRange.of(1, 3)),
                UNRECOGNIZED.of(".code = 1;\n").at(PositionRange.of(5, 15)));
    }

    @Test
    public void singleLinePositions() {
        impl("let foo = 1;",
                KEYWORD.of("let").at(PositionRange.of(1, 3)),
                IDENT.of("foo").at(PositionRange.of(5, 7)),
                PUNCT.of("=").at(PositionRange.of(9)),
                DOUBLE.of("1").at(PositionRange.of(11)),
                PUNCT.of(";").at(PositionRange.of(12)));
    }

    @Test
    public void multilineStringLiteral() {
        impl("'1234\n5678' 'hello world\n'",
                STRING.of("1234\n5678").at(PositionRange.of(1, 1, 0, 2, 5, 10)),
                STRING.of("hello world\n").at(PositionRange.of(2, 7, 12, 3, 1, 25)));
    }

    @Test
    public void multiLinePositions() {
        String program =
        //                 1111111 111222222222233333 333334444444 44 45 5555555556
        //       01234567890123456 789012345678901234 567890123456 78 90 1234567890
                "let foo = sensor{\n  host='cluster',\n  port='80'\n}\n;\navg(data)";
        SelLexer.ScannedToken[] tokens = new SelLexer.ScannedToken[] {
            KEYWORD.of("let").at(PositionRange.of(1, 3)),
            IDENT.of("foo").at(PositionRange.of(5, 7)),
            PUNCT.of("=").at(PositionRange.of(9)),
            IDENT.of("sensor").at(PositionRange.of(11, 16)),
            PUNCT.of("{").at(PositionRange.of(17)),
            IDENT.of("host").at(PositionRange.of(2, 3, 20, 6)),
            PUNCT.of("=").at(PositionRange.of(2, 7, 24, 7)),
            STRING.of("cluster").at(PositionRange.of(2, 8, 25, 16)),
            PUNCT.of(",").at(PositionRange.of(2, 17, 34, 17)),
            IDENT.of("port").at(PositionRange.of(3, 3, 38,6)),
            PUNCT.of("=").at(PositionRange.of(3, 7, 42, 7)),
            STRING.of("80").at(PositionRange.of(3, 8, 43, 11)),
            PUNCT.of("}").at(PositionRange.of(4, 1, 48, 1)),
            PUNCT.of(";").at(PositionRange.of(5, 1, 50, 1)),
            IDENT.of("avg").at(PositionRange.of(6, 1, 52, 3)),
            PUNCT.of("(").at(PositionRange.of(6, 4, 55, 4)),
            IDENT.of("data").at(PositionRange.of(6, 5, 56, 8)),
            PUNCT.of(")").at(PositionRange.of(6, 9, 60, 9))
        };
        impl(program, tokens);
        for (var token : tokens) {
            if (token.getTokenType() == STRING) { // strings are quoted
                continue;
            }
            Position tokenBegin = token.getRange(true).getBegin();
            Position tokenEnd = token.getRange(true).getEnd();
            assertThat(token.getValue(), equalTo(program.substring(tokenBegin.getOffset(), tokenEnd.getOffset() + 1)));
        }
    }
}
