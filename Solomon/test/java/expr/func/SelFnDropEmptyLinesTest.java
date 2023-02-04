package ru.yandex.solomon.expression.expr.func;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.expression.NamedGraphData;
import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.model.timeseries.GraphData;

import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.solomon.model.point.DataPoint.point;

/**
 * @author Oleg Baryshnikov
 */
@ParametersAreNonnullByDefault
public class SelFnDropEmptyLinesTest {

    @Test
    public void single() {
        NamedGraphData line1 = NamedGraphData.newBuilder()
            .setGraphData(GraphData.of(
                point("2019-12-17T00:00:00Z", 10),
                point("2019-12-17T00:01:00Z", Double.NaN)))
            .setLabels(Labels.of("sensor", "sensor1"))
            .build();

        NamedGraphData[] result = ProgramTestSupport.expression("drop_empty_lines(graphData);")
            .onSingleLine(line1)
            .exec()
            .getAsNamedMultipleLines();

        NamedGraphData[] expected = new NamedGraphData[] {line1};

        Assert.assertThat(result, equalTo(expected));
    }

    @Test
    public void vector() {
        NamedGraphData line1 = NamedGraphData.newBuilder()
            .setGraphData(GraphData.of(
                point("2019-12-17T00:00:00Z", 10),
                point("2019-12-17T00:01:00Z", Double.NaN)))
            .setLabels(Labels.of("sensor", "sensor1"))
            .build();
        NamedGraphData line2 = NamedGraphData.newBuilder()
            .setGraphData(GraphData.empty)
            .setLabels(Labels.of("sensor", "sensor2"))
            .build();
        NamedGraphData line3 = NamedGraphData.newBuilder()
            .setGraphData(GraphData.of(
                point("2019-12-17T00:00:00Z", Double.NaN),
                point("2019-12-17T00:01:00Z", Double.NaN)
            ))
            .setLabels(Labels.of("sensor", "sensor3"))
            .build();

        NamedGraphData[] result = ProgramTestSupport.expression("drop_empty_lines(graphData);")
            .onMultipleLines(line1, line2, line3)
            .exec()
            .getAsNamedMultipleLines();

        NamedGraphData[] expected = new NamedGraphData[] {line1};

        Assert.assertThat(result, equalTo(expected));
    }

    @Test
    public void alias() {
        NamedGraphData line1 = NamedGraphData.newBuilder()
                .setGraphData(GraphData.of(
                        point("2019-12-17T00:00:00Z", 10),
                        point("2019-12-17T00:01:00Z", Double.NaN)))
                .setLabels(Labels.of("sensor", "sensor1"))
                .build();
        NamedGraphData line2 = NamedGraphData.newBuilder()
                .setGraphData(GraphData.empty)
                .setLabels(Labels.of("sensor", "sensor2"))
                .build();
        NamedGraphData line3 = NamedGraphData.newBuilder()
                .setGraphData(GraphData.of(
                        point("2019-12-17T00:00:00Z", Double.NaN),
                        point("2019-12-17T00:01:00Z", Double.NaN)
                ))
                .setLabels(Labels.of("sensor", "sensor3"))
                .build();

        NamedGraphData[] result = ProgramTestSupport.expression("drop_empty_series(graphData);")
                .onMultipleLines(line1, line2, line3)
                .exec()
                .getAsNamedMultipleLines();

        NamedGraphData[] expected = new NamedGraphData[] {line1};

        Assert.assertThat(result, equalTo(expected));
    }
}
