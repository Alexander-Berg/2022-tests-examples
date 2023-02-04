package ru.yandex.solomon.codec;

import java.time.Instant;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.codec.archive.MetricArchiveImmutable;
import ru.yandex.solomon.codec.archive.MetricArchiveMutable;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.solomon.model.type.LogHistogram;

import static ru.yandex.solomon.util.CloseableUtils.close;

/**
 * @author Vladimir Gordiychuk
 */
public class MetricArchiveContentLogHistogramTest {

    @Test
    public void toMetricArchiveBuilderAndBack() throws Exception {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
            point("2017-05-10T01:00:00Z", LogHistogram.ofBuckets(1, 2, 3)),
            point("2017-05-10T02:00:00Z", LogHistogram.ofBuckets(3, 2, 1)),
            point("2017-05-10T03:10:00Z", LogHistogram.ofBuckets(1, 1, 1)),
            point("2017-05-10T04:11:00Z", LogHistogram.ofBuckets(2, 2, 2)),
            point("2017-05-10T05:15:00Z", LogHistogram.ofBuckets(3, 3, 3))
        );

        MetricArchiveMutable content = MetricArchiveMutable.of(source);
        AggrGraphDataArrayList result = content.toAggrGraphDataArrayList();

        Assert.assertThat(result, CoreMatchers.equalTo(source));
        close(content);
    }

    @Test
    public void toMetricArchiveBuilderAndBack2() throws Exception {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
            AggrPoint.shortPoint(timeMillis("2017-05-10T01:00:00Z"), 1),
            AggrPoint.shortPoint(timeMillis("2017-05-10T02:00:00Z"), 2),
            AggrPoint.shortPoint(timeMillis("2017-05-10T03:00:00Z"), 3)
        );

        MetricArchiveMutable content = MetricArchiveMutable.of(source);
        AggrGraphDataArrayList result = content.toAggrGraphDataArrayList();

        Assert.assertThat(result, CoreMatchers.equalTo(source));
        close(content);
    }

    @Test
    public void serializeAndDeserialize() throws Exception {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
            point("2017-05-10T01:00:00Z", LogHistogram.ofBuckets(1, 2, 3)),
            point("2017-05-10T02:00:00Z", LogHistogram.ofBuckets(1, 2, 3)),
            point("2017-05-10T03:00:00Z", LogHistogram.ofBuckets(1, 3, 3)),
            point("2017-05-10T04:00:00Z", LogHistogram.ofBuckets(1, 4, 3)),
            point("2017-05-10T05:00:00Z", LogHistogram.ofBuckets(1, 4, 4)),
            point("2017-05-10T06:00:00Z", LogHistogram.ofBuckets(2, 1, 1)),
            point("2017-05-10T07:00:00Z", LogHistogram.ofBuckets(1, 2, 1))
        );

        MetricArchiveImmutable serialized = MetricArchiveImmutable.of(source);
        AggrGraphDataArrayList result = serialized.toAggrGraphDataArrayList();

        Assert.assertThat(result, CoreMatchers.equalTo(source));
        close(serialized);
    }

    private static AggrPoint point(String time, LogHistogram histogram) {
        return AggrPoint.shortPoint(timeMillis(time), histogram);
    }

    private static long timeMillis(String time) {
        return Instant.parse(time).toEpochMilli();
    }
}
