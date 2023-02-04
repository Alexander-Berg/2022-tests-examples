package ru.yandex.stockpile.server.shard.test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.monlib.metrics.registry.MetricRegistry;
import ru.yandex.solomon.config.protobuf.Time;
import ru.yandex.solomon.config.protobuf.TimeUnit;
import ru.yandex.solomon.config.protobuf.stockpile.TKvStorageConfig;
import ru.yandex.solomon.config.protobuf.stockpile.TMergeConfig;
import ru.yandex.solomon.config.protobuf.stockpile.TMergeOptions;
import ru.yandex.solomon.config.protobuf.stockpile.TStockpileThreadPoolsConfig;
import ru.yandex.solomon.config.thread.StubThreadPoolProvider;
import ru.yandex.solomon.ut.ManualClock;
import ru.yandex.solomon.ut.ManualScheduledExecutorService;
import ru.yandex.solomon.util.concurrent.ForkJoinPools;
import ru.yandex.solomon.util.concurrent.ThreadUtils;
import ru.yandex.solomon.util.time.TimeProvider;
import ru.yandex.solomon.util.time.TimeProviderTestImpl;
import ru.yandex.stockpile.client.shard.StockpileShardId;
import ru.yandex.stockpile.kikimrKv.inMem.KikimrKvInMemContext;
import ru.yandex.stockpile.server.data.dao.StockpileDaoForTests;
import ru.yandex.stockpile.server.shard.InvalidArchiveStrategy;
import ru.yandex.stockpile.server.shard.StockpileExecutor;
import ru.yandex.stockpile.server.shard.StockpileMergeExecutor;
import ru.yandex.stockpile.server.shard.StockpileReadExecutor;
import ru.yandex.stockpile.server.shard.StockpileScheduledExecutor;
import ru.yandex.stockpile.server.shard.StockpileShardGlobals;

/**
 * @author Stepan Koltsov
 */
@Configuration
@Import({
    StockpileShardGlobals.class,
    KikimrKvInMemContext.class,
    UpdateShardMemoryLimitsDummy.class,
    StubThreadPoolProvider.class,
    StockpileDaoForTests.class,
})
public class StockpileShardTestContext {

    public static int nextShardId() {
        return StockpileShardId.random();
    }

    @StockpileExecutor
    @Bean
    public ExecutorService executorService() {
        return Executors.newFixedThreadPool(2);
    }

    @StockpileScheduledExecutor
    @Bean
    public ManualScheduledExecutorService scheduledExecutorService(ManualClock clock) {
        return new ManualScheduledExecutorService(1, clock);
    }

    @Bean
    @StockpileMergeExecutor
    public ForkJoinPool mergeExecutor() {
        return ForkJoinPools.newPool(1, StockpileMergeExecutor.class.getSimpleName());
    }

    @Bean
    @StockpileReadExecutor
    public ExecutorService stockpileReadExecutor() {
        return Executors.newFixedThreadPool(1, ThreadUtils.newThreadFactory(StockpileReadExecutor.class));
    }

    @Bean
    public TStockpileThreadPoolsConfig stockpileThreadPoolsConfig() {
        TStockpileThreadPoolsConfig.Builder builder = TStockpileThreadPoolsConfig.newBuilder();
        builder.setGeneralThreadPool("general");
        builder.setMergeThreadPool("merge");
        builder.setStockpileReadThreadPool("read");
        return builder.build();
    }

    @Bean
    public TKvStorageConfig storageConfig() {
        return TKvStorageConfig.newBuilder().build();
    }

    @Bean
    public ManualClock clock() {
        return new ManualClock();
    }

    @Bean
    public InvalidArchiveStrategy invalidArchiveStrategy() {
        return InvalidArchiveStrategy.fail();
    }

    @Bean
    @Primary
    public MetricRegistry registry() {
        return new MetricRegistry();
    }

    @Bean
    public TimeProvider timeProvider() {
        return new TimeProviderTestImpl();
    }

    @Bean
    public TMergeConfig mergeConfig() {
        return TMergeConfig.newBuilder()
            .setDailyOptions(TMergeOptions.newBuilder()
                .setEnableNew(true)
                .setSnapshotLimit(3)
                .setAllowDecim(true)
                .setSplitDelay(Time.newBuilder()
                    .setValue(1)
                    .setUnit(TimeUnit.DAYS)
                    .build())
                .setForceMergeAfter(Time.newBuilder()
                    .setValue(2)
                    .setUnit(TimeUnit.DAYS)
                    .build())
                .build())
            .setEternityOptions(TMergeOptions.newBuilder()
                .setEnableNew(true)
                .setSnapshotLimit(3)
                .setAllowDecim(true)
                .setForceMergeAfter(Time.newBuilder()
                    .setValue(30)
                    .setUnit(TimeUnit.DAYS)
                    .build())
                .build())
            .build();
    }
}
