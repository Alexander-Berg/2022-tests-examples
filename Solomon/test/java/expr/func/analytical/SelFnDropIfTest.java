package ru.yandex.solomon.expression.expr.func.analytical;

import java.time.Instant;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.expression.NamedGraphData;
import ru.yandex.solomon.expression.analytics.GraphDataLoaderStub;
import ru.yandex.solomon.expression.analytics.MultilineProgramTestSupport;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.GraphData;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Ivan Tsybulin
 */
public class SelFnDropIfTest {

    private static DataPoint point(String time, double value) {
        return new DataPoint(Instant.parse(time), value);
    }

    private GraphDataLoaderStub loader;

    @Before
    public void setup() {
        loader = new GraphDataLoaderStub();

        NamedGraphData line1 = NamedGraphData.of(GraphData.of(
                point("2018-06-02T06:51:00Z", 1),
                point("2018-06-02T06:52:00Z", 2),
                point("2018-06-02T06:53:00Z", 3),
                point("2018-06-02T06:54:00Z", 4),
                point("2018-06-02T06:55:00Z", 5),
                point("2018-06-02T06:56:00Z", 6),
                point("2018-06-02T06:57:00Z", 7),
                point("2018-06-02T06:58:00Z", 8),
                point("2018-06-02T06:59:00Z", 9)
        ), "", Labels.of("name", "line1"), "line1");

        NamedGraphData line2 = NamedGraphData.of(GraphData.of(
                point("2018-06-02T06:53:00Z", 100),
                point("2018-06-02T06:53:30Z", 200),
                point("2018-06-02T06:54:00Z", 300),
                point("2018-06-02T06:54:30Z", 400),
                point("2018-06-02T06:55:00Z", 500),
                point("2018-06-02T06:55:30Z", 600),
                point("2018-06-02T06:56:00Z", 700),
                point("2018-06-02T06:56:30Z", 800),
                point("2018-06-02T06:57:00Z", 900)
        ), "", Labels.of("name", "line2"), "line2");

        NamedGraphData alarms = NamedGraphData.of(GraphData.of(
                point("2018-06-02T06:53:17Z", 1),
                point("2018-06-02T06:54:00Z", 0),
                point("2018-06-02T06:54:30Z", Double.NaN),
                point("2018-06-02T06:55:00Z", 1),
                point("2018-06-02T06:56:59Z", 0)
        ), "", Labels.of("name", "alarms"), "alarms");

        loader.putSelectorValue("name=alarms", alarms);
        loader.putSelectorValue("name=line1", line1);
        loader.putSelectorValue("name=line2", line2);
        loader.putSelectorValue("name=lines", line1, line2);
    }

    @Test
    public void singleTest() {
        GraphData result = MultilineProgramTestSupport.create()
                .addLine("let line1 = {name=line1};")
                .addLine("let alarm = {name=alarms};")
                .addLine("let data = drop_if(alarm, line1);")
                .onData(loader)
                .fromTime("2018-06-02T00:00:00Z")
                .toTime("2018-06-03T00:00:00Z")
                .exec("data")
                .getAsSingleLine();

        assertThat(result, equalTo(GraphData.of(
                point("2018-06-02T06:51:00Z", 1),
                point("2018-06-02T06:52:00Z", 2),
                point("2018-06-02T06:53:00Z", 3),
                point("2018-06-02T06:54:00Z", 4),
                point("2018-06-02T06:57:00Z", 7),
                point("2018-06-02T06:58:00Z", 8),
                point("2018-06-02T06:59:00Z", 9)
        )));
    }

    @Test
    public void explicitSingleTest() {
        GraphData result = MultilineProgramTestSupport.create()
            .addLine("let line1 = {name=line1};")
            .addLine("let alarm = single({name=alarms});")
            .addLine("let data = drop_if(alarm, line1);")
            .onData(loader)
            .fromTime("2018-06-02T00:00:00Z")
            .toTime("2018-06-03T00:00:00Z")
            .exec("data")
            .getAsSingleLine();

        assertThat(result, equalTo(GraphData.of(
            point("2018-06-02T06:51:00Z", 1),
            point("2018-06-02T06:52:00Z", 2),
            point("2018-06-02T06:53:00Z", 3),
            point("2018-06-02T06:54:00Z", 4),
            point("2018-06-02T06:57:00Z", 7),
            point("2018-06-02T06:58:00Z", 8),
            point("2018-06-02T06:59:00Z", 9)
        )));
    }

    @Test
    public void multipleTest() {
        GraphData[] result = MultilineProgramTestSupport.create()
                .addLine("let lines = {name=lines};")
                .addLine("let alarm = {name=alarms};")
                .addLine("let data = drop_if(alarm, lines);")
                .onData(loader)
                .fromTime("2018-06-02T00:00:00Z")
                .toTime("2018-06-03T00:00:00Z")
                .exec("data")
                .getAsMultipleLines();

        assertThat(result[0], equalTo(GraphData.of(
                point("2018-06-02T06:51:00Z", 1),
                point("2018-06-02T06:52:00Z", 2),
                point("2018-06-02T06:53:00Z", 3),
                point("2018-06-02T06:54:00Z", 4),
                point("2018-06-02T06:57:00Z", 7),
                point("2018-06-02T06:58:00Z", 8),
                point("2018-06-02T06:59:00Z", 9)
        )));
        assertThat(result[1], equalTo(GraphData.of(
                point("2018-06-02T06:53:00Z", 100),
                point("2018-06-02T06:54:00Z", 300),
                point("2018-06-02T06:54:30Z", 400),
                point("2018-06-02T06:57:00Z", 900)
        )));
    }
}
