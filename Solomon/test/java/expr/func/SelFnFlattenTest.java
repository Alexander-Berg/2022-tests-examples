package ru.yandex.solomon.expression.expr.func;

import java.time.Instant;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.expression.exceptions.CompilerException;
import ru.yandex.solomon.expression.type.SelTypes;
import ru.yandex.solomon.expression.value.SelValue;
import ru.yandex.solomon.expression.value.SelValueGraphData;
import ru.yandex.solomon.expression.value.SelValueVector;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.GraphData;

/**
 * @author Vladimir Gordiychuk
 */
public class SelFnFlattenTest {

    @Test
    public void unionDoubles() {
        SelValueVector result = ProgramTestSupport.expression("flatten(as_vector(1, 2, 3), as_vector(4, 5, 6));")
            .exec()
            .getAsVector();

        SelValueVector expect = new SelValueVector(new double[]{1, 2, 3, 4, 5, 6});
        Assert.assertThat(result, CoreMatchers.equalTo(expect));
    }

    @Test
    public void unionGraphData() {
        GraphData first = GraphData.of(point("2017-06-20T09:50:00Z", 1));
        GraphData second = GraphData.of(point("2017-06-20T09:55:00Z", 2));
        SelValueVector result = ProgramTestSupport.expression("flatten(graphData, graphData);")
            .onMultipleLines(first, second)
            .exec()
            .getAsVector();

        SelValueVector expect = new SelValueVector(SelTypes.GRAPH_DATA, new SelValue[]{
            new SelValueGraphData(first),
            new SelValueGraphData(second),

            new SelValueGraphData(first),
            new SelValueGraphData(second),
        });

        Assert.assertThat(result, CoreMatchers.equalTo(expect));
    }

    @Test(expected = CompilerException.class)
    public void ableUnionOnlyVectors() {
        ProgramTestSupport.expression("flatten(1, 2, 3);").exec();
    }

    @Test(expected = CompilerException.class)
    public void notAbleUnionVectorsWithDifferentTypes() {
        ProgramTestSupport.expression("flatten(as_vector(1, 2, 3), as_vector('one', 'two', 'tree'));").exec();
    }

    private static DataPoint point(String time, double value) {
        return new DataPoint(Instant.parse(time), value);
    }
}
