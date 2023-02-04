package ru.yandex.solomon.model.timeseries.decim;

import org.junit.Test;

import ru.yandex.monlib.metrics.summary.ImmutableSummaryDoubleSnapshot;
import ru.yandex.monlib.metrics.summary.SummaryDoubleSnapshot;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.protobuf.MetricType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Vladimir Gordiychuk
 */
public class DecimPointValueCollectorSummaryDoubleTest {
    private static SummaryDoubleSnapshot decim(SummaryDoubleSnapshot... summaries) {
        DecimPointValueCollector collector = DecimPointValueCollector.of(MetricType.DSUMMARY);
        AggrPoint temp = new AggrPoint();
        for (SummaryDoubleSnapshot summary : summaries) {
            temp.setSummaryDouble(summary);
            collector.append(temp);
        }

        collector.compute(temp);
        return temp.summaryDouble;
    }

    private static SummaryDoubleSnapshot summary(long count, double sum, double max, double min) {
        return new ImmutableSummaryDoubleSnapshot(count, sum, min, max);
    }

    @Test
    public void decimEmpty() {
        SummaryDoubleSnapshot result = decim();
        assertThat(result, nullValue());
    }

    @Test
    public void decimOne() {
        SummaryDoubleSnapshot one = summary(10, 100, 50, 2);
        SummaryDoubleSnapshot result = decim(one);
        assertThat(result, equalTo(one));
    }

    @Test
    public void decimTwo() {
        SummaryDoubleSnapshot one = summary(10, 100, 50, 2);
        SummaryDoubleSnapshot two = summary(15, 125, 50, 1);

        SummaryDoubleSnapshot result = decim(one, two);
        assertThat(result, equalTo(summary(25, 225, 50, 1)));
    }

    @Test
    public void decimMany() {
        SummaryDoubleSnapshot one = summary(10, 100, 50, 2);
        SummaryDoubleSnapshot two = summary(15, 125, 50, 1);
        SummaryDoubleSnapshot tree = summary(40, 241, 53, 1);

        SummaryDoubleSnapshot result = decim(one, two, tree);
        assertThat(result, equalTo(summary(65, 466, 53, 1)));
    }
}
