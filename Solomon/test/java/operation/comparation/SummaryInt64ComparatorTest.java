package ru.yandex.solomon.math.operation.comparation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.junit.Test;

import ru.yandex.monlib.metrics.summary.ImmutableSummaryInt64Snapshot;
import ru.yandex.monlib.metrics.summary.SummaryInt64Snapshot;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public class SummaryInt64ComparatorTest {
    private static SummaryInt64Snapshot summary(long... values) {
        LongSummaryStatistics summary = LongStream.of(values).summaryStatistics();
        return new ImmutableSummaryInt64Snapshot(summary.getCount(), summary.getSum(), summary.getMin(), summary.getMax());
    }

    @Test
    public void compareEmpty() {
        int result = compare(summary(), summary());
        assertThat(result, equalTo(0));
    }

    @Test
    public void compareSame() {
        SummaryInt64Snapshot a = summary(1, 2, 3);
        SummaryInt64Snapshot b = summary(1, 2, 3);

        assertThat(compare(a, b), equalTo(0));
        assertThat(compare(b, a), equalTo(0));
    }

    @Test
    public void compareOneDiffSum() {
        SummaryInt64Snapshot a = summary(4);
        SummaryInt64Snapshot b = summary(42);

        assertThat(compare(a, b), equalTo(-1));
        assertThat(compare(b, a), equalTo(1));
    }

    @Test
    public void sameSumDiffCount() {
        SummaryInt64Snapshot a = summary(21, 21);
        SummaryInt64Snapshot b = summary(42);

        assertThat(compare(a, b), equalTo(-1));
        assertThat(compare(b, a), equalTo(1));
    }

    @Test
    public void sameCountSumDiffMaxMin() {
        SummaryInt64Snapshot a = summary(10, 10, 10);
        SummaryInt64Snapshot b = summary(5, 10, 15);

        assertThat(compare(a, b), equalTo(-1));
        assertThat(compare(b, a), equalTo(1));
    }

    @Test
    public void compareContract() {
        List<SummaryInt64Snapshot> source = IntStream.range(0, 10000)
                .parallel()
                .mapToObj(index -> {
                    LongSummaryStatistics summary =
                            LongStream.generate(() -> ThreadLocalRandom.current().nextLong(-100, 100))
                                    .limit(ThreadLocalRandom.current().nextInt(1, 100))
                                    .summaryStatistics();

                    return new ImmutableSummaryInt64Snapshot(summary.getCount(), summary.getSum(), summary.getMin(), summary.getMax());
                })
                .sorted(new SummaryInt64Comparator())
                .collect(Collectors.toList());

        List<SummaryInt64Snapshot> shuffled = new ArrayList<>(source);
        Collections.shuffle(shuffled);
        shuffled.sort(new SummaryInt64Comparator());
        assertThat(shuffled, equalTo(source));
    }

    private int compare(SummaryInt64Snapshot left, SummaryInt64Snapshot right) {
        return new SummaryInt64Comparator().compare(left, right);
    }
}
