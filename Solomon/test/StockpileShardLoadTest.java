package ru.yandex.stockpile.server.shard.test;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.Test;

import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.column.StockpileColumn;
import ru.yandex.solomon.model.point.column.ValueRandomData;
import ru.yandex.stockpile.server.shard.MergeKind;
import ru.yandex.stockpile.server.shard.StockpileShard;
import ru.yandex.stockpile.server.shard.StockpileWriteRequest;

import static org.junit.Assert.assertEquals;

/**
 * @author Vladimir Gordiychuk
 */
public class StockpileShardLoadTest extends StockpileShardTestBase {

    @Test
    public void empty() {
        stockpileShard = new StockpileShard(stockpileShardGlobals, StockpileShardTestContext.nextShardId(), tabletId, testName.getMethodName());
        stockpileShard.start();
        stockpileShard.waitForInitializedOrAnyError();
        assertEquals(StockpileShard.LoadState.DONE, stockpileShard.getLoadState());
        assertEquals(0, stockpileShard.recordCount().getAsLong());
    }

    @Test
    public void restartShardWithMetricIndex() {
        final int shardId = StockpileShardTestContext.nextShardId();
        stockpileShard = new StockpileShard(stockpileShardGlobals, shardId, tabletId, testName.getMethodName());
        stockpileShard.start();
        stockpileShard.waitForInitializedOrAnyError();

        pushBatchSync(generateRequest(100_000));
        stockpileShard.forceSnapshotSync();
        for (MergeKind kind : MergeKind.values()) {
            stockpileShard.forceMergeSync(kind);
        }
        long recordCount = stockpileShard.recordCount().getAsLong();
        stockpileShard.stop();

        stockpileShard = new StockpileShard(stockpileShardGlobals, shardId, tabletId, testName.getMethodName());
        stockpileShard.start();
        stockpileShard.waitForInitializedOrAnyError();

        assertEquals(recordCount, stockpileShard.recordCount().getAsLong());
    }

    private StockpileWriteRequest generateRequest(int points) {
        StockpileWriteRequest.Builder builder = StockpileWriteRequest.newBuilder();

        AggrPoint point = new AggrPoint(StockpileColumn.TS.mask() | StockpileColumn.VALUE.mask());
        point.tsMillis = System.currentTimeMillis();
        long localId = 1;
        for (int i = 0; i < points; i++) {
            fillNextRandomValue(point);
            builder.archiveRef((localId++ & 0xffff) + 1).addRecord(point);
        }

        return builder.build();
    }

    private void fillNextRandomValue(AggrPoint point) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        point.tsMillis += 15_000 + random.nextInt(180_000);
        point.valueNum = ValueRandomData.randomNum(random);
        point.valueDenom = ValueRandomData.randomDenom(random);
    }
}
