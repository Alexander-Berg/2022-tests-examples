package ru.yandex.solomon.model.protobuf;

import java.util.DoubleSummaryStatistics;
import java.util.LongSummaryStatistics;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableLong;
import org.junit.Test;

import ru.yandex.monlib.metrics.summary.ImmutableSummaryDoubleSnapshot;
import ru.yandex.monlib.metrics.summary.ImmutableSummaryInt64Snapshot;
import ru.yandex.monlib.metrics.summary.SummaryDoubleSnapshot;
import ru.yandex.monlib.metrics.summary.SummaryInt64Snapshot;

import static org.junit.Assert.assertEquals;

/**
 * @author Vladimir Gordiychuk
 */
public class SummaryConverterTest {

    @Test
    public void summaryInt64() {
        MutableLong last = new MutableLong();
        LongSummaryStatistics data = LongStream.generate(() -> ThreadLocalRandom.current().nextLong())
            .limit(ThreadLocalRandom.current().nextInt(1, 100))
            .peek(last::add)
            .summaryStatistics();

        SummaryInt64Snapshot snapshot =
            new ImmutableSummaryInt64Snapshot(data.getCount(), data.getSum(), data.getMax(), data.getMin(), last.longValue());

        SummaryInt64 proto = SummaryConverter.toProto(snapshot);
        SummaryInt64Snapshot result = SummaryConverter.fromProto(proto);
        assertEquals(snapshot, result);
    }

    @Test
    public void summaryDouble() {
        MutableDouble last = new MutableDouble();
        DoubleSummaryStatistics data = DoubleStream.generate(() -> ThreadLocalRandom.current().nextDouble())
            .limit(ThreadLocalRandom.current().nextInt(1, 100))
            .peek(last::add)
            .summaryStatistics();

        SummaryDoubleSnapshot snapshot =
            new ImmutableSummaryDoubleSnapshot(data.getCount(), data.getSum(), data.getMax(), data.getMin(), last.doubleValue());

        SummaryDouble proto = SummaryConverter.toProto(snapshot);
        SummaryDoubleSnapshot result = SummaryConverter.fromProto(proto);
        assertEquals(snapshot, result);
    }
}
