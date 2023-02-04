package ru.yandex.solomon.expression;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.ParametersAreNonnullByDefault;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.simple.SimpleLogger;

import ru.yandex.misc.test.Assert;
import ru.yandex.solomon.expression.CompilerCanonSupport.AlertProgram;
import ru.yandex.solomon.expression.CompilerCanonSupport.CompiledProgram;
import ru.yandex.solomon.expression.antlr.SolomonAstVisitor;
import ru.yandex.solomon.expression.grammar.generated.SolomonLexer;
import ru.yandex.solomon.expression.grammar.generated.SolomonParser;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class AntlrParserTest {

    @Parameterized.Parameter(value = 0)
    public String projectId;

    @Parameterized.Parameter(value = 1)
    public String alertId;

    @Parameterized.Parameter(value = 2)
    public String source;

    @Parameterized.Parameter(value = 3)
    public CompiledProgram result;

    private final static Map<Pair<String, String>, String> IGNORED_KEYS;

    static {
        IGNORED_KEYS = new HashMap<>();

        var brokenByRe2 = List.of(
                Pair.of("avia", "clickdaemon-geo-common-wizard-zero"),
                Pair.of("SK", "SK_errors_clients"),
                Pair.of("SK", "TestAlertAlice"),
                Pair.of("yabscs", "YabsCS-Servants"),
                Pair.of("yabscs", "YabsCS-Servants-Key5"),
                Pair.of("speechkit", "SK_errors")
        );
        for (var key : brokenByRe2) {
            IGNORED_KEYS.put(key, "This alert uses unsupported Perl syntax in regex.\n" +
                    "This was deprecated in SOLOMON-5667");
        }
    }

    @Parameterized.Parameters(name = "{index}: projectId={0} alertId={1}")
    public static Collection<Object[]> data() throws IOException {
        System.setProperty(SimpleLogger.LOG_KEY_PREFIX + "ru.yandex", "info");

        String prefix = "classpath:";

        List<AlertProgram> canonicalSources = CompilerCanonSupport.readAlertPrograms(prefix + CompilerCanonSupport.ALERT_PROGRAMS);
        List<CompiledProgram> canonicalResults = CompilerCanonSupport.readCanonicalResults(prefix + CompilerCanonSupport.COMPILED_PROGRAMS);

        return IntStream.range(0, canonicalResults.size())
            .mapToObj(i -> {
                AlertProgram alert = canonicalSources.get(i);
                CompiledProgram result = canonicalResults.get(i);
                return new Object[] { alert.project, alert.id, alert.program, result };
            })
            .collect(Collectors.toList());
    }

    @Test
    public void parse() {
        String url = "https://solomon.yandex-team.ru/admin/projects/" + projectId + "/alerts/" + alertId;
        System.out.println(url);

        String description = IGNORED_KEYS.get(Pair.of(projectId, alertId));
        Assume.assumeTrue(description, description == null);

        var lexer = new SolomonLexer(CharStreams.fromString(source));
        var tokens = new CommonTokenStream(lexer);
        var parser = new SolomonParser(tokens);

        lexer.removeErrorListeners();
        parser.removeErrorListeners();

        var errorListener = new ListErrorListener();
        lexer.addErrorListener(errorListener);
        parser.addErrorListener(errorListener);

        var astBuilder = new SolomonAstVisitor();

        System.out.println(source);
        parser.program();

        Assert.isEmpty(errorListener.getErrors());
    }

    private static class ListErrorListener extends BaseErrorListener {
        private final List<String> errors = new ArrayList<>();

        @Override
        public void syntaxError(
                Recognizer<?, ?> recognizer,
                Object offendingSymbol,
                int line,
                int charPositionInLine,
                String msg,
                RecognitionException e)
        {
            errors.add("line " + line + ":" + charPositionInLine + " " + msg);
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}
