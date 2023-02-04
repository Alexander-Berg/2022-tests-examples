package ru.yandex.stockpile.server.shard.test;

import java.time.Instant;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.misc.concurrent.CompletableFutures;
import ru.yandex.stockpile.server.shard.StockpileMetricReadRequest;

/**
 * @author Stepan Koltsov
 */
public class StockpileShardErrorTest extends StockpileShardTestBase {



    @Test
    public void threadLocalTimeout() throws Exception {
        long ts0 = Instant.parse("2015-10-22T12:13:14Z").toEpochMilli();
        long localId = 11234567;

        restart();

        pushBatchSinglePointSync(localId, ts0, 22);

        stockpileShard.forceSnapshotSync();

        try {
            CompletableFutures.join(stockpileShard.readOne(StockpileMetricReadRequest.newBuilder()
                    .setLocalId(localId)
                    .setDeadline(System.currentTimeMillis() - 1000)
                    .build()));
            Assert.fail();
        } catch (Exception e) {
            // expected
        }

    }


}
