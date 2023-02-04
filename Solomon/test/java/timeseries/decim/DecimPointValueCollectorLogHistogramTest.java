package ru.yandex.solomon.model.timeseries.decim;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.protobuf.MetricType;
import ru.yandex.solomon.model.type.LogHistogram;

/**
 * @author Vladimir Gordiychuk
 */
public class DecimPointValueCollectorLogHistogramTest {

    private static LogHistogram decim(LogHistogram... histograms) {
        DecimPointValueCollector collector = DecimPointValueCollector.of(MetricType.LOG_HISTOGRAM);
        AggrPoint temp = new AggrPoint();
        for (LogHistogram histogram : histograms) {
            temp.setLogHistogram(histogram);
            collector.append(temp);
        }

        collector.compute(temp);
        return temp.logHistogram;
    }

    @Test
    public void decimSingleHistogram() {
        LogHistogram source = LogHistogram.newBuilder()
                .setBuckets(new double[]{1, 1, 2})
                .setCountZero(10)
                .setStartPower(21)
                .build();

        LogHistogram result = decim(source);

        Assert.assertThat(result, CoreMatchers.equalTo(source));
    }

    @Test
    public void decimWithDiffSize() {
        LogHistogram result = decim(
                LogHistogram.ofBuckets(3, 1, 2, 3, 5),
                LogHistogram.ofBuckets(3, 5),
                LogHistogram.ofBuckets(8, 9, 1)
        );

        LogHistogram expected = LogHistogram.ofBuckets((3 + 3 + 8), (1 + 5 + 9), (2 + 1), 3, 5);
        Assert.assertThat(result, CoreMatchers.equalTo(expected));
    }

    @Test
    public void decimSumCountZero() {
        LogHistogram result = decim(
                LogHistogram.newBuilder().setBuckets(new double[]{1, 2, 3}).setCountZero(10).build(),
                LogHistogram.newBuilder().setBuckets(new double[]{1, 2, 3}).setCountZero(20).build(),
                LogHistogram.newBuilder().setBuckets(new double[]{1, 2, 3}).setCountZero(30).build()
        );

        LogHistogram expected = LogHistogram.newBuilder()
                .setBuckets(new double[]{3, 6, 9})
                .setCountZero(10 + 20 + 30)
                .build();

        Assert.assertThat(result, CoreMatchers.equalTo(expected));
    }

    @Test
    public void decimWithDiffStartPower() {
        LogHistogram one = LogHistogram.newBuilder()
                .setBuckets(new double[]{2, 3, 4})
                .setStartPower(1)
                .build();

        LogHistogram two = LogHistogram.newBuilder()
                .setBuckets(new double[]{10, 20, 30, 40})
                .setStartPower(-2)
                .build();

        LogHistogram tree = LogHistogram.newBuilder()
                .setBuckets(new double[]{10, 20, 30, 40, 50})
                .setStartPower(0)
                .build();

        LogHistogram result = decim(one, two, tree);

        LogHistogram expected = LogHistogram.newBuilder()
                .setBuckets(new double[]{3, 6, 9})
                .setBuckets(new double[]{10, 20, ((30) + 10), ((40 + 2) + 20), ((3) + 30), ((4) + 40), 50})
                .setStartPower(-2)
                .build();

        Assert.assertThat(result, CoreMatchers.equalTo(expected));
    }
}
