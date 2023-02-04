package ru.yandex.solomon.model.timeseries;

import org.junit.Test;

import ru.yandex.solomon.model.protobuf.MetricType;
import ru.yandex.solomon.model.type.Histogram;
import ru.yandex.solomon.model.type.LogHistogram;

import static org.junit.Assert.assertEquals;
import static ru.yandex.solomon.model.point.AggrPoints.point;

/**
 * @author Vladimir Gordiychuk
 */
public class MetricTypeTransfersFromLogHistogramTest extends MetricTypeTransfersTest {

    public MetricTypeTransfersFromLogHistogramTest() {
        super(MetricType.LOG_HISTOGRAM);
    }

    @Test
    public void toLogHistogram() {
        var source = listOf(
            point("2020-08-10T15:29:59Z", LogHistogram.ofBuckets(1, 2, 3)),
            point("2020-08-10T15:30:00Z", LogHistogram.ofBuckets(3, 4, 5)),
            point("2020-08-10T15:31:00Z", LogHistogram.ofBuckets(6, 7, 8)));

        var result = transferTo(MetricType.LOG_HISTOGRAM, source);
        assertEquals(source, result);
    }

    @Test
    public void toHistogram() {
        var source = listOf(
            point("2020-08-10T15:29:59Z", LogHistogram.ofBuckets(1, 2, 3)),
            point("2020-08-10T15:30:00Z", LogHistogram.ofBuckets(3, 4, 5)),
            point("2020-08-10T15:31:00Z", LogHistogram.ofBuckets(6, 7, 8)),
            point("2020-08-10T15:35:00Z", LogHistogram.ofBuckets(1)));

        var expected = listOf(
            point("2020-08-10T15:29:59Z", Histogram.newInstance(new double[]{1, 1.5, 2.25, 3.375}, new long[]{0, 1, 2, 3})),
            point("2020-08-10T15:30:00Z", Histogram.newInstance(new double[]{1, 1.5, 2.25, 3.375}, new long[]{0, 3, 4, 5})),
            point("2020-08-10T15:31:00Z", Histogram.newInstance(new double[]{1, 1.5, 2.25, 3.375}, new long[]{0, 6, 7, 8})),
            point("2020-08-10T15:35:00Z", Histogram.newInstance(new double[]{1, 1.5}, new long[]{0, 1}))
        );

        var result = transferTo(MetricType.HIST, source);
        assertEquals(expected, result);
    }
}
