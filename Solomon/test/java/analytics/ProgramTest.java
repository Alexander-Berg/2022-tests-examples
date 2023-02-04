package ru.yandex.solomon.expression.analytics;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.solomon.expression.test.ForEachSelVersionRunner;
import ru.yandex.solomon.expression.test.VersionedSelTestBase;
import ru.yandex.solomon.expression.value.SelValue;
import ru.yandex.solomon.expression.version.SelVersion;
import ru.yandex.solomon.labels.query.Selectors;
import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.util.time.Interval;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * @author Maksim Leonov
 */
@RunWith(ForEachSelVersionRunner.class)
public class ProgramTest extends VersionedSelTestBase {
    public ProgramTest(SelVersion version) {
        super(version);
    }

    private Program compile(String source) {
        return Program.fromSource(version, source).compile();
    }

    private void checkProgramGood(String program) {
        Program p = compile(program);
        PreparedProgram preparedProgram = p.prepare(new Interval(Instant.MIN, Instant.MAX));
        preparedProgram.evaluate(GraphDataLoader.empty(), Map.of());
    }

    private void checkProgramBad(String program) {
        try {
            Program p = compile(program);
            Assert.fail("Program compiled successfully while expected not to.");
        } catch (Exception e) {
            // If parse has failed then the test has passed
        }
    }

    @Test
    public void checkSyntax() {
        checkProgramGood("");
        checkProgramGood("   ");
        checkProgramGood("let a = 0;");
        checkProgramGood("let a=0\n;");
        checkProgramGood("let a = 0; let b = 1;");
        checkProgramGood("    let a = 0;\n     \n   let b = 1;");
        checkProgramGood("let a = 0; let a = 1;");

        checkProgramBad(";"); // Malformed assignment statement
        checkProgramBad("a = 0;"); // No 'let' keyword
        checkProgramBad("leta = 0;");
        checkProgramBad("let a =;");
        checkProgramBad("let a = 0");
        checkProgramBad("let a = b;"); // Undefined variable
    }

    @Test
    public void testStaticLoadRequestDetection() {
        Assert.assertEquals(0, compile("let a = 3;").getProgramSelectors().size());
        Assert.assertEquals(1, compile("let a = {sensor=selectors};").getProgramSelectors().size());
        Assert.assertEquals(1, compile("let s = {sensor=selectors};").getProgramSelectors().size());
        Assert.assertEquals(2, compile("let a = {sensor=selectors};let b = {sensor=selectors2};").getProgramSelectors().size());
        Assert.assertEquals(2, compile("let a = {sensor=selectors};let c = a;let b = {sensor=selectors2};").getProgramSelectors().size());
    }

    @Test
    public void testExternalExpressionsSupport() {
        Program p = Program.fromSource(version, "let a = {sensor=selectors};")
                .withExternalExpressions(List.of("a", "{sensor=request}"))
                .compile();

        Assert.assertEquals(2, p.getProgramSelectors().size());
    }


    @Test
    public void testSelectorsSupport() {
        Map<String, String> selectors = Map.of("selector", "value", "pill", "poison");

        Program p = Program.fromSource(version, "let a = {sensor='request{{selector}}'};").withSelectors(selectors).compile();

        Assert.assertEquals(1, p.getProgramSelectors().size());

        Assert.assertEquals(Selectors.parse("sensor=requestvalue"), p.getProgramSelectors().iterator().next());
    }

    @Test
    public void parseGlob() {
        String source = "let a = requestCount{host='man|sas', endpoint!='/ok'};";
        Selectors expect = Selectors.parse("{sensor==requestCount, host=man|sas, endpoint!='/ok'}");
        assertThat(parseSelectors(source), equalTo(expect));
    }

    @Test
    public void parseExactMatch() {
        String source = "let a = requestCount{host==cluster, endpoint!=='/ok'};";
        Selectors expect = Selectors.parse("{sensor==requestCount, host==cluster, endpoint!=='/ok'}");
        assertThat(parseSelectors(source), equalTo(expect));
    }

    @Test
    public void parseAbsent() {
        String source = "let a = requestCount{host=-};";
        Selectors expect = Selectors.parse("{sensor==requestCount, host=-}");
        assertThat(parseSelectors(source), equalTo(expect));
    }

    @Test
    public void parseRegexp() {
        String source = "let a = requestLatency{host='*', bin=~'\\d+', endpoint!~'\\d+'};";
        Selectors expect = Selectors.parse("{sensor==requestLatency, host='*', bin=~'\\d+', endpoint!~'\\d+'}");
        assertThat(parseSelectors(source), equalTo(expect));
    }

    @Test
    public void selectorAndInterpolatedParam() {
        String source = "let a = requestLatency{host='{{host}}', endpoint=='myService/{{endpoint}}'};";

        Selectors parsed = parseSelectors(source, ImmutableMap.of("host", "cluster", "endpoint", "test"));
        Selectors expected = Selectors.parse("{sensor==requestLatency, host='cluster',  endpoint=='myService/test'}");
        assertThat(parsed, equalTo(expected));
    }

    @Test
    public void expressionSupportComments() {
        StringBuilder source = new StringBuilder();
        source.append("let a = 10;\n");
        source.append("let b = 5;\n");
        source.append("//let c = 30;\n");
        source.append("  // comment \n");
        source.append("let expr = a + 5; // its comment should be ignored");

        PreparedProgram program = compile(source.toString()).prepare(Interval.seconds(0, 100));
        Map<String, SelValue> result = program.evaluate(GraphDataLoader.empty(), Collections.emptyMap());

        assertThat(result, not(hasKey("c")));
        assertThat(result.get("a").castToScalar().getValue(), equalTo(10d));
        assertThat(result.get("b").castToScalar().getValue(), equalTo(5d));
        assertThat(result.get("expr").castToScalar().getValue(), equalTo(15d));
    }

    @Test
    public void expressionSupportAnonymous() {
        String source = "" +
                "let foo = sinusoid{};\n" +
                "2 + 2;\n" +
                "let bar = group_lines('sum', foo);\n";

        PreparedProgram program = compile(source).prepare(Interval.seconds(0, 100));
        GraphDataLoaderStub loader = new GraphDataLoaderStub();
        loader.putSelectorValue("{sensor=='sinusoid'}", GraphData.empty);

        program.evaluate(loader, Collections.emptyMap());
    }

    @Test
    public void expressionAnonymousSkip() {
        String source = "" +
                "let foo = sinusoid{};\n" +
                "group_lines('xxx', foo);\n";

        PreparedProgram program = compile(source).prepare(Interval.seconds(0, 100));
        GraphDataLoaderStub loader = new GraphDataLoaderStub();
        loader.putSelectorValue("{sensor=='sinusoid'}", GraphData.empty);

        program.evaluate(loader, Collections.emptyMap());
    }

    @Test
    public void expressionAnonymousIntervalLoad() {
        String source = "" +
                "let foo = sinusoid{};\n" +
                "shift(foo, 12h);\n" +
                "let bar = group_lines('sum', foo);\n";

        PreparedProgram program = compile(source).prepare(Interval.seconds(86400, 87000));
        GraphDataLoaderStub loader = new GraphDataLoaderStub();
        loader.putSelectorValue("{sensor=='sinusoid'}", GraphData.empty);

        assertThat(program.getLoadRequests().iterator().next().getInterval(), equalTo(Interval.seconds(43200, 87000)));
    }

    private Selectors parseSelectors(String source) {
        return parseSelectors(source, Collections.emptyMap());
    }

    private Selectors parseSelectors(String source, Map<String, String> params) {
        Program program = Program.fromSource(version, source).withSelectors(params).compile();
        return Iterables.getOnlyElement(program.getProgramSelectors());
    }
}
