package ru.yandex.solomon.expression.expr.func;

import java.time.Instant;

import org.junit.Test;

import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.expression.value.SelValue;
import ru.yandex.solomon.expression.value.SelValueDouble;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.GraphData;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Vladimir Gordiychuk
 */
public class SingleVectorItemAsGraphDataTest {
    @Test
    public void max() {
        GraphData source = GraphData.of(
                point("2018-06-01T13:30:00Z", 1),
                point("2018-06-01T13:40:00Z", 5),
                point("2018-06-01T13:50:00Z", 2)
        );

        double result = ProgramTestSupport.expression("max(graphData);")
                .onMultipleLines(source)
                .exec()
                .getAsSelValue()
                .castToScalar()
                .getValue();

        assertThat(result, equalTo(5d));
    }

    @Test
    public void multiply() {
        GraphData source = GraphData.of(
                point("2018-06-01T13:30:00Z", 1),
                point("2018-06-01T13:40:00Z", 2),
                point("2018-06-01T13:50:00Z", 3)
        );

        GraphData expected = GraphData.of(
                point("2018-06-01T13:30:00Z", 3),
                point("2018-06-01T13:40:00Z", 6),
                point("2018-06-01T13:50:00Z", 9)
        );

        GraphData result = ProgramTestSupport.expression("graphData * 3;")
                .onMultipleLines(source)
                .exec()
                .getAsSingleLine();

        assertThat(result, equalTo(expected));
    }

    @Test
    public void metricPlusAnotherMetric() {
        GraphData source = GraphData.of(
                point("2018-06-01T13:30:00Z", 1),
                point("2018-06-01T13:40:00Z", 2),
                point("2018-06-01T13:50:00Z", 3)
        );

        GraphData expected = GraphData.of(
                point("2018-06-01T13:30:00Z", 2),
                point("2018-06-01T13:40:00Z", 4),
                point("2018-06-01T13:50:00Z", 6)
        );

        GraphData result = ProgramTestSupport.expression("graphData + graphData;")
                .onMultipleLines(source)
                .exec()
                .getAsSingleLine();

        assertThat(result, equalTo(expected));
    }

    @Test
    public void sumAvg() {
        GraphData source = GraphData.of(
                point("2018-06-01T13:30:00Z", 1),
                point("2018-06-01T13:40:00Z", 2),
                point("2018-06-01T13:50:00Z", 3)
        );

        SelValue result = ProgramTestSupport.expression("avg(graphData + graphData);")
                .onMultipleLines(source)
                .exec()
                .getAsSelValue();

        assertThat(result, equalTo(new SelValueDouble(4)));
    }

    private static DataPoint point(String time, double value) {
        return new DataPoint(Instant.parse(time), value);
    }

}
