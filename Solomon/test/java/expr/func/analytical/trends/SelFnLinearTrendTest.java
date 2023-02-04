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
public class SelFnLinearTrendTest {

    @Test
    public void empty() {
        GraphData result = ProgramTestSupport.expression("linear_trend(graphData, 0m, 1m);")
            .onSingleLine(GraphData.empty)
            .exec()
            .getAsSingleLine();

        assertThat(result, equalTo(GraphData.empty));
    }

    @Test
    public void singlePoint() {
        GraphData result = ProgramTestSupport.expression("linear_trend(graphData, 0m, 1m);")
            .onSingleLine(GraphData.of(DataPoint.point(10, 10)))
            .exec()
            .getAsSingleLine();

        assertThat(result, equalTo(GraphData.empty));
    }

    @Test
    public void twoPoints() {
        GraphData result = ProgramTestSupport.expression("linear_trend(graphData, 0m, 1m);")
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
        GraphData result = ProgramTestSupport.expression("linear_trend(graphData, -30s, 0s);")
            .onSingleLine(GraphData.of(
                DataPoint.point(15_000, 0),
                DataPoint.point(30_000, 10),
                DataPoint.point(45_000, 20)
            ))
            .exec()
            .getAsSingleLine();

        checkTrendLine(result);
    }

    @Test
    public void trendInFuture() {
        GraphData result = ProgramTestSupport.expression("linear_trend(graphData, 0s, 30s);")
            .onSingleLine(GraphData.of(
                DataPoint.point(15_000, 0),
                DataPoint.point(30_000, 10),
                DataPoint.point(45_000, 20)
            ))
            .exec()
            .getAsSingleLine();

        checkTrendLine(result);
    }

    private void checkTrendLine(GraphData result) {
        for (int i = 0; i < result.length(); ++i) {
            long ts = result.getTimestamps().at(i);
            double value = result.getValues().at(i);
            double expectedValue = ts / 1500d - 10;
            assertEquals(expectedValue, value, 1E-3);
        }
    }
}
