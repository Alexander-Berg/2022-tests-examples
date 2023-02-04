package ru.yandex.solomon.expression.expr.func;

import java.time.Instant;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.expression.exceptions.CompilerException;
import ru.yandex.solomon.expression.type.SelType;
import ru.yandex.solomon.expression.type.SelTypeVector;
import ru.yandex.solomon.expression.type.SelTypes;
import ru.yandex.solomon.expression.value.SelValue;
import ru.yandex.solomon.expression.value.SelValueGraphData;
import ru.yandex.solomon.expression.value.SelValueString;
import ru.yandex.solomon.expression.value.SelValueVector;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.GraphData;

/**
 * @author Vladimir Gordiychuk
 */
public class SelFnAsVectorTest {

    @Test
    public void vectorWithDoubleType() {
        SelValueVector result = ProgramTestSupport.expression("as_vector(1, 2, 3);")
            .exec()
            .getAsVector();

        Assert.assertThat(result.type(), CoreMatchers.equalTo(new SelTypeVector(SelTypes.DOUBLE)));

        result = ProgramTestSupport.expression("[1, 2, 3];")
                .exec()
                .getAsVector();

        Assert.assertThat(result.type(), CoreMatchers.equalTo(new SelTypeVector(SelTypes.DOUBLE)));
    }

    @Test
    public void vectorWithStringType() {
        ensureTypeValid("as_vector('one', 'two', 'three');", SelTypes.STRING_VECTOR);
    }

    @Test
    public void vectorWithDurationType() {
        ensureTypeValid("as_vector(1d, 2d1h, 3d);", new SelTypeVector(SelTypes.DURATION));
    }

    @Test
    public void vectorOfVectorOfGraphData() {
        ensureTypeValid("as_vector(graphData);", new SelTypeVector(SelTypes.GRAPH_DATA_VECTOR));
    }

    @Test
    public void vectorOfVectorType() {
        SelType expectType = new SelTypeVector(SelTypes.DOUBLE_VECTOR);
        ensureTypeValid("as_vector(as_vector(1, 2, 3), as_vector(3, 2));", expectType);
    }

    @Test
    public void vectorContentDouble() {
         double[] result = ProgramTestSupport.expression("as_vector(1, 2.4, 3);")
            .exec()
            .getAsVector()
            .doubleArray();

        Assert.assertArrayEquals(result, new double[]{1d, 2.4d, 3d}, 0);
    }

    @Test
    public void vectorContentString() {
        SelValue[] result = ProgramTestSupport.expression("as_vector('min', 'max', 'avg');")
            .exec()
            .getAsVector()
            .valueArray();

        Assert.assertArrayEquals(result, new SelValue[]{new SelValueString("min"), new SelValueString("max"), new SelValueString("avg")});
    }

    @Test
    public void vectorWithSingleGraphData() {
        GraphData expect = GraphData.of(
            point("2017-06-20T09:50:34Z", 2.1),
            point("2017-06-20T09:55:00Z", 5)
        );

        SelValueVector result = ProgramTestSupport.expression("as_vector(graphData);")
            .onSingleLine(expect)
            .exec()
            .getAsVector();

        Assert.assertThat(result.item(0), CoreMatchers.equalTo(new SelValueGraphData(expect)));
    }

    @Test(expected = CompilerException.class)
    public void notAbleCreateEmptyVector() {
        ProgramTestSupport.expression("as_vector();").exec();
        Assert.fail("As vector require one or more argument, but if arguments not specified function should fail");
    }

    @Test(expected = CompilerException.class)
    public void notAbleCreateVectorOnDifferentTypeArguments() {
        ProgramTestSupport.expression("as_vector(1, 'two', 3, 4d);").exec();
        Assert.fail("Vector can be create only via data with the same types");
    }

    @Test(expected = CompilerException.class)
    public void notAbleCreateVectorOnDifferentTypeArgumentsBrackets() {
        ProgramTestSupport.expression("[1, 'two', 3, 4d];").exec();
        Assert.fail("Vector can be create only via data with the same types");
    }

    private void ensureTypeValid(String expression, SelType expectType) {
        ensureTypeValidImpl(expression, expectType);
        ensureTypeValidImpl(expression
                        .replaceFirst("as_vector\\(", "[")
                        .replaceFirst("\\);", "];")
                , expectType);
    }

    private void ensureTypeValidImpl(String expression, SelType expectType) {
        SelValueVector result = ProgramTestSupport.expression(expression)
            .onMultipleLines(GraphData.empty)
            .exec()
            .getAsVector();

        Assert.assertThat(result.type(), CoreMatchers.equalTo(expectType));
    }

    private static DataPoint point(String time, double value) {
        return new DataPoint(Instant.parse(time), value);
    }
}
