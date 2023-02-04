package ru.yandex.solomon.math.operation.comparation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.junit.Test;

import ru.yandex.monlib.metrics.summary.ImmutableSummaryDoubleSnapshot;
import ru.yandex.monlib.metrics.summary.SummaryDoubleSnapshot;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public class SummaryDoubleComparatorTest {
    private static SummaryDoubleSnapshot summaryDouble(double... values) {
        DoubleSummaryStatistics summary = DoubleStream.of(values).summaryStatistics();
        return new ImmutableSummaryDoubleSnapshot(summary.getCount(), summary.getSum(), summary.getMin(), summary.getMax());
    }

    @Test
    public void compareEmpty() {
        int result = compare(summaryDouble(), summaryDouble());
        assertThat(result, equalTo(0));
    }

    @Test
    public void compareSame() {
        SummaryDoubleSnapshot a = summaryDouble(1, 2, 3);
        SummaryDoubleSnapshot b = summaryDouble(1, 2, 3);

        assertThat(compare(a, b), equalTo(0));
        assertThat(compare(b, a), equalTo(0));
    }

    @Test
    public void compareOneDiffSum() {
        SummaryDoubleSnapshot a = summaryDouble(4);
        SummaryDoubleSnapshot b = summaryDouble(42);

        assertThat(compare(a, b), equalTo(-1));
        assertThat(compare(b, a), equalTo(1));
    }

    @Test
    public void sameSumDiffCount() {
        SummaryDoubleSnapshot a = summaryDouble(21, 21);
        SummaryDoubleSnapshot b = summaryDouble(42);

        assertThat(compare(a, b), equalTo(-1));
        assertThat(compare(b, a), equalTo(1));
    }

    @Test
    public void sameCountSumDiffMaxMin() {
        SummaryDoubleSnapshot a = summaryDouble(10, 10, 10);
        SummaryDoubleSnapshot b = summaryDouble(5, 10, 15);

        assertThat(compare(a, b), equalTo(-1));
        assertThat(compare(b, a), equalTo(1));
    }

    @Test
    public void compareContract() {
        List<SummaryDoubleSnapshot> source = IntStream.range(0, 10000)
                .parallel()
                .mapToObj(index -> {
                    DoubleSummaryStatistics summary =
                            DoubleStream.generate(() -> ThreadLocalRandom.current().nextDouble(-100, 100))
                                    .limit(ThreadLocalRandom.current().nextInt(1, 100))
                                    .summaryStatistics();

                    return new ImmutableSummaryDoubleSnapshot(summary.getCount(), summary.getSum(), summary.getMin(), summary.getMax());
                })
                .sorted(new SummaryDoubleComparator())
                .collect(Collectors.toList());

        List<SummaryDoubleSnapshot> shuffled = new ArrayList<>(source);
        Collections.shuffle(shuffled);
        shuffled.sort(new SummaryDoubleComparator());
        assertThat(shuffled, equalTo(source));
    }

    private int compare(SummaryDoubleSnapshot left, SummaryDoubleSnapshot right) {
        return new SummaryDoubleComparator().compare(left, right);
    }
}
