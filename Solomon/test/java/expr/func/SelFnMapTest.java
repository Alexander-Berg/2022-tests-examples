package ru.yandex.solomon.expression.expr.func;

import java.time.Instant;

import org.junit.Test;

import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.expression.type.SelTypes;
import ru.yandex.solomon.expression.value.SelValueBoolean;
import ru.yandex.solomon.expression.value.SelValueVector;
import ru.yandex.solomon.expression.value.SelValueVectorBuilder;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.GraphData;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public class SelFnMapTest {

    @Test
    public void mapOnEmpty() throws Exception {
        GraphData[] result = ProgramTestSupport.expression("map(graphData, x -> x+2);")
                .onMultipleLines(GraphData.empty)
                .exec()
                .getAsMultipleLines();

        assertThat(result.length, equalTo(1));
        assertThat(result[0], equalTo(GraphData.empty));
    }

    @Test
    public void mapOnGraphData() throws Exception {
        GraphData source = GraphData.of(
                point("2017-11-17T09:36:48Z", 1),
                point("2017-11-17T09:36:50Z", 2),
                point("2017-11-17T09:36:55Z", 3)
        );

        GraphData expected = GraphData.of(
                point("2017-11-17T09:36:48Z", 3),
                point("2017-11-17T09:36:50Z", 4),
                point("2017-11-17T09:36:55Z", 5)
        );

        GraphData result = ProgramTestSupport.expression("map(graphData, x -> x+2);")
                .onMultipleLines(source)
                .exec()
                .getAsSingleLine();

        assertThat(result, equalTo(expected));
    }

    @Test
    public void mapOnBoolean() throws Exception {
        GraphData negativeCheck = GraphData.of(
                point("2017-11-17T09:36:48Z", 3),
                point("2017-11-17T09:36:50Z", 1),
                point("2017-11-17T09:36:55Z", 2)
        );

        GraphData positiveCheck = GraphData.of(
                point("2017-11-17T09:36:48Z", 10),
                point("2017-11-17T09:36:50Z", 24),
                point("2017-11-17T09:36:55Z", 11)
        );

        SelValueVector result = ProgramTestSupport.expression("map(graphData, sensor -> max(sensor) > 20);")
                .onMultipleLines(negativeCheck, positiveCheck)
                .exec()
                .getAsVector();

        SelValueVector expected = new SelValueVectorBuilder(SelTypes.BOOLEAN)
                .add(SelValueBoolean.FALSE)
                .add(SelValueBoolean.TRUE)
                .build();

        assertThat(result, equalTo(expected));
    }

    private DataPoint point(String time, double value) {
        return new DataPoint(Instant.parse(time), value);
    }
}
