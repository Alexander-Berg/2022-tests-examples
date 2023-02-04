package ru.yandex.solomon.expression;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.simple.SimpleLogger;

import ru.yandex.solomon.expression.CompilerCanonSupport.AlertProgram;
import ru.yandex.solomon.expression.CompilerCanonSupport.CompiledProgram;
import ru.yandex.solomon.expression.ast.Ast;
import ru.yandex.solomon.expression.ast.AstStatement;
import ru.yandex.solomon.expression.ast.serialization.AstMappingContext;
import ru.yandex.solomon.expression.version.SelVersion;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class CompilerCanonParametrizedTest {

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
    public void compareWithCanonResults() {
        String url = "https://solomon.yandex-team.ru/admin/projects/" + projectId + "/alerts/" + alertId;
        System.out.println(url);

        String description = IGNORED_KEYS.get(Pair.of(projectId, alertId));
        Assume.assumeTrue(description, description == null);

        for (SelVersion version : SelVersion.values()) {
            CompiledProgram compiled = CompilerCanonSupport.compileSource(version, source);
            compareForVersion(version, source, compiled, result);
        }
    }

    private static void compareForVersion(SelVersion version, String source, CompiledProgram compiled, CompiledProgram expected) {
        String description = "[" + version.name() + "] On source: \n" + source;
        String compiledException = String.format("%s: %s", compiled.exceptionClass, compiled.exceptionMessage);
        String expectedException = String.format("%s: %s", expected.exceptionClass, expected.exceptionMessage);
        assertThat(description, compiledException, equalTo(expectedException));
        assertThat(description, compiled.interval, equalTo(expected.interval));
        assertThat(description, compiled.loadRequests, equalTo(expected.loadRequests));
        assertThat(description, compiled.predefinedVars, equalTo(expected.predefinedVars));
        if (expected.code == null) {
            assertThat(description, compiled.code, nullValue());
        } else {
            assertThat(description, compiled.code.size(), equalTo(expected.code.size()));
            for (int i = 0; i < compiled.code.size(); i++) {
                assertThat(description, compiled.code.get(i), equalTo(expected.code.get(i)));
            }
        }
    }

    @Test
    public void parseAstAndBack() {
        AstMappingContext mapper = new AstMappingContext(true);
        List<AstStatement> statements = new SelParser(source).parseBlock();
        List<ObjectNode> statementsJson = statements.stream()
                .map(mapper::render)
                .collect(Collectors.toList());

        List<Ast> parsed = statementsJson.stream()
                .map(mapper::<Ast>parse)
                .collect(Collectors.toList());

        assertThat(parsed, equalTo(statements));

        String rebuilt = parsed.stream().map(mapper::renderToString).collect(Collectors.joining("\n"));

        statements = statements.stream().map(AstStatement::stripRanges).collect(Collectors.toList());
        List<AstStatement> rebuiltStatements = new SelParser(rebuilt, false).parseBlock();

        assertThat(statements, equalTo(rebuiltStatements));
    }

}
