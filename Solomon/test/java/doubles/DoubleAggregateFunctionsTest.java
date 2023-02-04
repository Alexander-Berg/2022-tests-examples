package ru.yandex.solomon.math.doubles;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.model.timeseries.SortedOrCheck;
import ru.yandex.solomon.model.timeseries.view.DoubleTimeSeriesView;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class DoubleAggregateFunctionsTest {

    private void testIntegrate(double expected, GraphData graphData) {
        DoubleTimeSeriesView timeSeriesView = new DefaultDoubleTimeSeriesView(graphData.getTimestamps(), graphData.getValues());

        Assert.assertEquals(expected, DoubleAggregateFunctions.integrate(timeSeriesView), 0);
    }

    @Test
    public void integrate() {
        testIntegrate(0, GraphData.empty);
        testIntegrate(0, new GraphData(new long[]{10000}, new double[]{20}, SortedOrCheck.CHECK));
        testIntegrate(5, new GraphData(new long[]{10000, 11000}, new double[]{3, 5}, SortedOrCheck.CHECK));
        testIntegrate(9, new GraphData(new long[]{10000, 11000, 12000, 13000}, new double[]{3, 3, 3, 3}, SortedOrCheck.CHECK));
        testIntegrate(3, new GraphData(new long[]{10000, 12000, 13000}, new double[]{3, Double.NaN, 3}, SortedOrCheck.CHECK));
    }
}
