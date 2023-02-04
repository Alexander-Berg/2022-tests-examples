package ru.yandex.solomon.expression.expr.func;

import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.expression.type.SelTypes;
import ru.yandex.solomon.expression.value.SelValue;
import ru.yandex.solomon.expression.value.SelValueGraphData;
import ru.yandex.solomon.expression.value.SelValueVector;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.GraphData;

/**
 * @author Oleg Baryshnikov
 */
@ParametersAreNonnullByDefault
public class SelFnToVectorTest {
    @Test
    public void elementToDoubleVector() {
        SelValueVector result = ProgramTestSupport.expression("to_vector(1);")
            .exec()
            .getAsVector();

        SelValueVector expect = new SelValueVector(new double[]{1});
        Assert.assertThat(result, CoreMatchers.equalTo(expect));
    }
    @Test
    public void vectorToDoubleVector() {
        SelValue result = ProgramTestSupport.expression("to_vector(as_vector(1, 2, 3));")
            .exec()
            .getAsSelValue();
        SelValue expect = new SelValueVector(new double[]{1, 2, 3});
        Assert.assertThat(result, CoreMatchers.equalTo(expect));
    }

    @Test
    public void elementToGraphDataVector() {
        GraphData first = GraphData.of(DataPoint.point("2017-06-20T09:50:00Z", 1));
        SelValue result = ProgramTestSupport.expression("to_vector(graphData);")
            .onSingleLine(first)
            .exec()
            .getAsSelValue();
        SelValue expect = new SelValueVector(SelTypes.GRAPH_DATA, new SelValue[]{
            new SelValueGraphData(first)
        });
        Assert.assertThat(result, CoreMatchers.equalTo(expect));
    }

    @Test
    public void vectorToGraphDataVector() {
        GraphData first = GraphData.of(DataPoint.point("2017-06-20T09:50:00Z", 1));
        SelValue result = ProgramTestSupport.expression("to_vector(graphData);")
            .onMultipleLines(first)
            .exec()
            .getAsSelValue();
        SelValue expect = new SelValueVector(SelTypes.GRAPH_DATA, new SelValue[]{
            new SelValueGraphData(first)
        });
        Assert.assertThat(result, CoreMatchers.equalTo(expect));
    }
}
