package ru.yandex.solomon.math;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.model.timeseries.SortedOrCheck;

/**
 * @author Vladimir Gordiychuk
 */
public class GraphDataIntegrateTest {

    @Test
    public void trapezoidalIntegrateC() {
        Assert.assertEquals(GraphData.empty, GraphDataIntegrate.trapezoidalIntegrateC(GraphData.empty, 0));
        Assert.assertEquals(GraphData.empty, GraphDataIntegrate.trapezoidalIntegrateC(new GraphData(new long[] { 1000 }, new double[] { 10 }, SortedOrCheck.CHECK), 0));

        {
            GraphData orig = new GraphData(new long[]{ 1000, 3000, 4000 }, new double[]{ 3, 9, 10 }, SortedOrCheck.CHECK);
            GraphData expe = new GraphData(new long[] { 3000, 4000 }, new double[] { 12, 21.5 }, SortedOrCheck.CHECK);
            Assert.assertEquals(expe, GraphDataIntegrate.trapezoidalIntegrateC(orig, 0));
        }
    }

    @Test
    public void rightRectangleIntegrate() {
        Assert.assertEquals(GraphData.empty, GraphDataIntegrate.rightRectangleIntegrate(GraphData.empty));

        {
            GraphData orig = new GraphData(new long[] { 1000 }, new double[] { 42 }, SortedOrCheck.CHECK);
            GraphData expe = new GraphData(new long[] { 1000 }, new double[] { 0 }, SortedOrCheck.CHECK);
            Assert.assertEquals(expe, GraphDataIntegrate.rightRectangleIntegrate(orig));
        }

        {
            GraphData orig = new GraphData(new long[] { 1000, 3000 }, new double[] { 42, 5 }, SortedOrCheck.CHECK);
            GraphData expe = new GraphData(new long[] { 1000, 3000 }, new double[] { 0, 10 }, SortedOrCheck.CHECK);
            Assert.assertEquals(expe, GraphDataIntegrate.rightRectangleIntegrate(orig));
        }

        {
            GraphData orig = new GraphData(new long[] { 1000, 3000, 4000 }, new double[] { 3, 9, 10 }, SortedOrCheck.CHECK);
            GraphData expe = new GraphData(new long[] { 1000, 3000, 4000 }, new double[] { 0, 18, 28 }, SortedOrCheck.CHECK);
            Assert.assertEquals(expe, GraphDataIntegrate.rightRectangleIntegrate(orig));
        }

        {
            long[] ts = new long[20];
            double[] vals = new double[20];

            ts[0] = 1000;
            vals[0] = 42;
            for (int i = 1; i < 20; i++) {
                ts[i] = ts[i - 1] + 1000 * ThreadLocalRandom.current().nextInt(1, 5);
                vals[i] = ThreadLocalRandom.current().nextInt(0, 10);
            }

            GraphData orig = new GraphData(ts, vals, SortedOrCheck.CHECK);
            GraphData antiderivative = GraphDataIntegrate.rightRectangleIntegrate(orig);
            GraphData same = antiderivative.deriv();

            Assert.assertEquals(GraphDataMath.dropHead(orig, 1), same);
        }
    }
}
