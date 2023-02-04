package ru.yandex.solomon.expression.expr.func;

import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.expression.NamedGraphData;
import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.expression.exceptions.EvaluationException;
import ru.yandex.solomon.model.timeseries.GraphData;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class SelFnGetLabelTest {
    @Test
    public void singleGraphData() {
        String result = ProgramTestSupport.expression("get_label(graphData, \"foo\");")
                .onSingleLine(GraphData.empty)
                .exec()
                .getAsSelValue()
                .castToString()
                .getValue();

        Assert.assertThat(result, CoreMatchers.equalTo(""));
    }

    @Test
    public void multiGraphData() {
        String result = ProgramTestSupport.expression("get_label(graphData, \"foo\");")
                .onMultipleLines(GraphData.empty)
                .exec()
                .getAsSelValue()
                .castToString()
                .getValue();

        Assert.assertThat(result, CoreMatchers.equalTo(""));
    }

    @Test
    public void singleNamedGraphData() {
        String result = ProgramTestSupport.expression("get_label(graphData, \"code\");")
                .onSingleLine(NamedGraphData.of("requests", Labels.of("host", "solomon-00", "code", "2xx")))
                .exec()
                .getAsSelValue()
                .castToString()
                .getValue();

        Assert.assertThat(result, CoreMatchers.equalTo("2xx"));
    }

    @Test
    public void singleNamedGraphDataMissingLabel() {
        String result = ProgramTestSupport.expression("get_label(graphData, \"foobar\");")
                .onSingleLine(NamedGraphData.of("requests", Labels.of("host", "solomon-00", "code", "2xx")))
                .exec()
                .getAsSelValue()
                .castToString()
                .getValue();

        Assert.assertThat(result, CoreMatchers.equalTo(""));
    }


    @Test
    public void multiNamedGraphDataAllHas() {
        String result = ProgramTestSupport.expression("get_label(graphData, \"host\");")
                .onMultipleLines(
                        NamedGraphData.of("requests2xx", Labels.of("host", "solomon-00", "code", "2xx")),
                        NamedGraphData.of("requests4xx", Labels.of("host", "solomon-00", "code", "4xx")),
                        NamedGraphData.of("requests5xx", Labels.of("host", "solomon-00", "code", "5xx"))
                )
                .exec()
                .getAsSelValue()
                .castToString()
                .getValue();

        Assert.assertThat(result, CoreMatchers.equalTo("solomon-00"));
    }

    @Test
    public void multiNamedGraphDataAllMissing() {
        String result = ProgramTestSupport.expression("get_label(graphData, \"foobar\");")
                .onMultipleLines(
                        NamedGraphData.of("requests2xx", Labels.of("host", "solomon-00", "code", "2xx")),
                        NamedGraphData.of("requests4xx", Labels.of("host", "solomon-00", "code", "4xx")),
                        NamedGraphData.of("requests5xx", Labels.of("host", "solomon-00", "code", "5xx"))
                )
                .exec()
                .getAsSelValue()
                .castToString()
                .getValue();

        Assert.assertThat(result, CoreMatchers.equalTo(""));
    }

    @Test(expected = EvaluationException.class)
    public void multiNamedGraphDataDifferentValues() {
        ProgramTestSupport.expression("get_label(graphData, \"code\");")
                .onMultipleLines(
                        NamedGraphData.of("requests2xx", Labels.of("host", "solomon-00", "code", "2xx")),
                        NamedGraphData.of("requests4xx", Labels.of("host", "solomon-00", "code", "4xx")),
                        NamedGraphData.of("requests5xx", Labels.of("host", "solomon-00", "code", "5xx"))
                )
                .exec();
    }

    @Test(expected = EvaluationException.class)
    public void multiNamedGraphDataSomeMissing() {
        ProgramTestSupport.expression("get_label(graphData, \"host\");")
                .onMultipleLines(
                        NamedGraphData.of("requests2xx", Labels.of("host", "solomon-00", "code", "2xx")),
                        NamedGraphData.of("requests4xx", Labels.of("code", "4xx")),
                        NamedGraphData.of("requests5xx", Labels.of("host", "solomon-00", "code", "5xx"))
                )
                .exec();
    }
}
