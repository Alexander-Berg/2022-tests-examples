package ru.yandex.stockpile.server.shard.test;

import java.time.Instant;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.kikimr.client.kv.inMem.KikimrKvClientInMem;
import ru.yandex.misc.ExceptionUtils;
import ru.yandex.misc.concurrent.CompletableFutures;
import ru.yandex.misc.lang.ShortUtils;
import ru.yandex.misc.thread.ThreadUtils;
import ru.yandex.solomon.codec.archive.MetricArchiveMutable;
import ru.yandex.solomon.codec.archive.header.DeleteBeforeField;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.solomon.ut.ManualClock;
import ru.yandex.solomon.util.time.TimeProviderTestImpl;
import ru.yandex.stockpile.api.EDecimPolicy;
import ru.yandex.stockpile.server.data.dao.StockpileDaoForTests;
import ru.yandex.stockpile.server.shard.StockpileShard;
import ru.yandex.stockpile.server.shard.StockpileShardGlobals;
import ru.yandex.stockpile.server.shard.StockpileShardHacksForTest;
import ru.yandex.stockpile.server.shard.StockpileWriteRequest;
import ru.yandex.stockpile.server.shard.stat.StockpileShardAggregatedStats;

import static ru.yandex.solomon.model.point.AggrPoints.point;

/**
 * @author Stepan Koltsov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    StockpileShardTestContext.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class StockpileShardTestBase {

    @Rule
    public TestName testName = new TestName();
    @Autowired
    protected StockpileDaoForTests stockpileDaoForTests;
    @Autowired
    protected KikimrKvClientInMem kikimrKvClientInMem;
    @Autowired
    protected TimeProviderTestImpl timeProvider;
    @Autowired
    protected StockpileShardAggregatedStats stockpileShardAggregatedStats;
    @Autowired
    protected StockpileShardHacksForTest stockpileShardHacksForTest;
    @Autowired
    protected StockpileShardGlobals stockpileShardGlobals;
    @Autowired
    protected ManualClock clock;

    protected StockpileShard stockpileShard;
    protected long tabletId = 10;

    @Before
    public void init() {
        timeProvider.setTime(Instant.parse("2015-10-23T20:21:22Z"));
        tabletId = kikimrKvClientInMem.createKvTablet();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Throwable cause = e;
            while (cause != null && !(cause instanceof RejectedExecutionException)) {
                cause = cause.getCause();
            }
            if (cause != null) { // Found RejectedExecutionException in cause chain
                // Test timeouted
                System.err.print(Instant.now().toString()
                        + " terminating on uncaught exception;"
                        + " thread=" + t.getName() + '\n'
                        + ExceptionUtils.getStackTrace(e));
                // don't halt
                return;
            }
            ThreadUtils.uncaughtException(e);
        });
    }

    @After
    public void checkAfter() {
        if (stockpileShard != null) {
            stockpileShard.stop();
        }
    }

    protected void restart2(Consumer<StockpileShard> configureShard) {
        restartNoWait(configureShard);
        stockpileShard.waitForInitializedOrAnyError();
    }

    protected void restart() {
        restart2(s -> {});
    }

    protected void restartNoWait() {
        restartNoWait(ignore -> {});
    }

    private void restartNoWait(Consumer<StockpileShard> configureShard) {
        if (stockpileShard != null) {
            stockpileShard.stop();
            stockpileShard = null;
        }

        stockpileShard = new StockpileShard(stockpileShardGlobals, StockpileShardTestContext.nextShardId(), tabletId, testName.getMethodName());
        stockpileShard.storage.lock().join();
        configureShard.accept(stockpileShard);
        stockpileShard.start();
    }

    public static StockpileWriteRequest request(long localId, AggrPoint... points) {
        return request(localId, AggrGraphDataArrayList.of(points));
    }

    public static StockpileWriteRequest request(long localId, AggrGraphDataArrayList list) {
        return request(localId, MetricArchiveMutable.of(list));
    }

    public static StockpileWriteRequest request(long localId, MetricArchiveMutable archive) {
        return StockpileWriteRequest.newBuilder()
                .addArchiveCopy(localId, archive)
                .build();
    }

    public long pushBatchSync(StockpileWriteRequest req) {
        return pushBatchSync(req, false);
    }

    public long pushBatchSync(StockpileWriteRequest req, boolean forceLogSnapshot) {
        try {
            return CompletableFutures.join(stockpileShard.pushBatch(req)).getTxn();
        } finally {
            if (forceLogSnapshot) {
                stockpileShard.forceLogSnapshot().join();
            }
        }
    }

    public long pushBatchSinglePointSync(long localId, long tsMillis, double value) {
        return pushBatchSinglePointSync(localId, tsMillis, value, false);
    }

    public long pushBatchSinglePointSync(long localId, long tsMillis, double value, boolean forceLogSnapshot) {
        return pushBatchSync(request(localId, point(tsMillis, value)), forceLogSnapshot);
    }

    public long pushBatchSinglePointAggrSync(long localId, long tsMillis, double value, boolean merge, int count, boolean forceLogSnapshot) {
        return pushBatchSinglePointAggrSync(localId, AggrPoint.fullForTest(tsMillis, value, merge, count), forceLogSnapshot);
    }

    public long pushBatchSinglePointAggrSync(long localId, AggrPoint aggrPoint, boolean forceLogSnapshot) {
        return pushBatchSync(request(localId, aggrPoint), forceLogSnapshot);
    }

    public void pushDeleteSync(long localId) {
        MetricArchiveMutable archive = new MetricArchiveMutable();
        archive.setDeleteBefore(DeleteBeforeField.DELETE_ALL);
        pushBatchSync(request(localId, archive));
    }

    public void pushDecimPolicySync(long localId, EDecimPolicy decimPolicyId) {
        MetricArchiveMutable archive = new MetricArchiveMutable();
        archive.setDecimPolicyId(ShortUtils.toShortExact(decimPolicyId.getNumber()));
        pushBatchSync(request(localId, archive));
    }
}
