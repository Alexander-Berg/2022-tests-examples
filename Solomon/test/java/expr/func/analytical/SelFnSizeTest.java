package ru.yandex.solomon.expression.expr.func.analytical;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import ru.yandex.solomon.expression.analytics.PreparedProgram;
import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.expression.compile.SelAssignment;
import ru.yandex.solomon.expression.exceptions.CompilerException;
import ru.yandex.solomon.expression.expr.SelExprValue;
import ru.yandex.solomon.expression.version.SelVersion;
import ru.yandex.solomon.model.timeseries.GraphData;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class SelFnSizeTest {

    @Test
    public void emptyVector() {
        double size = ProgramTestSupport.expression("size(graphData);")
                .onMultipleLines(new GraphData[0])
                .exec()
                .getAsSelValue()
                .castToScalar()
                .getValue();

        assertThat(size, equalTo(0d));
    }


    @Test
    public void singleton() {
        double size = ProgramTestSupport.expression("size(graphData);")
                .onMultipleLines(GraphData.empty)
                .exec()
                .getAsSelValue()
                .castToScalar()
                .getValue();

        assertThat(size, equalTo(1d));
    }

    @Test
    public void vector() {
        double size = ProgramTestSupport.expression("size(graphData);")
                .onMultipleLines(GraphData.empty, GraphData.empty, GraphData.empty)
                .exec()
                .getAsSelValue()
                .castToScalar()
                .getValue();

        assertThat(size, equalTo(3d));
    }

    @Test(expected = CompilerException.class)
    public void notVector() {
        ProgramTestSupport.expression("size(graphData);")
                .onSingleLine(GraphData.empty)
                .exec();
    }

    @Test
    public void inlined() {
        for (SelVersion version : SelVersion.values()) {
            PreparedProgram pp = ProgramTestSupport.expression("size(as_vector(3, 1, 4, 1, 5, 9));")
                    .onSingleLine(GraphData.empty)
                    .prepare(version)
                    .getPrepared();

            SelAssignment result = (SelAssignment) pp.getCode().get(1);
            assertThat(result.getIdent(), equalTo("data"));
            assertThat(result.getExpr(), instanceOf(SelExprValue.class));
            SelExprValue value = (SelExprValue) result.getExpr();
            assertThat(value.getValue().castToScalar().getValue(), equalTo(6d));
        }
    }

    @Test
    public void doubleVector() {
        double size = ProgramTestSupport.expression("size(get_timestamps(graphData));")
                .onSingleLine(GraphData.graphData(1000, 42, 1500, 43, 2500, 44))
                .exec()
                .getAsSelValue()
                .castToScalar()
                .getValue();

        assertThat(size, equalTo(3d));
    }
}
