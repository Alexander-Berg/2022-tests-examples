package ru.yandex.solomon.math.stat.trends;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.GraphData;

/**
 * @author Oleg Baryshnikov
 */
public class LinearTrendLineTest {

    @Test
    public void empty() {
        GraphData source = GraphData.empty;
        LinearTrendLine trendLine = new LinearTrendLine(source);
        Assert.assertFalse(trendLine.canPredict());
    }

    @Test
    public void singlePoint() {
        GraphData source = GraphData.of(DataPoint.point(0, 10));
        LinearTrendLine trendLine = new LinearTrendLine(source);
        Assert.assertFalse(trendLine.canPredict());
    }

    @Test
    public void twoPoints() {
        GraphData source = GraphData.of(
            DataPoint.point(0, 10),
            DataPoint.point(10, 20)
        );
        LinearTrendLine trendLine = new LinearTrendLine(source);
        Assert.assertFalse(trendLine.canPredict());
    }

    @Test
    public void linear() {
        GraphData source = GraphData.of(
            DataPoint.point(0, 10),
            DataPoint.point(10, 20),
            DataPoint.point(20, 30)
        );

        LinearTrendLine trendLine = new LinearTrendLine(source);

        Assert.assertEquals(40, trendLine.predict(30), 1E-3);
        Assert.assertEquals(50, trendLine.predict(40), 1E-3);
    }
}
