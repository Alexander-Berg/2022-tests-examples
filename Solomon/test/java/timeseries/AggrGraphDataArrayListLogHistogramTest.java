package ru.yandex.solomon.model.timeseries;

import java.time.Instant;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.column.StockpileColumn;
import ru.yandex.solomon.model.type.LogHistogram;

/**
 * @author Vladimir Gordiychuk
 */
public class AggrGraphDataArrayListLogHistogramTest {
    @Test
    public void setAndGet() throws Exception {
        LogHistogram histogram = LogHistogram.ofBuckets(1, 5, 2);
        AggrPoint expectedPoint = point("2017-05-10T09:00:00Z", histogram);

        AggrGraphDataArrayList list = AggrGraphDataArrayList.of(expectedPoint);
        AggrPoint result = list.getAnyPoint(0);

        Assert.assertThat(result, CoreMatchers.equalTo(expectedPoint));
    }

    @Test
    public void listEqualWhenAllHistogramsEqual() throws Exception {
        AggrGraphDataArrayList first = AggrGraphDataArrayList.of(
            point("2017-05-10T09:00:00Z", LogHistogram.ofBuckets(1, 2, 3)),
            point("2017-05-10T10:00:00Z", LogHistogram.ofBuckets(3, 2, 1))
        );

        AggrGraphDataArrayList second = AggrGraphDataArrayList.of(
            point("2017-05-10T09:00:00Z", LogHistogram.ofBuckets(1, 2, 3)),
            point("2017-05-10T10:00:00Z", LogHistogram.ofBuckets(3, 2, 1))
        );

        Assert.assertThat(first, CoreMatchers.equalTo(second));
    }

    @Test
    public void listDiffHistogramNotEqual() throws Exception {
        AggrGraphDataArrayList first = AggrGraphDataArrayList.of(
            point("2017-05-10T09:00:00Z", LogHistogram.ofBuckets(1, 2, 3)),
            point("2017-05-10T10:00:00Z", LogHistogram.ofBuckets(3, 2, 1))
        );

        AggrGraphDataArrayList second = AggrGraphDataArrayList.of(
            point("2017-05-10T09:00:00Z", LogHistogram.ofBuckets(1, 2, 3)),
            point("2017-05-10T10:00:00Z", LogHistogram.ofBuckets(3, 3, 3))
        );

        Assert.assertThat(first, CoreMatchers.not(CoreMatchers.equalTo(second)));
    }

    @Test
    public void mask() throws Exception {
        AggrGraphDataArrayList list = AggrGraphDataArrayList.of(
            point("2017-05-10T09:00:00Z", LogHistogram.ofBuckets(1, 2, 3))
        );

        int expectMask = StockpileColumn.LOG_HISTOGRAM.mask() | StockpileColumn.TS.mask();
        Assert.assertThat(list.columnSetMask(), CoreMatchers.equalTo(expectMask));
    }

    @Test
    public void sortAndMergeSkipMerge() throws Exception {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
            point("2017-05-10T10:00:00Z", LogHistogram.ofBuckets(1, 2, 3)),
            point("2017-05-10T03:10:00Z", LogHistogram.ofBuckets(3, 2, 1)),
            point("2017-05-10T13:00:00Z", LogHistogram.ofBuckets(3, 2, 1))
        );

        source.sortAndMerge();

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
            point("2017-05-10T03:10:00Z", LogHistogram.ofBuckets(3, 2, 1)),
            point("2017-05-10T10:00:00Z", LogHistogram.ofBuckets(1, 2, 3)),
            point("2017-05-10T13:00:00Z", LogHistogram.ofBuckets(3, 2, 1))
        );

        Assert.assertThat(source, CoreMatchers.equalTo(expected));
    }

    @Test
    public void sortAndMerge() throws Exception {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
            point("2017-05-10T03:10:00Z", LogHistogram.ofBuckets(1, 2, 3)),
            point("2017-05-10T10:10:00Z", LogHistogram.ofBuckets(2, 2, 2)),
            point("2017-05-10T03:10:00Z", LogHistogram.ofBuckets(3, 2, 1)),
            point("2017-05-10T03:10:00Z", LogHistogram.ofBuckets(4, 2, 8))
        );

        source.sortAndMerge();

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
            point("2017-05-10T03:10:00Z", LogHistogram.ofBuckets(4, 2, 8)),
            point("2017-05-10T10:10:00Z", LogHistogram.ofBuckets(2, 2, 2))
        );

        Assert.assertThat(source, CoreMatchers.equalTo(expected));
    }

    private static AggrPoint point(String time, LogHistogram histogram) {
        return AggrPoint.shortPoint(timeMillis(time), histogram);
    }

    private static long timeMillis(String time) {
        return Instant.parse(time).toEpochMilli();
    }
}
