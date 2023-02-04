package ru.yandex.solomon.model.timeseries.decim;

import org.junit.Test;

import ru.yandex.monlib.metrics.summary.ImmutableSummaryInt64Snapshot;
import ru.yandex.monlib.metrics.summary.SummaryInt64Snapshot;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.protobuf.MetricType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Vladimir Gordiychuk
 */
public class DecimPointValueCollectorSummaryInt64Test {
    private static SummaryInt64Snapshot decim(SummaryInt64Snapshot... summaries) {
        DecimPointValueCollector collector = DecimPointValueCollector.of(MetricType.ISUMMARY);
        AggrPoint temp = new AggrPoint();
        for (SummaryInt64Snapshot summary : summaries) {
            temp.setSummaryInt64(summary);
            collector.append(temp);
        }

        collector.compute(temp);
        return temp.summaryInt64;
    }

    private static SummaryInt64Snapshot summary(long count, long sum, long max, long min) {
        return new ImmutableSummaryInt64Snapshot(count, sum, min, max);
    }

    @Test
    public void decimEmpty() {
        SummaryInt64Snapshot result = decim();
        assertThat(result, nullValue());
    }

    @Test
    public void decimOne() {
        SummaryInt64Snapshot one = summary(10, 100, 50, 2);
        SummaryInt64Snapshot result = decim(one);
        assertThat(result, equalTo(one));
    }

    @Test
    public void decimTwo() {
        SummaryInt64Snapshot one = summary(10, 100, 50, 2);
        SummaryInt64Snapshot two = summary(15, 125, 50, 1);

        SummaryInt64Snapshot result = decim(one, two);
        var expected = summary(25, 225, 50, 1);
        assertThat(result, equalTo(expected));
    }

    @Test
    public void decimMany() {
        SummaryInt64Snapshot one = summary(10, 100, 50, 2);
        SummaryInt64Snapshot two = summary(15, 125, 50, 1);
        SummaryInt64Snapshot tree = summary(40, 241, 53, 1);

        SummaryInt64Snapshot result = decim(one, two, tree);
        assertThat(result, equalTo(summary(65, 466, 53, 1)));
    }
}
