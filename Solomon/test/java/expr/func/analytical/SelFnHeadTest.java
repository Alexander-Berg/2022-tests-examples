package ru.yandex.solomon.expression.expr.func.analytical;

import java.time.Instant;

import org.junit.Test;

import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.GraphData;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public class SelFnHeadTest {

    private static DataPoint point(String time, double value) {
        return new DataPoint(Instant.parse(time), value);
    }

    @Test
    public void emptyHeadByPoints() {
        GraphData result = ProgramTestSupport.expression("head(graphData, 42);")
                .onSingleLine(GraphData.empty)
                .fromTime("2018-06-02T06:50:00Z")
                .toTime("2018-06-02T06:59:00Z")
                .exec()
                .getAsSingleLine();

        assertThat(result, equalTo(GraphData.empty));
    }

    @Test
    public void headByPointsOnSmall() {
        GraphData source = GraphData.of(
                point("2018-06-02T06:50:00Z", 1),
                point("2018-06-02T06:51:00Z", 2)
        );

        GraphData result = ProgramTestSupport.expression("head(graphData, 3);")
                .onSingleLine(source)
                .exec()
                .getAsSingleLine();

        assertThat(result, equalTo(source));
    }

    @Test
    public void headByPoints() {
        GraphData source = GraphData.of(
                point("2018-06-02T06:50:00Z", 1),
                point("2018-06-02T06:51:00Z", 2),
                point("2018-06-02T06:51:10Z", 3),
                point("2018-06-02T06:51:30Z", 4)
        );

        GraphData result = ProgramTestSupport.expression("head(graphData, 2);")
                .onSingleLine(source)
                .exec()
                .getAsSingleLine();

        GraphData expected = GraphData.of(
                point("2018-06-02T06:50:00Z", 1),
                point("2018-06-02T06:51:00Z", 2)
        );

        assertThat(result, equalTo(expected));
    }

    @Test
    public void headByPointsVector() {
        GraphData one = GraphData.of(
                point("2018-06-02T06:50:00Z", 1),
                point("2018-06-02T06:51:00Z", 2),
                point("2018-06-02T06:51:10Z", 3),
                point("2018-06-02T06:51:30Z", 4)
        );

        GraphData two = GraphData.of(
                point("2018-06-02T06:50:10Z", 1),
                point("2018-06-02T06:50:20Z", 2),
                point("2018-06-02T06:50:30Z", 3)
        );

        GraphData[] result = ProgramTestSupport.expression("head(graphData, 2);")
                .onMultipleLines(one, two, GraphData.empty)
                .exec()
                .getAsMultipleLines();

        assertThat(result[0], equalTo(GraphData.of(
                point("2018-06-02T06:50:00Z", 1),
                point("2018-06-02T06:51:00Z", 2)
        )));

        assertThat(result[1], equalTo(GraphData.of(
                point("2018-06-02T06:50:10Z", 1),
                point("2018-06-02T06:50:20Z", 2)
        )));

        assertThat(result[2], equalTo(GraphData.empty));
    }

    @Test
    public void emptyHeadByDuration() {
        GraphData result = ProgramTestSupport.expression("head(graphData, 10m30s);")
                .onMultipleLines(GraphData.empty)
                .fromTime("2018-06-02T06:50:00Z")
                .toTime("2018-06-02T06:59:00Z")
                .exec()
                .getAsSingleLine();

        assertThat(result, equalTo(GraphData.empty));
    }

    @Test
    public void headByDurationOnSmall() {
        GraphData source = GraphData.of(
                point("2018-06-02T06:50:00Z", 1),
                point("2018-06-02T06:51:00Z", 2)
        );

        GraphData result = ProgramTestSupport.expression("head(graphData, 10m);")
                .onSingleLine(source)
                .exec()
                .getAsSingleLine();

        assertThat(result, equalTo(source));
    }

    @Test
    public void headByDuration() {
        GraphData source = GraphData.of(
                point("2018-06-02T06:50:00Z", 1),
                point("2018-06-02T06:50:10Z", 2),
                point("2018-06-02T06:50:30Z", 3),
                point("2018-06-02T06:51:30Z", 4)
        );

        GraphData result = ProgramTestSupport.expression("head(graphData, 1m);")
                .onSingleLine(source)
                .exec()
                .getAsSingleLine();

        GraphData expected = GraphData.of(
                point("2018-06-02T06:50:00Z", 1),
                point("2018-06-02T06:50:10Z", 2),
                point("2018-06-02T06:50:30Z", 3)
        );

        assertThat(result, equalTo(expected));
    }

    @Test
    public void headByDurationVector() {
        GraphData one = GraphData.of(
                point("2018-06-02T06:50:00Z", 1),
                point("2018-06-02T06:50:15Z", 2),
                point("2018-06-02T06:50:30Z", 3),
                point("2018-06-02T06:51:30Z", 4)
        );

        GraphData two = GraphData.of(
                point("2018-06-02T06:50:30Z", 1),
                point("2018-06-02T06:51:10Z", 2),
                point("2018-06-02T06:52:30Z", 3)
        );

        GraphData[] result = ProgramTestSupport.expression("head(graphData, 1m);")
                .onMultipleLines(one, two, GraphData.empty)
                .exec()
                .getAsMultipleLines();

        assertThat(result[0], equalTo(GraphData.of(
                point("2018-06-02T06:50:00Z", 1),
                point("2018-06-02T06:50:15Z", 2),
                point("2018-06-02T06:50:30Z", 3)
        )));

        assertThat(result[1], equalTo(GraphData.of(
                point("2018-06-02T06:50:30Z", 1),
                point("2018-06-02T06:51:10Z", 2)
        )));

        assertThat(result[2], equalTo(GraphData.empty));
    }
}
