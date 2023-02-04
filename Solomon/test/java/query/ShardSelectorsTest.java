package ru.yandex.solomon.labels.query;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.labels.shard.ShardKey;


/**
 * @author Sergey Polovko
 */
public class ShardSelectorsTest {

    @Test
    public void allExact() {
        Selectors selectors = Selectors.of(
            Selector.exact("project", "yt"),
            Selector.exact("cluster", "hahn"),
            Selector.exact("service", "yt_master_internal"));

        Assert.assertTrue(ShardSelectors.isSingleShard(selectors));

        ShardKey shardKey = ShardSelectors.getShardKeyOrNull(selectors);
        Assert.assertNotNull(shardKey);
        Assert.assertEquals(new ShardKey("yt", "hahn", "yt_master_internal"), shardKey);
    }

    @Test
    public void globAllExact() {
        Selectors selectors = Selectors.of(
            Selector.glob("project", "yt"),
            Selector.glob("cluster", "hahn"),
            Selector.glob("service", "yt_master_internal"));

        Assert.assertTrue(ShardSelectors.isSingleShard(selectors));

        ShardKey shardKey = ShardSelectors.getShardKeyOrNull(selectors);
        Assert.assertNotNull(shardKey);
        Assert.assertEquals(new ShardKey("yt", "hahn", "yt_master_internal"), shardKey);
    }

    @Test
    public void withOneNotExact() {
        // not exact
        {
            Selectors selectors = Selectors.of(
                Selector.exact("project", "yt"),
                Selector.notExact("cluster", "hahn"),
                Selector.exact("service", "yt_master_internal"));

            Assert.assertFalse(ShardSelectors.isSingleShard(selectors));

            ShardKey shardKey = ShardSelectors.getShardKeyOrNull(selectors);
            Assert.assertNull(shardKey);
        }

        // regex
        {
            Selectors selectors = Selectors.of(
                Selector.exact("project", "yt"),
                Selector.regex("cluster", "^hahn.*$"),
                Selector.exact("service", "yt_master_internal"));

            Assert.assertFalse(ShardSelectors.isSingleShard(selectors));

            ShardKey shardKey = ShardSelectors.getShardKeyOrNull(selectors);
            Assert.assertNull(shardKey);
        }

        // glob not exact
        {
            Selectors selectors = Selectors.of(
                Selector.exact("project", "yt"),
                Selector.glob("cluster", "hahn*"),
                Selector.exact("service", "yt_master_internal"));

            Assert.assertFalse(ShardSelectors.isSingleShard(selectors));

            ShardKey shardKey = ShardSelectors.getShardKeyOrNull(selectors);
            Assert.assertNull(shardKey);
        }

        // multi glob
        {
            Selectors selectors = Selectors.of(
                Selector.exact("project", "yt"),
                Selector.glob("cluster", "hahn|banach"),
                Selector.exact("service", "yt_master_internal"));

            Assert.assertFalse(ShardSelectors.isSingleShard(selectors));

            ShardKey shardKey = ShardSelectors.getShardKeyOrNull(selectors);
            Assert.assertNull(shardKey);
        }
    }

    @Test
    public void withMetricName() {
        Selectors selectors = Selectors.of(
            "sensor",
            Selector.exact("project", "yt"),
            Selector.exact("cluster", "hahn"),
            Selector.exact("service", "yt_master_internal"));

        Assert.assertFalse(ShardSelectors.isSingleShard(selectors));
    }
}
