package ru.yandex.metabase.client;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Egor Litvinenko
 */
public class MetabasePartitionsTest {

    @Test
    public void zero() {
        IntStream.range(1, 1_000).forEach(i -> {
            Assert.assertEquals(0, MetabasePartitions.toPartition(0, i));
        });
    }

    @Test
    public void one() {
        IntStream.range(1, 1_000).forEach(i -> {
            Assert.assertEquals("failed on " + i, 0, MetabasePartitions.toPartition(1, i));
        });
    }

    @Test
    public void testRange() {
        IntStream.rangeClosed(1, 32).map(partitions -> {
            final long max = 1l << 32;
            final long step = max / partitions;
            final long[] lowerBounds = LongStream.iterate(0, x -> x + step).limit(partitions).toArray();

            IntStream.range(1, 1_000).forEach(i -> {
                final long hash = Integer.toUnsignedLong(ThreadLocalRandom.current().nextInt());
                final int pos = Arrays.binarySearch(lowerBounds, hash);
                final int partition = pos >= 0 ? pos : -(pos + 1) - 1;
                Assert.assertEquals("failed on " + i, partition, MetabasePartitions.toPartition(hash, partitions));
            });
            return partitions;
        }).count();
    }

    @Test
    public void testRandom() {
        IntStream.rangeClosed(1, 32).map(partitions -> {
            final long max = 1l << 32;
            final long step = max / partitions;
            final long[] lowerBounds = LongStream.iterate(0, x -> x + step).limit(partitions).toArray();
            final long hash = Integer.toUnsignedLong(ThreadLocalRandom.current().nextInt());
            final int partition = -(Arrays.binarySearch(lowerBounds, hash) + 1) - 1;
            Assert.assertEquals(partition, MetabasePartitions.toPartition(hash, partitions));
            return partition;
        });
    }

    @Test
    public void testPowerOfTwo() {
        LongStream.rangeClosed(1, 32).forEach(i -> {
            Assert.assertEquals((int) i, MetabasePartitions.calcPowerOfTwo(1l << i));
        });
    }

}
