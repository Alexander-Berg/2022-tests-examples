package ru.yandex.solomon.math.stat.trends;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.GraphData;

/**
 * @author Oleg Baryshnikov
 */
public class ExpTrendLineTest {

    @Test
    public void empty() {
        GraphData source = GraphData.empty;
        ExpTrendLine trendLine = new ExpTrendLine(source);
        Assert.assertFalse(trendLine.canPredict());
    }

    @Test
    public void singlePoint() {
        GraphData source = GraphData.of(DataPoint.point(0, 10));
        ExpTrendLine trendLine = new ExpTrendLine(source);
        Assert.assertFalse(trendLine.canPredict());
    }

    @Test
    public void twoPoints() {
        GraphData source = GraphData.of(
            DataPoint.point(0, 10),
            DataPoint.point(10, 20)
        );
        ExpTrendLine trendLine = new ExpTrendLine(source);
        Assert.assertFalse(trendLine.canPredict());
    }

    @Test
    public void exp() {
        GraphData source = GraphData.of(
            DataPoint.point(0, 1.0),
            DataPoint.point(1, 2.78),
            DataPoint.point(2, 7.39)
        );

        ExpTrendLine trendLine = new ExpTrendLine(source);

        Assert.assertEquals(20.24, trendLine.predict(3), 1E-2);
        Assert.assertEquals(55.02, trendLine.predict(4), 1E-2);
    }
}
