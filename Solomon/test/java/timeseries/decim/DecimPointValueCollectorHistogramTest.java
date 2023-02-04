package ru.yandex.solomon.model.timeseries.decim;

import org.junit.Test;

import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.protobuf.MetricType;
import ru.yandex.solomon.model.type.Histogram;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Vladimir Gordiychuk
 */
public class DecimPointValueCollectorHistogramTest {
    private static Histogram decim(Histogram... histograms) {
        DecimPointValueCollector collector = DecimPointValueCollector.of(MetricType.HIST);
        AggrPoint temp = new AggrPoint();
        for (var histogram : histograms) {
            temp.setHistogram(histogram);
            collector.append(temp);
        }

        temp.histogram = null;
        collector.compute(temp);
        return temp.histogram;
    }

    private static Histogram histogram(double[] bounds, long[] buckets) {
        return Histogram.newInstance(bounds, buckets);
    }

    @Test
    public void decimEmpty() {
        var result = decim();
        assertThat(result, nullValue());
    }

    @Test
    public void decimOne() {
        var one = histogram(new double[]{10, 30, 50}, new long[]{1, 2, 3});
        var result = decim(one);
        assertThat(result, equalTo(one));
    }

    @Test
    public void decimTwoSameBounds() {
        var one = histogram(new double[]{10, 30, 50}, new long[]{1, 2, 3});
        var two = histogram(new double[]{10, 30, 50}, new long[]{3, 5, 0});

        var expected = histogram(new double[]{10, 30, 50}, new long[]{4, 7, 3});
        var result = decim(one, two);
        assertThat(result, equalTo(expected));
    }

    @Test
    public void decimManySameBounds() {
        var one = histogram(new double[]{10, 30, 50}, new long[]{1, 2, 3});
        var two = histogram(new double[]{10, 30, 50}, new long[]{3, 5, 0});
        var tree = histogram(new double[]{10, 30, 50}, new long[]{10, 50, 42});

        var expected = histogram(
                new double[]{10, 30, 50},
                new long[]{1 + 3 + 10, 2 + 5 + 50, 3 + 42});

        var result = decim(one, two, tree);
        assertThat(result, equalTo(expected));
    }

    @Test
    public void decimTwoDifferentBounds() {
        var one = histogram(
                new double[]{10, 30, 50},
                new long[]{1, 2, 3});
        var two = histogram(
                new double[]{10, 20, 30, 40, 50},
                new long[]{1, 2, 3, 4, 5});

        var expected = histogram(
                new double[]{10, 20, 30, 40, 50},
                new long[]{1 + 1, 2 + 1, 3 + 1, 4 + 2, 5 + 2});

        var result = decim(one, two);
        assertThat(result, equalTo(expected));
    }

    @Test
    public void decimTreeDifferentBounds() {
        var one = histogram(
                new double[]{10, 30, 50},
                new long[]{1, 2, 3});
        var two = histogram(
                new double[]{10, 20, 30, 40, 50},
                new long[]{1, 2, 3, 4, 5});
        var tree = histogram(
                new double[]{10, 15, 20, 30, 40, 50},
                new long[]{5, 10, 3, 0, 0, 1});
        var four = Histogram.newInstance();

        var expected = histogram(
                new double[]{10, 15, 20, 30, 40, 50},
                new long[]{
                        1 + 1 + 5, // 10
                        10 + 2,        // 15
                        2 + 3,     // 20
                        3 + 1,     // 30
                        4 + 2,         // 40
                        5 + 3  // 50
                });

        var result = decim(one, two, tree, four);
        assertThat(result, equalTo(expected));
    }
}
