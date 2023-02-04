package ru.yandex.solomon.alert.rule.expression;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.solomon.alert.domain.expression.ExpressionAlert;
import ru.yandex.solomon.alert.rule.ProgramCompiler;
import ru.yandex.solomon.expression.analytics.Program;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static ru.yandex.solomon.alert.domain.AlertTestSupport.randomExpressionAlert;

/**
 * @author Vladimir Gordiychuk
 */
public class ProgramCompilerTest {

    private ProgramCompiler compiler;

    @Before
    public void setUp() throws Exception {
        compiler = new ProgramCompiler();
    }

    @Test
    public void sameProgramForSameAlert() {
        ExpressionAlert source = randomExpressionAlert(ThreadLocalRandom.current())
                .toBuilder()
                .build();

        Program one = compiler.compile(source);
        Program two = compiler.compile(source);

        assertThat(one, sameInstance(two));
    }

    @Test
    public void sameProgramForSameSource() {
        ExpressionAlert v1 = randomExpressionAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setVersion(1)
                .build();

        Program one = compiler.compile(v1);

        ExpressionAlert v2 = v1.toBuilder()
                .setVersion(v1.getVersion() + 1)
                .build();

        Program two = compiler.compile(v2);

        assertThat(one, sameInstance(two));
    }

    @Test
    public void diffProgramForDiffSource() {
        ExpressionAlert v1 = randomExpressionAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setProgram("let a = true;")
                .setCheckExpression("a")
                .build();

        Program one = compiler.compile(v1);

        ExpressionAlert v2 = v1.toBuilder()
                .setProgram("let a = false;")
                .build();

        Program two = compiler.compile(v2);
        assertThat(one, not(sameInstance(two)));
    }

    @Test
    public void diffProgramForDiffCheck() {
        ExpressionAlert v1 = randomExpressionAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setProgram("let a = true;")
                .setCheckExpression("a")
                .build();

        Program one = compiler.compile(v1);

        ExpressionAlert v2 = v1.toBuilder()
                .setCheckExpression("false")
                .build();

        Program two = compiler.compile(v2);
        assertThat(one, not(sameInstance(two)));
    }

    @Test
    public void concurrentCompileSameProgram() {
        var alerts = IntStream.range(0, 100)
                .mapToObj(value -> randomExpressionAlert(ThreadLocalRandom.current())
                        .toBuilder()
                        .setProgram("let test = true;")
                        .setCheckExpression("test")
                        .build())
                .collect(Collectors.toList());

        var programs = alerts.parallelStream()
                .map(alert -> compiler.compile(alert))
                .collect(Collectors.toList());

        assertEquals(programs.size(), alerts.size());
        for (int index = 1; index < programs.size(); index++) {
            assertSame(programs.get(index - 1), programs.get(index));
        }
    }
}
