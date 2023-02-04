package ru.yandex.metabase.client.impl;

import java.util.Arrays;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.labels.shard.ShardKey;

/**
 * @author Egor Litvinenko
 * */
public class MetabaseShardTest {

    @Test
    public void partitionStreamSimple() {
        var key = ShardKey.create("foo", "cluster", "service");
        var numId = 1;
        var genId = 0;
        var totalPartitions = 4;
        final MetabaseShard metabaseShard = new MetabaseShard(
                    System.nanoTime(),
                    key,
                    numId,
                    genId,
                    totalPartitions
                );
        metabaseShard.addPartition(genId, 0, "fqdn1", true, true);
        metabaseShard.addPartition(genId, 1, "fqdn2", false, true);
        metabaseShard.addPartition(genId, 2, "fqdn3", true, true);
        metabaseShard.addPartition(genId, 3, "fqdn4", false, true);
        int[] expectedPartitions = { 0, 2 };
        int[] actual = metabaseShard.partitionStream().toArray();
        Assert.assertTrue(Arrays.equals(expectedPartitions, actual));
    }

    @Test
    public void partitionStreamAll() {
        var key = ShardKey.create("foo", "cluster", "service");
        var numId = 1;
        var genId = 0;
        var totalPartitions = 4;
        final MetabaseShard metabaseShard = new MetabaseShard(
                System.nanoTime(),
                key,
                numId,
                genId,
                totalPartitions
        );
        metabaseShard.addPartition(genId, 0, "fqdn1", true, true);
        metabaseShard.addPartition(genId, 1, "fqdn2", true, true);
        metabaseShard.addPartition(genId, 2, "fqdn3", true, true);
        metabaseShard.addPartition(genId, 3, "fqdn4", true, true);
        int[] actual = metabaseShard.partitionStream().toArray();
        int[] expectedPartitions = { 0, 1, 2, 3 };
        Assert.assertTrue(Arrays.equals(expectedPartitions, actual));
    }

    @Test
    public void partitionStreamAllEquals() {
        var key = ShardKey.create("foo", "cluster", "service");
        var numId = 1;
        var genId = 0;
        var totalPartitions = 4;
        final MetabaseShard metabaseShard = new MetabaseShard(
                System.nanoTime(),
                key,
                numId,
                genId,
                totalPartitions
        );
        metabaseShard.addPartition(genId, 0, "fqdn1", true, true);
        metabaseShard.addPartition(genId, 1, "fqdn2", true, true);
        metabaseShard.addPartition(genId, 2, "fqdn3", true, true);
        metabaseShard.addPartition(genId, 3, "fqdn4", true, true);
        PartitionedShard full = metabaseShard;
        Assert.assertTrue(full == metabaseShard);
        int[] actual = full.partitionStream().toArray();
        int[] expectedPartitions = { 0, 1, 2, 3 };
        Assert.assertTrue(Arrays.equals(expectedPartitions, actual));
    }

    @Test
    public void partitionStreamOne() {
        var key = ShardKey.create("foo", "cluster", "service");
        var numId = 1;
        var genId = 0;
        var totalPartitions = 4;
        final MetabaseShard metabaseShard = new MetabaseShard(
                System.nanoTime(),
                key,
                numId,
                genId,
                totalPartitions
        );
        metabaseShard.addPartition(genId, 0, "fqdn1", false, true);
        metabaseShard.addPartition(genId, 1, "fqdn2", false, true);
        metabaseShard.addPartition(genId, 2, "fqdn3", true, true);
        metabaseShard.addPartition(genId, 3, "fqdn4", false, true);
        int[] actual = metabaseShard.partitionStream().toArray();
        int[] expectedPartitions = { 2 };
        Assert.assertTrue(Arrays.equals(expectedPartitions, actual));
    }

    @Test
    public void simpleMerge() {
        var key = ShardKey.create("foo", "cluster", "service");
        var numId = 1;
        var genId = 0;
        var totalPartitions = 4;
        final MetabaseShard left = new MetabaseShard(
                System.nanoTime(),
                key,
                numId,
                genId,
                totalPartitions
        );
        left.addPartition(genId, 2, "fqdn3", true, true);

        final MetabaseShard right = new MetabaseShard(
                System.nanoTime(),
                key,
                numId,
                genId,
                totalPartitions
        );
        right.addPartition(genId, 0, "fqdn1", true, true);
        right.addPartition(genId, 1, "fqdn2", true, true);
        right.addPartition(genId, 3, "fqdn4", true, true);

        var merged = MetabaseShard.mergeShardInfoFromServers(left, right);
        int[] actual = merged.partitionStream().toArray();
        int[] expectedPartitions = { 0, 1, 2, 3 };
        String[] expectedFqdns = { "fqdn1", "fqdn2", "fqdn3", "fqdn4" };
        String[] actualFqdns = IntStream.of(actual).mapToObj(i -> merged.getFqdn(i)).toArray(String[]::new);
        Assert.assertTrue(Arrays.equals(expectedPartitions, actual));
        Assert.assertTrue(Arrays.equals(expectedFqdns, actualFqdns));
    }

    @Test
    public void mergeChooseMaxGeneration() {
        var key = ShardKey.create("foo", "cluster", "service");
        var numId = 1;
        var genId = 0;
        var totalPartitions = 4;
        final MetabaseShard left = new MetabaseShard(
                System.nanoTime(),
                key,
                numId,
                genId,
                totalPartitions
        );
        left.addPartition(genId, 2, "fqdn3", true, true);

        var rightGenId = genId + 1;
        final MetabaseShard right = new MetabaseShard(
                System.nanoTime(),
                key,
                numId,
                genId + 1,
                totalPartitions
        );
        right.addPartition(rightGenId, 0, "fqdn1", true, true);
        right.addPartition(rightGenId, 1, "fqdn2", true, true);
        right.addPartition(rightGenId, 3, "fqdn4", true, true);

        var merged = MetabaseShard.mergeShardInfoFromServers(left, right);
        int[] actual = merged.partitionStream().toArray();
        int[] expectedPartitions = { 0, 1, 3 };
        Assert.assertTrue(Arrays.equals(expectedPartitions, actual));
        String[] expectedFqdns = { "fqdn1", "fqdn2", "fqdn4" };
        String[] actualFqdns = IntStream.of(actual).mapToObj(i -> merged.getFqdn(i)).toArray(String[]::new);
        Assert.assertTrue(Arrays.equals(expectedPartitions, actual));
        Assert.assertTrue(Arrays.equals(expectedFqdns, actualFqdns));
    }

    @Test
    public void mergeChooseMaxFqdnCreatedAt() {
        var key = ShardKey.create("foo", "cluster", "service");
        var numId = 1;
        var genId = 0;
        var totalPartitions = 4;
        var createdAt = System.nanoTime();
        final MetabaseShard left = new MetabaseShard(
                createdAt,
                key,
                numId,
                genId,
                totalPartitions
        );
        left.addPartition(genId, 0, "fqdn1", true, true);
        left.addPartition(genId, 2, "fqdn3", true, true);

        var rightGenId = genId;
        final MetabaseShard right = new MetabaseShard(
                createdAt + 1,
                key,
                numId,
                rightGenId,
                totalPartitions
        );
        right.addPartition(rightGenId, 1, "fqdn2", true, true);
        right.addPartition(rightGenId, 2, "fqdn3_2", true, true);
        right.addPartition(rightGenId, 3, "fqdn4", true, true);

        var merged = MetabaseShard.mergeShardInfoFromServers(left, right);
        int[] actual = merged.partitionStream().toArray();
        int[] expectedPartitions = { 0, 1, 2, 3 };
        Assert.assertTrue(Arrays.equals(expectedPartitions, actual));
        String[] expectedFqdns = { "fqdn1", "fqdn2", "fqdn3_2", "fqdn4" };
        String[] actualFqdns = IntStream.of(actual).mapToObj(i -> merged.getFqdn(i)).toArray(String[]::new);
        Assert.assertTrue(Arrays.equals(expectedPartitions, actual));
        Assert.assertTrue(Arrays.equals(expectedFqdns, actualFqdns));
    }

    @Test
    public void mergeChooseMaxFqdnIgnoreNotReady() {
        var key = ShardKey.create("foo", "cluster", "service");
        var numId = 1;
        var genId = 0;
        var totalPartitions = 4;
        var createdAt = System.nanoTime();
        final MetabaseShard left = new MetabaseShard(
                createdAt,
                key,
                numId,
                genId,
                totalPartitions
        );
        left.addPartition(genId, 0, "fqdn1", true, true);
        left.addPartition(genId, 2, "fqdn3", true, true);

        var rightGenId = genId;
        final MetabaseShard right = new MetabaseShard(
                createdAt + 1,
                key,
                numId,
                rightGenId,
                totalPartitions
        );
        right.addPartition(rightGenId, 1, "fqdn2", true, true);
        right.addPartition(rightGenId, 2, "fqdn3_2", false, true);
        right.addPartition(rightGenId, 3, "fqdn4", true, true);

        var merged = MetabaseShard.mergeShardInfoFromServers(left, right);
        int[] actual = merged.partitionStream().toArray();
        int[] expectedPartitions = { 0, 1, 3 };
        Assert.assertTrue(Arrays.equals(expectedPartitions, actual));
        String[] expectedFqdns = { "fqdn1", "fqdn2", "fqdn4" };
        String[] actualFqdns = IntStream.of(actual).mapToObj(i -> merged.getFqdn(i)).toArray(String[]::new);
        Assert.assertTrue(Arrays.equals(expectedPartitions, actual));
        Assert.assertTrue(Arrays.equals(expectedFqdns, actualFqdns));
        Assert.assertEquals("fqdn3_2", merged.getFqdn(2));
    }

}
