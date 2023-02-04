package ru.yandex.solomon.expression.use;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import ru.yandex.solomon.expression.analytics.Program;
import ru.yandex.solomon.expression.exceptions.ParserException;
import ru.yandex.solomon.labels.query.Selector;
import ru.yandex.solomon.labels.query.Selectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class UseConstructTest {
    @Test
    public void useCompiles() {
        String code = ""
            + "use {host='solomon-1', cluster=production, service=alerting};\n"
            + "let data = {sensor=uptime};\n";

        Program.fromSource(code).compile();
    }

    @Test
    public void useAffectsSelectors() {
        String code = ""
                + "use {host='solomon-1', cluster=production, service==alerting, projectId != total};\n"
                + "let data = {sensor=uptime};\n";

        var program = Program.fromSource(code).compile();
        var selectors = program.getProgramSelectors();
        assertThat(selectors, equalTo(List.of(Selectors.of(
                Selector.glob("host", "solomon-1"),
                Selector.glob("cluster", "production"),
                Selector.exact("service", "alerting"),
                Selector.notGlob("projectId", "total"),
                Selector.glob("sensor", "uptime")
        ))));
    }

    @Test
    public void useMayBeOverriden() {
        String code = ""
                + "use {service=alerting, cluster=production, host=cluster};\n"
                + "let fraction = requests{host='solomon-1'} / requests{};\n";

        var program = Program.fromSource(code).compile();
        var selectors = program.getProgramSelectors();

        assertThat(selectors, containsInAnyOrder(
            Selectors.of(
                Selector.exact("sensor", "requests"),
                Selector.glob("service", "alerting"),
                Selector.glob("cluster", "production"),
                Selector.glob("host", "solomon-1")),
            Selectors.of(
                Selector.exact("sensor", "requests"),
                Selector.glob("service", "alerting"),
                Selector.glob("cluster", "production"),
                Selector.glob("host", "cluster"))
        ));
    }

    @Test(expected = ParserException.class)
    public void useIsNotFirstFails() {
        String code = ""
                + "let foo = 42;"
                + "use {service=alerting, cluster=production, host=cluster};\n"
                + "let fraction = requests{host='solomon-1'} / requests{};\n";

        Program.fromSource(code).compile();
    }

    @Test(expected = ParserException.class)
    public void useRequiresBraces() {
        String code = ""
                + "use service=alerting, cluster=production, host=cluster;\n"
                + "let fraction = requests{host='solomon-1'} / requests{};\n";

        Program.fromSource(code).compile();
    }

}
