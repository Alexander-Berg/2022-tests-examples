package ru.yandex.solomon.math;

import org.junit.Test;

import ru.yandex.solomon.model.timeseries.GraphData;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.solomon.model.point.DataPoint.point;

/**
 * @author Vladimir Gordiychuk
 */
public class DiffTest {

    @Test
    public void diffValueCounter() {
        GraphData source = GraphData.of(
                point("2018-04-09T14:50:00Z", 100),
                point("2018-04-09T14:51:00Z", 105),
                point("2018-04-09T14:52:00Z", 200));

        GraphData expected = GraphData.of(
                point("2018-04-09T14:51:00Z", 5),
                point("2018-04-09T14:52:00Z", 95));

        GraphData result = Diff.deltaValues(source);
        assertEquals(expected, result);
    }

    @Test
    public void diffValueNegative() {
        GraphData source = GraphData.of(
                point("2018-04-09T14:50:00Z", 100),
                point("2018-04-09T14:51:00Z", 200),
                point("2018-04-09T14:52:00Z", 150));

        GraphData expected = GraphData.of(
                point("2018-04-09T14:51:00Z", 100),
                point("2018-04-09T14:52:00Z", -50));

        GraphData result = Diff.deltaValues(source);
        assertEquals(expected, result);
    }

    @Test
    public void diffValueEmpty() {
        GraphData result = Diff.deltaValues(GraphData.empty);
        assertThat(result, equalTo(GraphData.empty));
    }

    @Test
    public void diffOnePoint() {
        GraphData result = Diff.deltaValues(GraphData.of(point("2018-04-09T14:50:00Z", 42)));
        assertThat(result, equalTo(GraphData.empty));
    }
}
