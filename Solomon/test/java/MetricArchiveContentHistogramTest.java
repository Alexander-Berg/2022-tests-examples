package ru.yandex.solomon.codec;

import org.junit.Test;

import ru.yandex.solomon.codec.archive.MetricArchiveImmutable;
import ru.yandex.solomon.codec.archive.MetricArchiveMutable;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.solomon.model.type.Histogram;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.solomon.util.CloseableUtils.close;

/**
 * @author Vladimir Gordiychuk
 */
public class MetricArchiveContentHistogramTest {
    private static AggrPoint point(String time, double[] bounds, long[] buckets) {
        return AggrPoint.builder()
                .time(time)
                .histogram(histogram(bounds, buckets))
                .build();
    }

    private static Histogram histogram(double[] bounds, long[] buckets) {
        return Histogram.newInstance(bounds, buckets);
    }

    @Test
    public void toMetricArchiveAndBackSameBounds() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point("2017-05-10T01:00:00Z", new double[]{10, 20, 30}, new long[]{1, 2, 3}),
                point("2017-05-10T02:00:00Z", new double[]{10, 20, 30}, new long[]{4, 5, 6}),
                point("2017-05-10T03:10:00Z", new double[]{10, 20, 30}, new long[]{7, 8, 9}),
                point("2017-05-10T04:11:00Z", new double[]{10, 20, 30}, new long[]{7, 8, 9}),
                point("2017-05-10T05:15:00Z", new double[]{10, 20, 30}, new long[]{9, 9, 9})
        );

        MetricArchiveImmutable content = MetricArchiveImmutable.of(source);
        assertThat(content.columnSetMask(), equalTo(source.columnSetMask()));

        AggrGraphDataArrayList result = content.toAggrGraphDataArrayList();
        assertThat(result, equalTo(source));
        close(content);
    }

    @Test
    public void toMetricsArchiveAndBackDifferentBounds() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point("2017-05-10T01:00:00Z", new double[]{10, 20, 30}, new long[]{1, 2, 3}),
                point("2017-05-10T02:00:00Z", new double[]{10, 20, 30}, new long[]{4, 5, 6}),
                point("2017-05-10T03:10:00Z", new double[]{10, 20, 30}, new long[]{7, 8, 9}),
                point("2017-05-10T04:11:00Z", new double[]{10, 15, 20, 30}, new long[]{7, 2, 9, 10}),
                point("2017-05-10T05:15:00Z", new double[]{10, 15, 20, 30}, new long[]{8, 4, 10, 15})
        );

        MetricArchiveImmutable content = MetricArchiveImmutable.of(source);
        assertThat(content.columnSetMask(), equalTo(source.columnSetMask()));

        AggrGraphDataArrayList result = content.toAggrGraphDataArrayList();
        assertThat(result, equalTo(source));
        close(content);
    }

    @Test
    public void sortAndMerge() {
        MetricArchiveMutable mContent = new MetricArchiveMutable();
        mContent.addRecord(point("2017-05-10T03:10:00Z", new double[]{10, 20, 30}, new long[]{1, 2, 3}));
        mContent.addRecord(point("2017-05-10T03:20:00Z", new double[]{10, 20, 30}, new long[]{3, 4, 5}));
        mContent.addRecord(point("2017-05-10T01:00:00Z", new double[]{10, 20, 30}, new long[]{6, 7, 8}));
        mContent.addRecord(point("2017-05-10T03:10:00Z", new double[]{10, 20, 30}, new long[]{9, 10, 11}));
        MetricArchiveImmutable iContent = mContent.toImmutable();

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point("2017-05-10T01:00:00Z", new double[]{10, 20, 30}, new long[]{6, 7, 8}),
                point("2017-05-10T03:10:00Z", new double[]{10, 20, 30}, new long[]{9, 10, 11}), // latest win
                point("2017-05-10T03:20:00Z", new double[]{10, 20, 30}, new long[]{3, 4, 5})
        );

        AggrGraphDataArrayList result = iContent.toAggrGraphDataArrayList();
        assertThat(result, equalTo(expected));
        close(mContent, iContent);
    }
}
