package ru.yandex.solomon.expression.expr.func;

import java.time.Instant;

import org.junit.Test;

import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.expression.value.SelValueVector;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.GraphData;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public class SelFnDiffTest {

    @Test
    public void emptyVector() {
        SelValueVector result = ProgramTestSupport.expression("diff(graphData);")
                .onMultipleLines(new GraphData[0])
                .exec()
                .getAsVector();

        assertThat(result.length(), equalTo(0));
    }

    @Test
    public void deltaSingle() {
        GraphData source = GraphData.of(
                point("2018-04-09T15:24:00Z", 100),
                point("2018-04-09T15:25:00Z", 125),
                point("2018-04-09T15:26:00Z", 200));

        GraphData result = ProgramTestSupport.expression("diff(graphData);")
                .onSingleLine(source)
                .exec()
                .getAsSingleLine();

        GraphData expected = GraphData.of(
                point("2018-04-09T15:25:00Z", 25),
                point("2018-04-09T15:26:00Z", 75));

        assertThat(result, equalTo(expected));
    }

    @Test
    public void deltaMultiple() {
        GraphData one = GraphData.of(
                point("2018-04-09T15:24:00Z", 100),
                point("2018-04-09T15:25:00Z", 125),
                point("2018-04-09T15:26:00Z", 200));

        GraphData two = GraphData.empty;

        GraphData tree = GraphData.of(
                point("2018-04-09T15:24:10Z", 100),
                point("2018-04-09T15:25:10Z", 100),
                point("2018-04-09T15:26:10Z", 100));


        SelValueVector result = ProgramTestSupport.expression("diff(graphData);")
                .onMultipleLines(one, two, tree)
                .exec()
                .getAsVector();

        assertThat(result.item(0).castToGraphData().getGraphData(),
                equalTo(GraphData.of(
                        point("2018-04-09T15:25:00Z", 25),
                        point("2018-04-09T15:26:00Z", 75))));

        assertThat(result.item(1).castToGraphData().getGraphData(), equalTo(GraphData.empty));

        GraphData expected = GraphData.of(
                point("2018-04-09T15:25:10Z", 0),
                point("2018-04-09T15:26:10Z", 0));

        assertThat(result.item(2).castToGraphData().getGraphData(), equalTo(expected));
    }

    private DataPoint point(String time, double value) {
        return new DataPoint(Instant.parse(time), value);
    }
}
