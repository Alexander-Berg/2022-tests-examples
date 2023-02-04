package ru.yandex.solomon.math;

import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.model.timeseries.SortedOrCheck;
import ru.yandex.solomon.model.timeseries.Timeline;

/**
 * @author Vladimir Gordiychuk
 */
public class ResamplerTest {
    private void testOp(long[] origTimeline, double[] origValues, long[] newTimeline, double[] newValues, Function<GraphData, GraphData> tr) {
        GraphData orig = new GraphData(new Timeline(origTimeline, SortedOrCheck.CHECK), origValues);
        GraphData expected = new GraphData(new Timeline(newTimeline, SortedOrCheck.CHECK), newValues);
        Assert.assertEquals(expected, tr.apply(orig));
    }

    private void testResample(long[] origTimeline, double[] origValues, long[] newTimeline, double[] newValues) {
        testOp(origTimeline, origValues, newTimeline, newValues,
            orig -> Resampler.resample(orig, new Timeline(newTimeline, SortedOrCheck.CHECK), Interpolate.LINEAR));
    }

    @Test
    public void resample() {
        {
            GraphData graphData = new GraphData(
                new long[]{ 100, 200, 300, 400 },
                new double[]{ 1, 3, 2, 4 },
                SortedOrCheck.CHECK);
            Assert.assertEquals(graphData, Resampler.resample(graphData, graphData.getTimeline(), Interpolate.LINEAR));
        }

        // simple average
        testResample(
            new long[] { 100, 200 }, new double[] { 1, 2 },
            new long[] { 150 }, new double[] { 1.5 });
        // skip point
        testResample(
            new long[] { 100, 200, 300 }, new double[] { 1, 3, 2 },
            new long[] { 100, 300 }, new double[] { 1, 2 });
        // outer points
        testResample(
            new long[] { 100, 200 }, new double[] { 1, 3 },
            new long[]{0, 50, 250, 300}, new double[]{Double.NaN, 1, 3, Double.NaN});
        // linear
        testResample(
            new long[] { 100, 400 }, new double[] { 1, 4 },
            new long[] { 100, 200, 300, 400 }, new double[] { 1, 2, 3, 4 });

        testResample(
            new long[]{}, new double[]{},
            new long[]{ 1000, 2000, 3000 }, new double[]{ Double.NaN, Double.NaN, Double.NaN });
    }
}
