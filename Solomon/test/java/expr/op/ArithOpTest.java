package ru.yandex.solomon.expression.expr.op;

import java.time.Instant;

import org.junit.Test;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.expression.NamedGraphData;
import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.GraphData;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public class ArithOpTest {

    private static DataPoint point(String time, double value) {
        return new DataPoint(Instant.parse(time), value);
    }

    @Test
    public void multiplyOnVector() throws Exception {
        GraphData[] result = ProgramTestSupport.expression("2 * graphData;")
                .onMultipleLines(
                        GraphData.of(
                                point("2017-09-11T09:30:25Z", 2),
                                point("2017-09-11T09:30:50Z", 10),
                                point("2017-09-11T09:30:59Z", 50)
                        ),
                        GraphData.of(
                                point("2017-09-11T09:32:10Z", 100),
                                point("2017-09-11T09:32:15Z", 500),
                                point("2017-09-11T09:32:25Z", -50)
                        ))
                .exec()
                .getAsMultipleLines();

        assertThat(result[0], equalTo(GraphData.of(
                point("2017-09-11T09:30:25Z", 4),
                point("2017-09-11T09:30:50Z", 20),
                point("2017-09-11T09:30:59Z", 100)
        )));

        assertThat(result[1], equalTo(GraphData.of(
                point("2017-09-11T09:32:10Z", 200),
                point("2017-09-11T09:32:15Z", 1000),
                point("2017-09-11T09:32:25Z", -100)
        )));
    }

    @Test
    public void divOnVector() throws Exception {
        GraphData[] result = ProgramTestSupport.expression("graphData / 2;")
                .onMultipleLines(
                        GraphData.of(
                                point("2017-09-11T09:30:25Z", 2),
                                point("2017-09-11T09:30:50Z", 10),
                                point("2017-09-11T09:30:59Z", 50)
                        ),
                        GraphData.of(
                                point("2017-09-11T09:32:10Z", 100),
                                point("2017-09-11T09:32:15Z", 500),
                                point("2017-09-11T09:32:25Z", -50)
                        ))
                .exec()
                .getAsMultipleLines();

        assertThat(result[0], equalTo(GraphData.of(
                point("2017-09-11T09:30:25Z", 1),
                point("2017-09-11T09:30:50Z", 5),
                point("2017-09-11T09:30:59Z", 25)
        )));

        assertThat(result[1], equalTo(GraphData.of(
                point("2017-09-11T09:32:10Z", 50),
                point("2017-09-11T09:32:15Z", 250),
                point("2017-09-11T09:32:25Z", -25)
        )));
    }

    @Test
    public void arithOpSaveMetricMetadata() throws Exception {
        Labels expectLabels = Labels.of("project", "solomon", "cluster", "slice05", "service", "stockpile", "host", "solomon-1", "sensor", "idleTime");
        String expectedAlias = "mySensor";

        NamedGraphData namedGraphData = ProgramTestSupport.expression("2 + graphData - 5;")
                .onSingleLine(NamedGraphData.of(
                        expectedAlias, expectLabels,
                        point("2017-09-11T09:32:10Z", 1),
                        point("2017-09-11T09:32:15Z", 2),
                        point("2017-09-11T09:32:25Z", 3)
                ))
                .exec()
                .getAsNamedSingleLine();

        assertThat(namedGraphData.getLabels(), equalTo(expectLabels));
        assertThat(namedGraphData.getAlias(), equalTo(expectedAlias));
    }

    @Test
    public void arithOpSaveMetricMetadataForVector() throws Exception {
        NamedGraphData first = NamedGraphData.of(
                "solomon-1",
                Labels.of("project", "solomon", "cluster", "slice05", "service", "stockpile", "sensor", "idleTime", "host", "solomon-1"),
                point("2017-09-11T09:32:10Z", 3),
                point("2017-09-11T09:32:15Z", 2),
                point("2017-09-11T09:32:25Z", 1)
        );

        NamedGraphData second = NamedGraphData.of(
                "solomon-2",
                Labels.of("project", "solomon", "cluster", "slice05", "service", "stockpile", "sensor", "idleTime", "host", "solomon-2"),
                point("2017-09-11T09:32:10Z", 1),
                point("2017-09-11T09:32:15Z", 2),
                point("2017-09-11T09:32:25Z", 3)
        );

        NamedGraphData result[] = ProgramTestSupport.expression("2 * graphData - 5;")
                .onMultipleLines(first, second)
                .exec()
                .getAsNamedMultipleLines();

        assertThat(result[0].getLabels(), equalTo(first.getLabels()));
        assertThat(result[0].getAlias(), equalTo(first.getAlias()));

        assertThat(result[1].getLabels(), equalTo(second.getLabels()));
        assertThat(result[1].getAlias(), equalTo(second.getAlias()));
    }
}
