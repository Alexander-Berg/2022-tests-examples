package ru.yandex.solomon.expression.expr.func;

import org.junit.Test;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.expression.NamedGraphData;
import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.expression.type.SelTypes;
import ru.yandex.solomon.expression.value.SelValue;
import ru.yandex.solomon.expression.value.SelValueGraphData;
import ru.yandex.solomon.expression.value.SelValueVector;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Oleg Baryshnikov
 */
public class SelFnAliasTest {

    @Test
    public void aliasForEmptyData() {
        SelValue result = ProgramTestSupport.expression("alias(graphData, 'line');")
            .exec()
            .getAsSelValue();

        SelValueVector expected =
            new SelValueVector(SelTypes.GRAPH_DATA, new SelValue[0]);

        assertThat(result, equalTo(expected));
    }

    @Test
    public void aliasAsIs() {
        SelValue result = ProgramTestSupport.expression("alias(graphData, 'line');")
            .onMultipleLines(NamedGraphData.of(Labels.of("sensor", "jvm.memory.max")))
            .exec()
            .getAsSelValue();

        SelValue expected = new SelValueVector(
            SelTypes.GRAPH_DATA,
            new SelValue[]{
                new SelValueGraphData(NamedGraphData.of("line", Labels.of("sensor", "jvm.memory.max")))
            }
        );

        assertThat(result, equalTo(expected));
    }

    @Test
    public void aliasWithLabelPattern() {
        SelValue result = ProgramTestSupport.expression("alias(graphData, 'line {{sensor}}');")
            .onMultipleLines(NamedGraphData.of(Labels.of("sensor", "jvm.memory.max")))
            .exec()
            .getAsSelValue();

        SelValue expected = new SelValueVector(
            SelTypes.GRAPH_DATA,
            new SelValue[]{
                new SelValueGraphData(NamedGraphData.of("line jvm.memory.max", Labels.of("sensor", "jvm.memory.max")))
            }
        );

        assertThat(result, equalTo(expected));
    }

    @Test
    public void aliasWithMissingPattern() {
        SelValue result = ProgramTestSupport.expression("alias(graphData, \"line '{{unknownVar}}'\");")
            .onMultipleLines(NamedGraphData.of(Labels.of("sensor", "jvm.memory.used")))
            .exec()
            .getAsSelValue();

        SelValue expected = new SelValueVector(
            SelTypes.GRAPH_DATA,
            new SelValue[]{
                new SelValueGraphData(NamedGraphData.of("line ''", Labels.of("sensor", "jvm.memory.used")))
            }
        );

        assertThat(result, equalTo(expected));
    }

    @Test
    public void aliasWithExistingVariable() {
        SelValue result = ProgramTestSupport.expression("alias(graphData, 'line {{extVar}}');")
            .onMultipleLines(NamedGraphData.of(Labels.of("sensor", "jvm.memory.min")))
            .forVariables("let extVar = 12;")
            .exec()
            .getAsSelValue();

        SelValue expected = new SelValueVector(
            SelTypes.GRAPH_DATA,
            new SelValue[]{
                new SelValueGraphData(NamedGraphData.of("line 12.0", Labels.of("sensor", "jvm.memory.min")))
            }
        );

        assertThat(result, equalTo(expected));
    }

    @Test
    public void crossVariablesAndLabels() {
        SelValue result = ProgramTestSupport.expression("alias(graphData, 'line {{sensor}}');")
            .onMultipleLines(NamedGraphData.of(Labels.of("sensor", "jvm.memory.max")))
            .forVariables("let sensor = 8;")
            .exec()
            .getAsSelValue();

        SelValue expected = new SelValueVector(
            SelTypes.GRAPH_DATA,
            new SelValue[]{
                new SelValueGraphData(NamedGraphData.of("line 8.0", Labels.of("sensor", "jvm.memory.max")))
            }
        );

        assertThat(result, equalTo(expected));
    }
}
