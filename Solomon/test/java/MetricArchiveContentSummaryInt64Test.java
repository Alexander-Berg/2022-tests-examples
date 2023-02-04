package ru.yandex.solomon.codec;

import org.junit.Test;

import ru.yandex.monlib.metrics.summary.ImmutableSummaryInt64Snapshot;
import ru.yandex.monlib.metrics.summary.SummaryInt64Snapshot;
import ru.yandex.solomon.codec.archive.MetricArchiveImmutable;
import ru.yandex.solomon.codec.archive.MetricArchiveMutable;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.solomon.util.CloseableUtils.close;

/**
 * @author Vladimir Gordiychuk
 */
public class MetricArchiveContentSummaryInt64Test {
    private static AggrPoint point(String time, SummaryInt64Snapshot summary) {
        return AggrPoint.builder()
                .time(time)
                .summary(summary)
                .build();
    }

    private static SummaryInt64Snapshot summary(long count, long sum, long min, long max) {
        return new ImmutableSummaryInt64Snapshot(count, sum, min, max);
    }

    @Test
    public void toMetricArchiveAndBack() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point("2017-05-10T01:00:00Z", summary(1, 1, 1, 1)),
                point("2017-05-10T02:00:00Z", summary(2, 2, 1, 1)),
                point("2017-05-10T03:10:00Z", summary(3, 2, 0, 1)),
                point("2017-05-10T04:11:00Z", summary(4, 5, 0, 2)),
                point("2017-05-10T05:15:00Z", summary(4, 5, 0, 2)),
                point("2017-05-10T06:00:00Z", summary(123, 10421, 0, 1000))
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
        mContent.addRecord(point("2017-05-10T03:10:00Z", summary(3, 2, 0, 1)));
        mContent.addRecord(point("2017-05-10T03:20:00Z", summary(123, 10421, 0, 1000)));
        mContent.addRecord(point("2017-05-10T01:00:00Z", summary(3, 2, 0, 1)));
        mContent.addRecord(point("2017-05-10T03:10:00Z", summary(4, 5, 0, 2)));
        MetricArchiveImmutable iContent = mContent.toImmutable();

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point("2017-05-10T01:00:00Z", summary(3, 2, 0, 1)),
                point("2017-05-10T03:10:00Z", summary(4, 5, 0, 2)), // latest win
                point("2017-05-10T03:20:00Z", summary(123, 10421, 0, 1000))
        );

        AggrGraphDataArrayList result = iContent.toAggrGraphDataArrayList();
        assertThat(result, equalTo(expected));
        close(mContent, iContent);
    }
}
