package ru.yandex.solomon.math.stat.trends;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.GraphData;

/**
 * @author Oleg Baryshnikov
 */
public class LogTrendLineTest {

    @Test
    public void empty() {
        GraphData source = GraphData.empty;
        LogTrendLine trendLine = new LogTrendLine(source);
        Assert.assertFalse(trendLine.canPredict());
    }

    @Test
    public void singlePoint() {
        GraphData source = GraphData.of(DataPoint.point(0, 10));
        LogTrendLine trendLine = new LogTrendLine(source);
        Assert.assertFalse(trendLine.canPredict());
    }

    @Test
    public void twoPoints() {
        GraphData source = GraphData.of(
            DataPoint.point(0, 10),
            DataPoint.point(10, 20)
        );
        LogTrendLine trendLine = new LogTrendLine(source);
        Assert.assertFalse(trendLine.canPredict());
    }

    @Test
    public void log() {
        GraphData source = GraphData.of(
            DataPoint.point(10, 0),
            DataPoint.point(20, 0.693),
            DataPoint.point(30, 1.098)
        );

        LogTrendLine trendLine = new LogTrendLine(source);

        Assert.assertEquals(1.386, trendLine.predict(40), 1E-3);
        Assert.assertEquals(1.609, trendLine.predict(50), 1E-3);
    }
}
