package ru.yandex.solomon.expression.expr.func.analytical.trends;

import org.junit.Test;

import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.GraphData;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Oleg Baryshnikov
 */
public class SelFnLogarithmicTrendTest {

    @Test
    public void empty() {
        GraphData result = ProgramTestSupport.expression("logarithmic_trend(graphData, 0m, 1m);")
            .onSingleLine(GraphData.empty)
            .exec()
            .getAsSingleLine();

        assertThat(result, equalTo(GraphData.empty));
    }

    @Test
    public void singlePoint() {
        GraphData result = ProgramTestSupport.expression("logarithmic_trend(graphData, 0m, 1m);")
            .onSingleLine(GraphData.of(DataPoint.point(10, 10)))
            .exec()
            .getAsSingleLine();

        assertThat(result, equalTo(GraphData.empty));
    }

    @Test
    public void twoPoints() {
        GraphData result = ProgramTestSupport.expression("logarithmic_trend(graphData, 0m, 1m);")
            .onSingleLine(GraphData.of(
                DataPoint.point(10, 10),
                DataPoint.point(20, 20)
            ))
            .exec()
            .getAsSingleLine();

        assertThat(result, equalTo(GraphData.empty));
    }

    @Test
    public void trendInPast() {
        GraphData result = ProgramTestSupport.expression("logarithmic_trend(graphData, -20s, 0s);")
            .onSingleLine(GraphData.of(
                DataPoint.point(10_000, 0),
                DataPoint.point(20_000, Math.log(2)),
                DataPoint.point(30_000, Math.log(3))
            ))
            .exec()
            .getAsSingleLine();

        checkTrendLine(result);
    }

    @Test
    public void trendInFuture() {
        GraphData result = ProgramTestSupport.expression("logarithmic_trend(graphData, 0s, 30s);")
            .onSingleLine(GraphData.of(
                DataPoint.point(10_000, 0),
                DataPoint.point(20_000, Math.log(2)),
                DataPoint.point(30_000, Math.log(3))
            ))
            .exec()
            .getAsSingleLine();

        checkTrendLine(result);
    }

    private void checkTrendLine(GraphData result) {
        for (int i = 0; i < result.length(); ++i) {
            long ts = result.getTimestamps().at(i);
            double value = result.getValues().at(i);
            double expectedValue = Math.log(ts / 10_000d);
            assertEquals(expectedValue, value, 10);
        }
    }
}
