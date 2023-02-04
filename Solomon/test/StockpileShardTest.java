package ru.yandex.stockpile.server.shard.test;

import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.kikimr.client.kv.KikimrKvClient;
import ru.yandex.kikimr.proto.MsgbusKv;
import ru.yandex.kikimr.util.NameRange;
import ru.yandex.misc.concurrent.CompletableFutures;
import ru.yandex.misc.dataSize.DataSize;
import ru.yandex.misc.random.Random2;
import ru.yandex.misc.thread.ThreadLocalTimeout;
import ru.yandex.misc.thread.ThreadUtils;
import ru.yandex.monlib.metrics.CompositeMetricSupplier;
import ru.yandex.monlib.metrics.encode.text.MetricTextEncoder;
import ru.yandex.monlib.metrics.summary.ImmutableSummaryDoubleSnapshot;
import ru.yandex.monlib.metrics.summary.ImmutableSummaryInt64Snapshot;
import ru.yandex.salmon.proto.StockpileCanonicalProto;
import ru.yandex.solomon.codec.archive.MetricArchiveImmutable;
import ru.yandex.solomon.codec.archive.MetricArchiveMutable;
import ru.yandex.solomon.codec.archive.header.DeleteBeforeField;
import ru.yandex.solomon.codec.archive.header.MetricHeader;
import ru.yandex.solomon.codec.serializer.StockpileFormat;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.column.TsColumn;
import ru.yandex.solomon.model.point.column.TsRandomData;
import ru.yandex.solomon.model.point.column.ValueColumn;
import ru.yandex.solomon.model.point.column.ValueRandomData;
import ru.yandex.solomon.model.protobuf.MetricType;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.solomon.model.timeseries.MetricTypeTransfers;
import ru.yandex.solomon.model.type.Histogram;
import ru.yandex.solomon.model.type.LogHistogram;
import ru.yandex.solomon.util.host.HostUtils;
import ru.yandex.solomon.util.protobuf.ProtobufText;
import ru.yandex.solomon.util.time.InstantUtils;
import ru.yandex.stockpile.api.EDecimPolicy;
import ru.yandex.stockpile.api.EProjectId;
import ru.yandex.stockpile.api.EStockpileStatusCode;
import ru.yandex.stockpile.api.MetricMeta;
import ru.yandex.stockpile.api.grpc.StockpileRuntimeException;
import ru.yandex.stockpile.client.shard.StockpileLocalId;
import ru.yandex.stockpile.ser.test.convert.ArchiveConverters;
import ru.yandex.stockpile.ser.test.convert.LogEntryConverters;
import ru.yandex.stockpile.server.SnapshotLevel;
import ru.yandex.stockpile.server.Txn;
import ru.yandex.stockpile.server.data.chunk.ChunkIndex;
import ru.yandex.stockpile.server.data.chunk.DataRange;
import ru.yandex.stockpile.server.data.chunk.DataRangeGlobal;
import ru.yandex.stockpile.server.data.chunk.DataRangeInSnapshot;
import ru.yandex.stockpile.server.data.index.SnapshotIndex;
import ru.yandex.stockpile.server.data.names.FileKind;
import ru.yandex.stockpile.server.data.names.FileNameParsed;
import ru.yandex.stockpile.server.data.names.StockpileKvNames;
import ru.yandex.stockpile.server.data.names.file.IndexFile;
import ru.yandex.stockpile.server.data.names.file.LogFile;
import ru.yandex.stockpile.server.shard.KvLock;
import ru.yandex.stockpile.server.shard.MergeKind;
import ru.yandex.stockpile.server.shard.ReadResultStatus;
import ru.yandex.stockpile.server.shard.SnapshotTs;
import ru.yandex.stockpile.server.shard.SnapshotTs.SpecialTs;
import ru.yandex.stockpile.server.shard.StockpileMetricReadRequest;
import ru.yandex.stockpile.server.shard.StockpileMetricReadResponse;
import ru.yandex.stockpile.server.shard.StockpileShard;
import ru.yandex.stockpile.server.shard.StockpileWriteRequest;
import ru.yandex.stockpile.server.shard.stat.SizeAndCount;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;
import static ru.yandex.solomon.model.point.AggrPointDataTestSupport.randomPoint;
import static ru.yandex.solomon.model.point.AggrPoints.lpoint;
import static ru.yandex.solomon.model.point.AggrPoints.point;
import static ru.yandex.solomon.util.CloseableUtils.close;


/**
 * @author Stepan Koltsov
 */
public class StockpileShardTest extends StockpileShardTestBase {
    private static final Logger logger = LoggerFactory.getLogger(StockpileShardTest.class);
    @Rule
    public Timeout timeoutRule = Timeout.builder()
            .withLookingForStuckThread(true)
            .withTimeout(1, TimeUnit.MINUTES)
            .build();

    @Test
    public void emptyWriteIsNotAllowed() {
        restart();

        try {
            pushBatchSync(StockpileWriteRequest.newBuilder().build());
            Assert.fail("must failed");
        } catch (Exception e) {
            Throwable t = CompletableFutures.unwrapCompletionException(e);
            Assert.assertEquals("INVALID_REQUEST: write without points", t.getMessage());
        }

        // no files written
        List<KikimrKvClient.KvEntryStats> files = kikimrKvClientInMem.readRangeNames(
            stockpileShard.kvTabletId, 0, 0).join();
        assertTrue(files.isEmpty());
    }

    @Test
    public void lastTsPresentInIndex() {
        restart();

        long ts0 = System.currentTimeMillis();
        long ts1 = ts0 + 1;
        long ts2 = ts0 + 1000;

        //
        //                  ts0   ts1                      ts2
        // localId: ---------*-----*---------- ~ -----------*----------
        //                   10    20                       30
        //

        long localId = StockpileLocalId.random();
        pushBatchSinglePointSync(localId, ts0, 10);
        pushBatchSinglePointSync(localId, ts2, 30);  // <- out of order
        pushBatchSinglePointSync(localId, ts1, 20);

        long localId2 = StockpileLocalId.random();
        pushBatchSinglePointSync(localId2, ts0+10, 10);
        pushBatchSinglePointSync(localId2, ts2+10, 30);  // <- out of order
        pushBatchSinglePointSync(localId2, ts1+10, 20);

        long localId3 = StockpileLocalId.random();
        pushBatchSinglePointSync(localId3, ts0+20, 10);
        pushBatchSinglePointSync(localId3, ts2+20, 30);  // <- out of order
        pushBatchSinglePointSync(localId3, ts1+20, 20);

        // after two hours snapshot
        {
            long txn = stockpileShard.forceSnapshotSync();
            SnapshotIndex index = readSnapshotIndex(txn, SnapshotLevel.TWO_HOURS);
            long lastTsMillis = index.getContent().findMetricLastTsMillis(localId);
            Assert.assertEquals(ts2, lastTsMillis);

            List<MetricMeta> meta = stockpileShard.readMetricsMeta(new long[]{ localId }).join();
            Assert.assertEquals(1, meta.size());
            Assert.assertEquals(localId, meta.get(0).getLocalId());
            Assert.assertEquals(ts2, meta.get(0).getLastTsMillis());
        }

        // after daily merge
        {
            long txn = stockpileShard.forceMergeSync(MergeKind.DAILY).getTxn();
            SnapshotIndex index = readSnapshotIndex(txn, SnapshotLevel.DAILY);
            long lastTsMillis = index.getContent().findMetricLastTsMillis(localId);
            Assert.assertEquals(ts2, lastTsMillis);

            List<MetricMeta> meta = stockpileShard.readMetricsMeta(new long[]{ localId }).join();
            Assert.assertEquals(1, meta.size());
            Assert.assertEquals(localId, meta.get(0).getLocalId());
            Assert.assertEquals(ts2, meta.get(0).getLastTsMillis());
        }

        // after eternity merge
        {
            clock.passedTime(30, TimeUnit.DAYS);
            long txn = stockpileShard.forceMergeSync(MergeKind.DAILY).getTxn();
            SnapshotIndex index = readSnapshotIndex(txn, SnapshotLevel.ETERNITY);
            long lastTsMillis = index.getContent().findMetricLastTsMillis(localId);
            Assert.assertEquals(ts2, lastTsMillis);

            List<MetricMeta> meta = stockpileShard.readMetricsMeta(new long[]{ localId }).join();
            Assert.assertEquals(1, meta.size());
            Assert.assertEquals(localId, meta.get(0).getLocalId());
            Assert.assertEquals(ts2, meta.get(0).getLastTsMillis());
        }
    }

    @Nonnull
    private SnapshotIndex readSnapshotIndex(long txn, SnapshotLevel level) {
        ThreadLocalTimeout.Handle timeout = ThreadLocalTimeout.pushTimeout(1, TimeUnit.MINUTES);
        try {
            return stockpileDaoForTests
                .readIndex(stockpileShard.kvTabletId, 0, level, txn)
                .join();
        } finally {
            timeout.pop();
        }
    }

    @Test
    public void deleteLocalIdWithWriteRequest() {
        restart();

        long ts0 = timeProvider.nowMillis();
        long ts1 = ts0 + 1000;

        // (1) add 3 points
        //                         ts0           ts1
        //       localId0:  --------*-------------*--------
        //       localId1:  --------*----------------------
        //

        long localId0 = StockpileLocalId.random();
        pushBatchSinglePointSync(localId0, ts0, 111);
        pushBatchSinglePointSync(localId0, ts1, 222);

        long localId1 = StockpileLocalId.random();
        pushBatchSinglePointSync(localId1, ts0, 333);

        stockpileShard.forceMergeSync(MergeKind.DAILY);
        stockpileShard.forceMergeSync(MergeKind.ETERNITY);

        // (2) check points are written

        restart();

        Assert.assertEquals(listOf(point(ts0, 111), point(ts1, 222)), readPointsSync(localId0));
        Assert.assertEquals(listOf(point(ts0, 333)), readPointsSync(localId1));

        // (3) delete all points in localId0 before ts1
        //
        //                         ts0           ts1
        //       localId0:  ~~~~~~~~~~~~~~~~~~~~~~*--------
        //       localId1:  --------*----------------------
        //

        {
            pushDeleteBefore(localId0, ts1).join();
            stockpileShard.forceMergeSync(MergeKind.DAILY);
            stockpileShard.forceMergeSync(MergeKind.ETERNITY);

            Assert.assertEquals(listOf(point(ts1, 222)), readPointsSync(localId0));
            Assert.assertEquals(listOf(point(ts0, 333)), readPointsSync(localId1));
        }

        // (4) delete all points in localId1
        //
        //                         ts0           ts1
        //       localId0:  ----------------------*--------
        //       localId1:  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        //

        {
            // deletion batch build with write request builder
            pushDeleteBefore(localId1, DeleteBeforeField.DELETE_ALL).join();

            long[] localIds = CompletableFutures.join(stockpileShard.inspector().allLocalIds()
                .thenApply(LongOpenHashSet::toLongArray));

            long[] expectedLocalIds = {localId0, localId1};
            Arrays.sort(expectedLocalIds);
            Arrays.sort(localIds);

            Assert.assertArrayEquals(expectedLocalIds, localIds);

            stockpileShard.forceSnapshotSync();
            stockpileShard.forceMergeSync(MergeKind.DAILY);
            stockpileShard.forceMergeSync(MergeKind.ETERNITY);
        }

        {
            restart();

            clock.passedTime(31, TimeUnit.DAYS);
            stockpileShard.forceMergeSync(MergeKind.DAILY);
            stockpileShard.forceMergeSync(MergeKind.ETERNITY);

            long[] localIds = CompletableFutures.join(stockpileShard.inspector().allLocalIds()
                .thenApply(LongOpenHashSet::toLongArray));

            Assert.assertArrayEquals(new long[] { localId0 }, localIds);

            Assert.assertEquals(listOf(point(ts1, 222)), readPointsSync(localId0));
            Assert.assertEquals(AggrGraphDataArrayList.empty(), readPointsSync(localId1));
        }
    }

    @Test
    public void forceMergeEmptySnapshot() {
        restart();

        stockpileShard.forceSnapshotSync();

        Assert.assertEquals(0, stockpileDaoForTests.readIndexList(tabletId, 0, SnapshotLevel.TWO_HOURS).join().size());
        Assert.assertEquals(0, stockpileDaoForTests.readIndexList(tabletId, 0, SnapshotLevel.ETERNITY).join().size());

        stockpileShard.forceMergeSync(MergeKind.DAILY);

        Assert.assertEquals(0, stockpileDaoForTests.readIndexList(tabletId, 0, SnapshotLevel.TWO_HOURS).join().size());
        Assert.assertEquals(0, stockpileDaoForTests.readIndexList(tabletId, 0, SnapshotLevel.ETERNITY).join().size());

        stockpileShard.forceMergeSync(MergeKind.ETERNITY);

        Assert.assertEquals(0, stockpileDaoForTests.readIndexList(tabletId, 0, SnapshotLevel.TWO_HOURS).join().size());
        Assert.assertEquals(0, stockpileDaoForTests.readIndexList(tabletId, 0, SnapshotLevel.ETERNITY).join().size());
    }

    @Test
    public void mergeAfterLog() {
        long ts0 = Instant.parse("2015-10-22T12:13:14Z").toEpochMilli();
        long localId = StockpileLocalId.random();

        restart();

        pushBatchSinglePointSync(localId, ts0, 10);

        dumpDatabase();

        stockpileShard.forceMergeSync(MergeKind.DAILY);
        stockpileShard.forceMergeSync(MergeKind.ETERNITY);

        restart();

        Assert.assertEquals(listOf(point(ts0, 10)), readPointsSync(localId));
    }

    @Test
    public void writeAndRead() {
        long ts0 = Instant.parse("2015-10-22T12:13:14Z").toEpochMilli();
        long localId = StockpileLocalId.random();

        restart();

        pushBatchSinglePointSync(localId, ts0, 111);

        Assert.assertEquals(listOf(point(ts0, 111)), readPointsSync(localId));
    }

    @Test
    public void readLogsAfterRestart() {

        long ts0 = Instant.parse("2015-10-22T12:13:14Z").toEpochMilli();
        long localId = StockpileLocalId.random();

        restart();

        {

            pushBatchSinglePointSync(localId, ts0, 29);

            AggrGraphDataArrayList graphData = readPointsSync(localId);

            Assert.assertEquals(listOf(point(ts0, 29)), graphData);
        }

        restart();

        {
            AggrGraphDataArrayList graphData = readPointsSync(localId);
            Assert.assertEquals(listOf(point(ts0, 29)), graphData);
        }
    }

    @Test
    public void readParallelWithWrite() {
        long ts0 = Instant.parse("2015-10-22T12:13:14Z").toEpochMilli();
        long localId = StockpileLocalId.random();

        restart();

        stockpileShard.setMaxCacheSize(100500);

        // need to write snapshot, otherwize there won't be anything to read
        pushBatchSinglePointSync(localId, ts0, 10);
        stockpileShard.forceSnapshotSync();

        kikimrKvClientInMem.pauseReads();

        CompletableFuture<AggrGraphDataArrayList> readFuture = readPoints(localId);
        Assert.assertFalse(readFuture.isDone());

        pushBatchSinglePointSync(localId, ts0 + 1000, 20);

        kikimrKvClientInMem.resumeReads();

        // read started before write, so it shouldn't see that write
        Assert.assertEquals(listOf(point(ts0, 10)), readFuture.join());

        Assert.assertEquals(listOf(point(ts0, 10), point(ts0 + 1000, 20)), readPointsSync(localId));
    }

    @Test
    public void readFromSnapshot() {
        long ts0 = Instant.parse("2015-10-22T12:13:14Z").toEpochMilli();
        long localId = StockpileLocalId.random();

        restart();

        pushBatchSinglePointSync(localId, ts0, 29);

        {
            AggrGraphDataArrayList graphData = readPointsSync(localId);
            Assert.assertEquals(listOf(point(ts0, 29)), graphData);
        }

        stockpileShard.forceSnapshotSync();

        Assert.assertEquals(0,
            stockpileDaoForTests.readLogs(stockpileShard.kvTabletId, 0).join()._1.size());

        {
            AggrGraphDataArrayList graphData = readPointsSync(localId);
            Assert.assertEquals(listOf(point(ts0, 29)), graphData);
        }

        restart();

        {
            AggrGraphDataArrayList graphData = readPointsSync(localId);
            Assert.assertEquals(listOf(point(ts0, 29)), graphData);
        }
    }

    @Test
    public void readDuringRename() throws Exception {
        long ts0 = Instant.parse("2015-10-22T12:13:14Z").toEpochMilli();
        long localId = StockpileLocalId.random();

        stockpileShardHacksForTest.mergeRenameCyclicBarrier = new CyclicBarrier(2);

        restart();

        pushBatchSinglePointSync(localId, ts0, 10);

        var mergeFuture = stockpileShard.forceMerge(MergeKind.DAILY);

        // 1
        stockpileShardHacksForTest.mergeRenameCyclicBarrier.await();

        CompletableFuture<AggrGraphDataArrayList> readFuture = readPoints(localId);

        // there is no simple way to check read hit the block
        ThreadUtils.sleep(50);

        Assert.assertFalse(mergeFuture.isDone());
        Assert.assertFalse(readFuture.isDone());

        // 2
        stockpileShardHacksForTest.mergeRenameCyclicBarrier.await();

        mergeFuture.join();
        Assert.assertEquals(listOf(point(ts0, 10)), readFuture.join());
    }

    @Test
    public void mergePoints() {
        long ts0 = Instant.parse("2015-10-22T12:13:14Z").toEpochMilli();
        long localId1 = StockpileLocalId.random();
        long localId2 = StockpileLocalId.random();

        restart();

        pushBatchSinglePointSync(localId1, ts0, 17);

        stockpileShard.forceSnapshotSync();

        pushBatchSinglePointSync(localId1, ts0 + 1000, 13);
        pushBatchSinglePointSync(localId2, ts0, 19);

        stockpileShard.forceSnapshotSync();

        stockpileShard.forceMergeSync(MergeKind.DAILY);
        stockpileShard.forceMergeSync(MergeKind.ETERNITY);

        Assert.assertEquals(listOf(point(ts0, 17), point(ts0 + 1000, 13)), readPointsSync(localId1));
        Assert.assertEquals(listOf(point(ts0, 19)), readPointsSync(localId2));

        restart();

        Assert.assertEquals(listOf(point(ts0, 17), point(ts0 + 1000, 13)), readPointsSync(localId1));
        Assert.assertEquals(listOf(point(ts0, 19)), readPointsSync(localId2));
    }

    @Test
    public void cacheWorks() {

        long ts0 = Instant.parse("2015-11-08T12:13:44Z").toEpochMilli();
        long localId = StockpileLocalId.random();

        restart();

        pushBatchSinglePointSync(localId, ts0, 17);

        stockpileShard.forceSnapshotSync();

        restart();

        stockpileShard.setMaxCacheSize(100500);

        Assert.assertEquals(listOf(point(ts0, 17)), readPointsSync(localId));

        Assert.assertEquals(1L, stockpileShardAggregatedStats.readProcessingStatus.get(ReadResultStatus.FROM_STORAGE).get());
        Assert.assertEquals(0L, stockpileShardAggregatedStats.readProcessingStatus.get(ReadResultStatus.CACHED).get());

        Assert.assertEquals(listOf(point(ts0, 17)), readPointsSync(localId));

        Assert.assertEquals(1L, stockpileShardAggregatedStats.readProcessingStatus.get(ReadResultStatus.FROM_STORAGE).get());
        Assert.assertEquals(1L, stockpileShardAggregatedStats.readProcessingStatus.get(ReadResultStatus.CACHED).get());
    }

    @Test
    public void cacheBug1() {
        long ts0 = Instant.parse("2015-11-08T12:13:44Z").toEpochMilli();
        long localId = StockpileLocalId.random();

        restart();

        pushBatchSinglePointSync(localId, ts0, 17);

        stockpileShard.forceSnapshotSync();

        restart();

        stockpileShard.setMaxCacheSize(100500);

        kikimrKvClientInMem.pauseReads();

        CompletableFuture<AggrGraphDataArrayList> future = readPoints(localId);

        pushBatchSinglePointSync(localId, ts0, 19);

        kikimrKvClientInMem.resumeReads();

        // 19 would be OK too
        Assert.assertEquals(listOf(point(ts0, 17)), future.join());

        Assert.assertEquals(listOf(point(ts0, 19)), readPointsSync(localId));
    }

    @Test
    public void logSnapshot() {
        long ts0 = Instant.parse("2015-11-08T12:13:44Z").toEpochMilli();
        long localId = StockpileLocalId.random();

        restart();

        pushBatchSinglePointSync(localId, ts0, 44);
        Assert.assertEquals(listOf(point(ts0, 44)), readPointsSync(localId));

        Assert.assertEquals(1, stockpileDaoForTests.readLogs(stockpileShard.kvTabletId, 0)
            .join()
            ._1
            .size());

        pushBatchSinglePointSync(localId, ts0 + 1000, 55, true);
        Assert.assertEquals(listOf(point(ts0, 44), point(ts0 + 1000, 55)), readPointsSync(localId));

        Assert.assertEquals(1, stockpileDaoForTests.readLogs(stockpileShard.kvTabletId, 0)
            .join()
            ._1
            .size());

        restart();

        Assert.assertEquals(listOf(point(ts0, 44), point(ts0 + 1000, 55)), readPointsSync(localId));
    }


    @Test
    public void indexStats() {
        long ts0 = Instant.parse("2016-01-25T12:13:44Z").toEpochMilli();
        long localId = StockpileLocalId.random();

        restart();

        pushBatchSinglePointSync(localId, ts0, 10);
        pushBatchSinglePointSync(localId, ts0 + 1000, 20);
        pushBatchSinglePointSync(localId, ts0 + 1, 30);

        long snapshotTxn = stockpileShard.forceSnapshotSync();

        ThreadLocalTimeout.Handle handle = ThreadLocalTimeout.pushTimeout(1, TimeUnit.MINUTES);
        try {
            Assert.assertEquals(3L,
                stockpileDaoForTests.readIndex(stockpileShard.kvTabletId, 0, SnapshotLevel.TWO_HOURS, snapshotTxn)
                    .join().getContent().getRecordCount());
        } finally {
            handle.popSafely();
        }
    }

    @Test
    public void countIsProperlyMerged() {
        restart();

        long localId = StockpileLocalId.random();
        long ts0 = timeProvider.nowMillis();

        pushBatchSinglePointAggrSync(localId, ts0, 10, true, 1, false);
        pushBatchSinglePointAggrSync(localId, ts0, 20, true, 2, false);
        pushBatchSinglePointAggrSync(localId, ts0, 40, true, 4, true);

        restart();

        pushBatchSinglePointAggrSync(localId, ts0, 80, true, 8, false);

        stockpileShard.forceSnapshotSync();

        pushBatchSinglePointAggrSync(localId, ts0, 160, true, 16, false);

        stockpileShard.forceMergeSync(MergeKind.DAILY);
        stockpileShard.forceMergeSync(MergeKind.ETERNITY);

        MetricArchiveImmutable ma = MetricArchiveImmutable.of(readPointsSync(localId));

        MetricArchiveMutable expected = new MetricArchiveMutable();
        expected.addRecord(AggrPoint.builder()
                .time(ts0)
                .doubleValue(310)
                .merged()
                .count(31)
                .build());

        var immutable = expected.toImmutable();
        Assert.assertEquals(immutable, ma);
        close(ma, expected, immutable);
    }

    private void dumpSnapshot(long txn, SnapshotLevel level) {
        ThreadLocalTimeout.Handle handle = ThreadLocalTimeout.pushTimeout(1, TimeUnit.MINUTES);
        try {
            SnapshotIndex index = stockpileDaoForTests.readIndex(tabletId, 0, level, txn).join();
            System.out.println(index.getContent());
            ChunkIndex[] chunks = index.getContent().getChunks();
            for (int i = 0; i < chunks.length; i++) {
                ChunkIndex chunkIndex = chunks[i];
                for (int metricIdx = 0; metricIdx < chunkIndex.metricCount(); metricIdx++) {
                    DataRangeGlobal dataRangeGlobal = new DataRangeGlobal(
                        level, txn,
                        index.getContent().getFormat(),
                        new DataRangeInSnapshot(i, new DataRange(chunkIndex.getOffset(metricIdx), chunkIndex.getSize(metricIdx))));
                    MetricArchiveImmutable
                        multiArchive = stockpileShard.storage.readSnapshotRange(dataRangeGlobal)
                        .join().get();
                    String text = ProtobufText.serializeToText(ArchiveConverters.toProto(multiArchive));
                    System.out.println("metric: " + StockpileLocalId.toString(chunkIndex.getLocalId(metricIdx)));
                    System.out.println(text);
                    close(multiArchive);
                }
            }
        } finally {
            handle.popSafely();
        }
    }

    private void dumpLog(long txn) {
        System.out.println("log: " + txn);
        try (var log = stockpileDaoForTests.readLog(tabletId, 0, txn).join()) {
            StockpileCanonicalProto.LogEntry proto = LogEntryConverters.toProto(log.getContent());
            String text = ProtobufText.serializeToText(proto);
            System.out.println(text);
        }
    }

    private void dumpDatabase() {
        List<KikimrKvClient.KvEntryStats> join = kikimrKvClientInMem.readRangeNames(tabletId, 0, NameRange.all(), 0).join();
        for (KikimrKvClient.KvEntryStats e : join) {
            FileNameParsed name = FileNameParsed.parseCurrent(e.getName());
            if (name instanceof IndexFile) {
                IndexFile index = (IndexFile) name;
                dumpSnapshot(index.txn0zForTest(), index.getLevel());
            } else if (name instanceof LogFile) {
                LogFile log = (LogFile) name;
                dumpLog(log.txn0zForTest());
            }
        }
    }

    @Test
    public void simpleNonderivData() {
        restart();

        long ts0 = timeProvider.nowMillis() - 8 * 24 * 60 * 60 * 1000;
        AggrGraphDataArrayList expected = listOf(
            point(ts0, 40),
            point(ts0 + 1000, 50),
            point(ts0 + 2000, 60));
        AggrGraphDataArrayList expectedDecim = listOf(point(ts0 - 82000, 50));

        long localId = StockpileLocalId.random();

        pushDecimPolicySync(localId, EDecimPolicy.POLICY_5_MIN_AFTER_7_DAYS);
        pushBatchSinglePointSync(localId, ts0, 40);
        pushBatchSinglePointSync(localId, ts0 + 1000, 50);
        pushBatchSinglePointSync(localId, ts0 + 2000, 60);

        checkDataLifecycle(localId, expectedDecim);

        restart();
    }

    private void checkDataLifecycle(long localId, AggrGraphDataArrayList expectedDecim) {
        // All data is in the latest snapshot; deriv occurs on read: AggrGraphDataArrayList calculates forward deriv
        Assert.assertEquals(expectedDecim, readPointsSync(localId));

        stockpileShard.forceSnapshotSync();

        Assert.assertEquals(expectedDecim, readPointsSync(localId));

        stockpileShard.forceMergeSync(MergeKind.DAILY);

        Assert.assertEquals(expectedDecim, readPointsSync(localId));

        stockpileShard.forceMergeSync(MergeKind.ETERNITY);

        Assert.assertEquals(expectedDecim, readPointsSync(localId));
    }

    @Test
    public void deleteFromEternity() {
        restart();

        long ts0 = timeProvider.nowMillis();

        long localId = StockpileLocalId.random();

        pushBatchSinglePointSync(localId, ts0, 40);

        stockpileShard.forceSnapshotSync();
        stockpileShard.forceMergeSync(MergeKind.DAILY);
        stockpileShard.forceMergeSync(MergeKind.ETERNITY);

        Assert.assertEquals(listOf(point(ts0, 40)), readPointsSync(localId));

        pushDeleteSync(localId);

        Assert.assertEquals(AggrGraphDataArrayList.empty(), readPointsSync(localId));

        long txn = stockpileShard.forceSnapshotSync();

        dumpSnapshot(txn, SnapshotLevel.TWO_HOURS);

        Assert.assertEquals(AggrGraphDataArrayList.empty(), readPointsSync(localId));

        stockpileShard.forceSnapshotSync();

        Assert.assertEquals(AggrGraphDataArrayList.empty(), readPointsSync(localId));
    }

    @Test
    public void deleteBefore() {
        restart();

        long ts0 = timeProvider.nowMillis();
        long ts1 = ts0 + 2000;

        long localId = StockpileLocalId.random();

        pushBatchSinglePointSync(localId, ts0, 40);
        pushBatchSinglePointSync(localId, ts1, 60);

        stockpileShard.forceSnapshotSync();

        Assert.assertEquals(listOf(point(ts0, 40), point(ts1, 60)), readPointsSync(localId));

        pushDeleteBefore(localId, ts1).join();

        Assert.assertEquals(listOf(point(ts1, 60)), readPointsSync(localId));

        stockpileShard.forceSnapshotSync();

        Assert.assertEquals(listOf(point(ts1, 60)), readPointsSync(localId));

        pushBatchSinglePointSync(localId, ts0, 80);

        Assert.assertEquals(listOf(point(ts0, 80), point(ts1, 60)), readPointsSync(localId));

        stockpileShard.forceSnapshotSync();

        Assert.assertEquals(listOf(point(ts0, 80), point(ts1, 60)), readPointsSync(localId));
    }

    @Test
    public void forceLogSnapshotAfterLogSnapshot() {
        restart();

        long ts0 = timeProvider.nowMillis();
        long localId = 1234567890;

        AggrGraphDataArrayList expected = new AggrGraphDataArrayList();

        for (int i = 0; i < 2; ++i) {
            AggrPoint aggrPoint = AggrPoint.fullForTest(ts0, (i + 1) * 100, true, (i + 1) * 10);
            pushBatchSinglePointAggrSync(
                localId, aggrPoint, true);
            expected.addRecord(aggrPoint);
        }

        restart();

        AggrGraphDataArrayList read = readPointsSync(localId);
        Assert.assertEquals(expected.toSortedMerged(), read.toSortedMerged());
    }

    @Test
    public void forceLogSnapshotMoreComplexTest() {
        restart();

        long ts0 = timeProvider.nowMillis();
        long localId = 1234567890;

        AggrGraphDataArrayList expected = new AggrGraphDataArrayList();

        for (int i = 0; i < 4; ++i) {
            if (i == 2) {
                restart();
            }

            AggrPoint aggrPoint = AggrPoint.fullForTest(ts0, 10 * (1 << i), true, 1 << i);
            pushBatchSinglePointAggrSync(localId, aggrPoint, true);

            expected.addRecord(aggrPoint);
        }

        restart();

        AggrGraphDataArrayList read = readPointsSync(localId);
        Assert.assertEquals(expected.toSortedMerged(), read.toSortedMerged());
    }

    private void randomItem(Random2 r) {
        restart();

        long ts0 = timeProvider.nowMillis();
        long localId = 1234567890;

        int len = r.nextInt(100);

        AggrGraphDataArrayList expected = new AggrGraphDataArrayList();

        for (int i = 0; i < len; ++i) {
            int op = r.nextInt(5);
            if (op == 0) {
                restart();
            } else if (op == 1) {
                AggrPoint aggrPoint = AggrPoint.fullForTest(
                    ts0 + 1000 * r.nextInt(10),
                    r.nextInt(100),
                    true,
                    1 + r.nextInt(100));
                pushBatchSinglePointAggrSync(localId, aggrPoint, true);

                expected.addRecord(aggrPoint);
            } else if (op == 2) {
                stockpileShard.forceSnapshotSync();
            } else if (op == 3) {
                stockpileShard.forceMergeSync(MergeKind.DAILY);
            } else if (op == 4) {
                stockpileShard.forceMergeSync(MergeKind.ETERNITY);
            } else {
                throw new IllegalStateException();
            }
        }

        AggrGraphDataArrayList read = readPointsSync(localId);
        Assert.assertEquals(expected.toSortedMerged(), read.toSortedMerged());
    }

    @Ignore // too noisy: TODO: run with disabled tests
    @Test
    public void random() {
        for (int i = 0; i < 100; ++i) {
            kikimrKvClientInMem.clear();
            Random2 r = new Random2(i);
            randomItem(r);
        }
    }

    @Test
    public void bugDeleteAll() {
        restart();

        long ts0 = timeProvider.nowMillis();

        long localId0 = 8524895220L;
        pushBatchSinglePointSync(localId0, ts0, 111);

        stockpileShard.forceSnapshotSync();
        stockpileShard.forceMergeSync(MergeKind.DAILY);
        stockpileShard.forceMergeSync(MergeKind.ETERNITY);

        Assert.assertEquals(listOf(point(ts0, 111)), readPointsSync(localId0));

        {
            pushDeleteSync(localId0);

            stockpileShard.forceSnapshotSync();
            Assert.assertEquals(AggrGraphDataArrayList.empty(), readPointsSync(localId0));

            stockpileShard.forceMergeSync(MergeKind.DAILY);
            Assert.assertEquals(AggrGraphDataArrayList.empty(), readPointsSync(localId0)); // <--

            stockpileShard.forceMergeSync(MergeKind.ETERNITY);
            Assert.assertEquals(AggrGraphDataArrayList.empty(), readPointsSync(localId0));
        }
    }

    @Test
    public void testMergeOutOfOrder() {
        restart();

        long ts0 = timeProvider.nowMillis();
        long ts1 = timeProvider.nowMillis() + 15000;

        long localId0 = 8524895220L;

        pushBatchSinglePointSync(localId0, ts1, 3);
        stockpileShard.forceSnapshotSync();

        pushBatchSinglePointSync(localId0, ts0, 3);
        pushBatchSinglePointSync(localId0, ts1, 5);
        stockpileShard.forceSnapshotSync();

        stockpileShard.forceMergeSync(MergeKind.DAILY);
        stockpileShard.forceMergeSync(MergeKind.ETERNITY);

        Assert.assertEquals(listOf(point(ts0, 3), point(ts1, 5)), readPointsSync(localId0));
    }

    @Test
    public void readCroppedRawData() {
        restart();

        long localId = 8524895220L;
        pushBatchSync(
            request(localId,
                point("2017-04-24T12:20:00Z", 1),
                point("2017-04-24T12:28:00Z", 2),
                point("2017-04-24T12:32:00Z", 3),
                point("2017-04-24T12:35:00Z", 4),
                point("2017-04-24T12:50:00Z", 5)
            )
        );

        MetricArchiveImmutable result = readOne(StockpileMetricReadRequest
                .newBuilder()
                .setLocalId(localId)
                .setFrom("2017-04-24T12:25:00Z")
                .setTo("2017-04-24T12:40:00Z")
                .build())
                .join();

        AggrGraphDataArrayList graphData = result.toAggrGraphDataArrayList();

        AggrGraphDataArrayList expected = listOf(
            point("2017-04-24T12:28:00Z", 2),
            point("2017-04-24T12:32:00Z", 3),
            point("2017-04-24T12:35:00Z", 4)
        );

        Assert.assertThat(graphData, CoreMatchers.equalTo(expected));
        close(result);
    }

    @Test
    public void stockpileWriteReadSingleLogHistogram() {
        restart();
        long localId = 8524895220L;

        LogHistogram source = LogHistogram.newBuilder()
            .setBuckets(new double[]{55, 12, 1, 4, 9})
            .setMaxBucketsSize(10)
            .setStartPower(3)
            .setCountZero(13)
            .build();

        AggrPoint singlePoint = AggrPoint.shortPoint(timeMillis("2018-06-19T15:00:00Z"), source);

        pushBatchSync(request(localId, singlePoint));

        MetricArchiveImmutable result = readOne(
                StockpileMetricReadRequest.newBuilder()
                        .setLocalId(localId)
                        .setFrom("2018-06-19T13:00:00Z")
                        .setTo("2018-06-19T16:00:00Z")
                        .build()).join();

        AggrGraphDataArrayList resultList = result.toAggrGraphDataArrayList();
        Assert.assertThat(resultList, CoreMatchers.equalTo(AggrGraphDataArrayList.of(singlePoint)));
        close(result);
    }

    @Test
    public void stockpileWriteReadMultipleLogHistogram() {
        restart();
        long localId = 8524895220L;

        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
            AggrPoint.shortPoint(
                timeMillis("2018-06-19T15:00:00Z"),
                LogHistogram.newBuilder()
                    .setBuckets(new double[]{100, 22, 41, 1})
                    .setMaxBucketsSize(10)
                    .setStartPower(3)
                    .setCountZero(2)
                    .build()),

            AggrPoint.shortPoint(
                timeMillis("2018-06-19T15:01:00Z"),
                LogHistogram.newBuilder()
                    .setBuckets(new double[]{1, 0, 0, 80, 23, 41})
                    .setMaxBucketsSize(10)
                    .setStartPower(0)
                    .setCountZero(5)
                    .build()),

            AggrPoint.shortPoint(
                timeMillis("2018-06-19T15:02:00Z"),
                LogHistogram.newBuilder()
                    .setBuckets(new double[]{1, 4, 0, 80, 22, 40})
                    .setMaxBucketsSize(10)
                    .setStartPower(0)
                    .setCountZero(5)
                    .build()),

            AggrPoint.shortPoint(
                timeMillis("2018-06-19T15:03:00Z"),
                LogHistogram.newBuilder()
                    .setBuckets(new double[]{1441})
                    .setMaxBucketsSize(10)
                    .setStartPower(6)
                    .setCountZero(5)
                    .build())
        );

        pushBatchSync(request(localId, source));

        MetricArchiveImmutable result = readOne(
                StockpileMetricReadRequest.newBuilder()
                        .setLocalId(localId)
                        .setFrom("2018-06-19T14:00:00Z")
                        .setTo("2018-06-19T16:02:00Z")
                        .build()).join();

        AggrGraphDataArrayList resultList = result.toAggrGraphDataArrayList();
        Assert.assertEquals(source, resultList);
        close(result);
    }

    @Test
    public void writeHistogram() {
        restart();

        long localId = 8524895220L;
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                AggrPoint.builder()
                        .time("2017-06-13T16:00:00Z")
                        .histogram(histogram(
                                new double[]{50, 100, 200, 300},
                                new long[]{123, 21, 2, 1}))
                        .build(),

                AggrPoint.builder()
                        .time("2017-06-13T16:05:00Z")
                        .histogram(histogram(
                                new double[]{50, 100, 200, 300},
                                new long[]{221, 41, 20, 1}))
                        .build(),

                AggrPoint.builder()
                        .time("2017-06-13T16:10:00Z")
                        .histogram(histogram(
                                new double[]{50, 100, 200, 300},
                                new long[]{30, 10, 0, 0}))
                        .build()
        );
        pushBatchSync(request(localId, source));

        MetricArchiveImmutable result = readOne(StockpileMetricReadRequest
                .newBuilder()
                .setLocalId(localId)
                .setFrom("2017-06-13T16:00:00Z")
                .setTo("2017-06-13T16:20:00Z")
                .build())
                .join();

        AggrGraphDataArrayList line = result.toAggrGraphDataArrayList();
        Assert.assertThat(line, Matchers.equalTo(source));
        close(result);
    }

    @Test
    public void writeSummaryInt64() {
        restart();

        long localId = 8524895220L;
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                AggrPoint.builder()
                        .time("2017-06-13T16:00:00Z")
                        .summary(new ImmutableSummaryInt64Snapshot(5, 123, 2, 110))
                        .build(),

                AggrPoint.builder()
                        .time("2017-06-13T16:05:00Z")
                        .summary(new ImmutableSummaryInt64Snapshot(13, 200, 1, 131))
                        .build(),

                AggrPoint.builder()
                        .time("2017-06-13T16:10:00Z")
                        .summary(new ImmutableSummaryInt64Snapshot(42, 500, -10, 200))
                        .build());
        pushBatchSync(request(localId, source));

        MetricArchiveImmutable result = readOne(StockpileMetricReadRequest
                .newBuilder()
                .setLocalId(localId)
                .setFrom("2017-06-13T16:00:00Z")
                .setTo("2017-06-13T16:20:00Z")
                .build())
                .join();

        AggrGraphDataArrayList line = result.toAggrGraphDataArrayList();
        Assert.assertThat(line, Matchers.equalTo(source));
        close(result);
    }

    @Test
    public void writeSummaryDouble() {
        restart();

        long localId = 8524895220L;
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                AggrPoint.builder()
                        .time("2017-06-13T16:00:00Z")
                        .summary(new ImmutableSummaryDoubleSnapshot(5, 123.21, 2.1, 110.4))
                        .build(),

                AggrPoint.builder()
                        .time("2017-06-13T16:05:00Z")
                        .summary(new ImmutableSummaryDoubleSnapshot(13, 200.2, 1.2, 131.5))
                        .build(),

                AggrPoint.builder()
                        .time("2017-06-13T16:10:00Z")
                        .summary(new ImmutableSummaryDoubleSnapshot(42, 500.3, -10.4, 200.6))
                        .build());
        pushBatchSync(request(localId, source));

        MetricArchiveImmutable result = readOne(
                StockpileMetricReadRequest.newBuilder()
                        .setLocalId(localId)
                        .setFrom("2017-06-13T16:00:00Z")
                        .setTo("2017-06-13T16:20:00Z")
                        .build()).join();

        AggrGraphDataArrayList line = result.toAggrGraphDataArrayList();
        Assert.assertThat(line, Matchers.equalTo(source));
        close(result);
    }

    @Test
    public void exceptionOnRead() {
        long ts0 = Instant.parse("2015-10-22T12:13:14Z").toEpochMilli();
        long localId = 11234567;

        restart();

        pushBatchSinglePointSync(localId, ts0, 22);

        stockpileShard.forceSnapshotSync();

        kikimrKvClientInMem.throwOnRead();

        try {
            readPointsSync(localId);
            Assert.fail();
        } catch (Exception e) {
            // expected
        }

        kikimrKvClientInMem.resumeReads();

        Assert.assertEquals(listOf(point(ts0, 22)), readPointsSync(localId));
    }

    @Test
    public void rateAvailableOnReadOnlyAsGauge() {
        restart();

        final long localId = StockpileLocalId.random();

        MetricArchiveMutable source = new MetricArchiveMutable();
        source.setType(MetricType.RATE);
        source.addAll(AggrGraphDataArrayList.of(
                lpoint("2018-08-07T14:36:00Z", 100),
                lpoint("2018-08-07T14:36:10Z", 200),
                lpoint("2018-08-07T14:36:20Z", 300),
                // gap here
                lpoint("2018-08-07T14:36:40Z", 10),
                lpoint("2018-08-07T14:36:50Z", 80),
                lpoint("2018-08-07T14:37:00Z", 300)));
        pushBatchSync(request(localId, source));

        MetricArchiveMutable expected = new MetricArchiveMutable();
        expected.setType(MetricType.DGAUGE);
        expected.addAll(AggrGraphDataArrayList.of(
                // absent
                point("2018-08-07T14:36:10Z", 10),
                point("2018-08-07T14:36:20Z", 10),
                point("2018-08-07T14:36:40Z", 10),
                point("2018-08-07T14:36:50Z", 7),
                point("2018-08-07T14:37:00Z", 22)));

        MetricArchiveImmutable archive = read(localId).join();
        AggrGraphDataArrayList list = archive.toAggrGraphDataArrayList();
        list.foldDenomIntoOne();
        assertEquals(expected.header(), archive.header());
        assertEquals(expected.toAggrGraphDataArrayList(), list);
        close(source, expected, archive);
    }

    @Test
    public void ignoreInvalidMetricTypes() {
        restart();

        final long localId = StockpileLocalId.random();
        MetricArchiveMutable one = new MetricArchiveMutable();
        one.setType(MetricType.DGAUGE);
        one.addRecord(point("2017-06-13T16:00:00Z", 42.0));
        pushBatchSync(request(localId, one));

        MetricArchiveMutable two = new MetricArchiveMutable();
        two.setType(MetricType.HIST);
        two.addRecord(randomPoint(MetricType.HIST));
        pushBatchSync(request(localId, two));

        MetricArchiveMutable three = new MetricArchiveMutable();
        three.setType(MetricType.DGAUGE);
        three.addRecord(point("2017-06-13T17:00:00Z", 2.0));
        pushBatchSync(request(localId, three));

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point("2017-06-13T16:00:00Z", 42.0),
                point("2017-06-13T17:00:00Z", 2.0));

        assertEquals(expected, readPointsSync(localId));
        close(one, two, three);
    }

    @Test
    public void ignoreInvalidMetricTypesCacheUpdate() {
        restart();
        stockpileShard.setMaxCacheSize(100500);

        final long localId = StockpileLocalId.random();
        MetricArchiveMutable one = new MetricArchiveMutable();
        one.setType(MetricType.DGAUGE);
        one.addRecord(point("2017-06-13T16:00:00Z", 42.0));
        pushBatchSync(request(localId, one));
        // warm cache
        assertEquals(AggrGraphDataArrayList.of(point("2017-06-13T16:00:00Z", 42.0)), readPointsSync(localId));

        MetricArchiveMutable two = new MetricArchiveMutable();
        two.setType(MetricType.HIST);
        two.addRecord(randomPoint(MetricType.HIST));
        pushBatchSync(request(localId, two));

        MetricArchiveMutable three = new MetricArchiveMutable();
        three.setType(MetricType.DGAUGE);
        three.addRecord(point("2017-06-13T17:00:00Z", 2.0));
        pushBatchSync(request(localId, three));

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point("2017-06-13T16:00:00Z", 42.0),
                point("2017-06-13T17:00:00Z", 2.0));

        assertEquals(expected, readPointsSync(localId));
        close(one, two, three);
    }

    @Test
    public void ignoreInvalidMetricTypesDuringReadLogs() {
        restart();

        final long localId = StockpileLocalId.random();
        MetricArchiveMutable one = new MetricArchiveMutable();
        one.setType(MetricType.DGAUGE);
        one.addRecord(point("2017-06-13T16:00:00Z", 42.0));
        pushBatchSync(request(localId, one));
        restart();

        MetricArchiveMutable two = new MetricArchiveMutable();
        two.setType(MetricType.HIST);
        two.addRecord(randomPoint(MetricType.HIST));
        pushBatchSync(request(localId, two));
        restart();

        MetricArchiveMutable three = new MetricArchiveMutable();
        three.setType(MetricType.DGAUGE);
        three.addRecord(point("2017-06-13T17:00:00Z", 2.0));
        pushBatchSync(request(localId, three));
        restart();

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point("2017-06-13T16:00:00Z", 42.0),
                point("2017-06-13T17:00:00Z", 2.0));

        assertEquals(expected, readPointsSync(localId));
        close(one, two, three);
    }

    @Test
    public void ignoreInvalidMetricTypesDuringLoadSnapshots() {
        restart();

        final long localId = StockpileLocalId.random();
        MetricArchiveMutable one = new MetricArchiveMutable();
        one.setType(MetricType.DGAUGE);
        one.addRecord(point("2017-06-13T16:00:00Z", 42.0));
        pushBatchSync(request(localId, one));
        stockpileShard.forceSnapshot().join();
        restart();

        MetricArchiveMutable two = new MetricArchiveMutable();
        two.setType(MetricType.HIST);
        two.addRecord(randomPoint(MetricType.HIST));
        pushBatchSync(request(localId, two));
        stockpileShard.forceSnapshot().join();
        restart();

        MetricArchiveMutable three = new MetricArchiveMutable();
        three.setType(MetricType.DGAUGE);
        three.addRecord(point("2017-06-13T17:00:00Z", 2.0));
        pushBatchSync(request(localId, three));
        stockpileShard.forceSnapshot().join();
        restart();

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point("2017-06-13T16:00:00Z", 42.0),
                point("2017-06-13T17:00:00Z", 2.0));

        assertEquals(expected, readPointsSync(localId));
        close(one, two, three);
    }

    @Test
    public void generationChangeDuringMergeProcessUnloadShard() throws InterruptedException {
        do {
            restart();
            long localId = StockpileLocalId.random();
            for (int index = 0; index < 100; index++) {
                pushBatchSync(request(localId, randomPoint(MetricType.DGAUGE)));
                stockpileShard.forceSnapshotSync();
            }
            var future = stockpileShard.forceMerge(MergeKind.DAILY);
            TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextLong(0, 100));
            kikimrKvClientInMem.incrementGeneration(tabletId, 0).join();
            try {
                future.join();
            } catch (Throwable e) {
                // ok
            }
        } while (!stockpileShard.isGenerationChanged());
        assertTrue(stockpileShard.isStop());
    }

    @Test
    public void generationChangeDuringWriteProcessUnloadShard() throws InterruptedException {
        restart();
        do {
            long localId = StockpileLocalId.random();
            var future = stockpileShard.pushBatch(request(localId, randomPoint(MetricType.DGAUGE)));
            TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextLong(0, 10));
            kikimrKvClientInMem.incrementGeneration(tabletId, 0).join();
            try {
                future.join();
            } catch (Throwable e) {
                // ok
            }
        } while (!stockpileShard.isGenerationChanged());
        assertTrue(stockpileShard.isStop());
    }

    @Test
    public void generationChangeDuringSnapshotProcessUnloadShard() throws InterruptedException {
        restart();
        do {
            long localId = StockpileLocalId.random();
            var future = stockpileShard.pushBatch(request(localId, randomPoint(MetricType.DGAUGE)));
            TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextLong(0, 10));
            kikimrKvClientInMem.incrementGeneration(tabletId, 0).join();
            try {
                future.join();
            } catch (Throwable e) {
                // ok
            }
        } while (!stockpileShard.isGenerationChanged());
        assertTrue(stockpileShard.isStop());
    }

    @Test
    public void generationChangeDuringReadProcessUnloadShard() throws InterruptedException {
        do {
            restart();
            long localId = StockpileLocalId.random();
            for (int index = 0; index < 100; index++) {
                pushBatchSync(request(localId, randomPoint(MetricType.DGAUGE)));
                stockpileShard.forceSnapshotSync();
            }
            var future = read(localId);
            TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextLong(0, 10));
            kikimrKvClientInMem.incrementGeneration(tabletId, 0).join();
            try {
                var archive = future.join();
                archive.close();
            } catch (Throwable e) {
                // ok
            }
        } while (!stockpileShard.isGenerationChanged());
        assertTrue(stockpileShard.isStop());
    }

    @Test
    public void generationChangeDuringLoadProcessUnloadShard() throws InterruptedException {
        restart();

        do {
            logger.info("Generate logs for shard");
            AggrPoint point = new AggrPoint();
            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int index = 0; index < 100; index++) {
                long localId = StockpileLocalId.random();
                point.setTsMillis(TsRandomData.randomTs(random));
                point.setValue(ValueRandomData.randomNum(random));
                pushOnePoint(localId, MetricType.DGAUGE, point);
            }

            stockpileShard.stop();
            logger.info("Init load process");
            stockpileShard = new StockpileShard(stockpileShardGlobals, stockpileShard.shardId, tabletId, testName.getMethodName());
            stockpileShard.storage.lock().join();
            stockpileShard.start();

            TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextLong(0, 10));
            logger.info("Generation changed");
            kikimrKvClientInMem.incrementGeneration(tabletId, 0).join();

            try {
                stockpileShard.waitForInitializedOrAnyError();
            } catch (Throwable e) {
                // ok
            }
        } while (!stockpileShard.isGenerationChanged());
        assertTrue(stockpileShard.isStop());
    }

    @Test
    public void aggregateSplitBySnapshotKindChange() {
        restart();

        final long step = 10_000;
        final long ts0 = Instant.parse("2018-08-20T09:05:00Z").toEpochMilli();
        final long ts1 = ts0 + step;
        final long ts2 = ts1 + step;
        final long localId = StockpileLocalId.random();

        // fake point that will be dropped by deriv
        pushOnePoint(localId, MetricType.DGAUGE, point(ts0 - step, 0.0, true, 3, step)).join();

        // snapshot one
        {
            // ts0
            pushOnePoint(localId, MetricType.DGAUGE, point(ts0, 2.0, true, 1, step));
            pushOnePoint(localId, MetricType.DGAUGE, point(ts0, 3.0, true, 1, step));
            pushOnePoint(localId, MetricType.DGAUGE, point(ts0, 1.0, true, 1, step));

            // ts1
            pushOnePoint(localId, MetricType.DGAUGE, point(ts1, 1.0, true, 1, step));
            pushOnePoint(localId, MetricType.DGAUGE, point(ts1, 2.0, true, 1, step)).join();
            stockpileShard.forceSnapshotSync();
        }

        // snapshot two
        {
            // ts1
            pushOnePoint(localId, MetricType.DGAUGE, point(ts1, 4.0, true, 1, step));

            // ts2
            pushOnePoint(localId, MetricType.RATE, lpoint(ts2, 60, true, 1, step));
            pushOnePoint(localId, MetricType.RATE, lpoint(ts2, 40, true, 1, step));
            pushOnePoint(localId, MetricType.RATE, lpoint(ts2, 50, true, 1, step)).join();
            stockpileShard.forceSnapshotSync();
        }

        AggrGraphDataArrayList expectedRate = AggrGraphDataArrayList.of(
                lpoint(ts0-step, 0, true, 3, step),
                lpoint(ts0, 60, true, 3, step),
                lpoint(ts1, 130, true, 3, step),
                lpoint(ts2, 240, true, 3, step));

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(MetricTypeTransfers.of(MetricType.RATE, MetricType.DGAUGE, expectedRate.iterator()));
        AggrGraphDataArrayList result = readPointsSync(localId);
        assertEquals(expected, result);
    }

    @Test
    public void logsDiskSizeActual() {
        restart();

        {
            SizeAndCount init = stockpileShard.diskStats().get(FileKind.LOG);
            assertEquals(0, init.count());
            assertEquals(0, init.size());
        }

        final long localId = StockpileLocalId.random();
        final long ts0 = Instant.parse("2018-08-20T09:05:00Z").toEpochMilli();
        pushOnePoint(localId, MetricType.DGAUGE, point(ts0 - 1_000, 42)).join();
        ensureMemLogsSizeSameAsDiskSize();

        SizeAndCount one = stockpileShard.diskStats().get(FileKind.LOG);
        assertEquals(1, one.count());
        assertTrue(one.size() > Long.BYTES);

        // push few points with the same
        for (int index = 0; index < 3; index++) {
            pushOnePoint(localId, MetricType.DGAUGE, point(ts0 - 1_000, 42)).join();
            ensureMemLogsSizeSameAsDiskSize();
        }

        SizeAndCount two = stockpileShard.diskStats().get(FileKind.LOG);
        assertEquals(4, two.count());
        assertEquals(one.size() * 4, two.size());

        stockpileShard.forceLogSnapshot().join();
        SizeAndCount tree = stockpileShard.diskStats().get(FileKind.LOG);

        ensureMemLogsSizeSameAsDiskSize();
        assertEquals(1, tree.count());
        assertEquals(one.size(), tree.size(), 2);
    }

    @Test
    public void logsDiskSizeActual2() {
        restart();

        final long localId = StockpileLocalId.random();
        for (int snapshotIndex = 0; snapshotIndex < 5; snapshotIndex++) {
            final int points = ThreadLocalRandom.current().nextInt(10, 50);
            for (int pointIndex = 0; pointIndex < points; pointIndex++) {
                pushOnePoint(localId, MetricType.DGAUGE, randomPoint(MetricType.DGAUGE)).join();
            }
            stockpileShard.forceLogSnapshot().join();
            ensureMemLogsSizeSameAsDiskSize();
        }
    }

    @Test
    public void snapshotLogsLastTxHuge() {
        restart();

        logger.info("write 3 small logs tx");
        ThreadLocalRandom random = ThreadLocalRandom.current();
        final long localId = StockpileLocalId.random(random);
        for (int index = 0; index < 3; index++) {
            pushOnePoint(localId, MetricType.DGAUGE, randomPoint(MetricType.DGAUGE)).join();
        }
        logger.info("small logs generated");

        long lastLocalId = localId;
        var builder = StockpileWriteRequest.newBuilder();
        MetricArchiveMutable archive = new MetricArchiveMutable();
        archive.setType(MetricType.DGAUGE);
        archive.addRecord(randomPoint(MetricType.DGAUGE));

        int bytes = 0;
        while (bytes < KikimrKvClient.DO_NOT_EXCEED_FILE_SIZE * 10) {
            builder.addArchiveCopy(lastLocalId++, archive.cloneToUnsealed());
            bytes += archive.memorySizeIncludingSelfInt() + Long.BYTES;
        }

        logger.info("write huge log: "+ DataSize.shortString(bytes));
        pushBatchSync(builder.build());
        logger.info("huge write, done start snapshot");

        stockpileShard.forceLogSnapshot().join();

        logger.info("write one more log after snapshot");
        pushOnePoint(lastLocalId + 1, MetricType.DGAUGE, randomPoint(MetricType.DGAUGE));
        logger.info("restart shard");
        restart();
        stockpileShard.forceSnapshot().join();
    }

    @Test
    public void snapshotLogsFirstTxHuge() {
        restart();

        long lastLocalId = StockpileLocalId.random();
        var builder = StockpileWriteRequest.newBuilder();
        MetricArchiveMutable archive = new MetricArchiveMutable();
        archive.setType(MetricType.DGAUGE);
        archive.addRecord(randomPoint(MetricType.DGAUGE));

        int bytes = 0;
        while (bytes < KikimrKvClient.DO_NOT_EXCEED_FILE_SIZE * 10) {
            builder.addArchiveCopy(lastLocalId++, archive);
            bytes += archive.memorySizeIncludingSelfInt() + Long.BYTES;
        }

        logger.info("write huge log: "+ DataSize.shortString(bytes));
        pushBatchSync(builder.build());
        logger.info("huge write done");

        logger.info("write 3 small logs tx");
        ThreadLocalRandom random = ThreadLocalRandom.current();
        final long localId = StockpileLocalId.random(random);
        for (int index = 0; index < 3; index++) {
            pushOnePoint(localId, MetricType.DGAUGE, randomPoint(MetricType.DGAUGE)).join();
        }
        logger.info("small logs generated, trigger snapshot process");

        stockpileShard.forceLogSnapshot().join();

        logger.info("restart shard");
        restart();
        stockpileShard.forceSnapshot().join();
        close(archive);
    }

    @Test(timeout = 60_000)
    public void writeLogsAvailableDuringLoading() {
        restart();

        long localId = StockpileLocalId.random();
        long ts0 = Instant.parse("2018-08-20T09:05:00Z").toEpochMilli();

        // Fill merge=true, count=1 to guarantee that point write only once during load process
        AggrPoint point = new AggrPoint();
        point.setStepMillis(10_000);
        point.setMerge(true);
        point.setCount(1);

        int writtenPoints = 0;
        while (stockpileShard.diskStats().get(FileKind.LOG).count() < 90) {
            point.setTsMillis(ts0 + writtenPoints * 10_000);
            point.setValue(writtenPoints);
            pushOnePoint(localId, MetricType.DGAUGE, point).join();
            writtenPoints++;

            int logs = stockpileShard.diskStats().get(FileKind.LOG).count();
            logger.info("shard count logs: {}", logs);
            if (logs >= 90) {
                break;
            }
        }

        restartNoWait();
        int writesOnLoad = 0;
        while (writesOnLoad < 20 || stockpileShard.getLoadState() != StockpileShard.LoadState.DONE) {
            point.setTsMillis(ts0 + writtenPoints * 10_000);
            point.setValue(writtenPoints);
            long txn = pushOnePoint(localId, MetricType.DGAUGE, point)
                .thenApply(Txn::getTxn)
                .exceptionally(throwable -> 0L)
                .join();

            if (txn == 0L) {
                continue;
            }

            writtenPoints++;
            if (stockpileShard.getLoadState() != StockpileShard.LoadState.DONE) {
                logger.info("during shard load, txn {} success write {}", txn, point);
                writesOnLoad++;
            } else if (writesOnLoad < 20) {
                logger.info("shard already loaded, restart one more time and try writes during load, success writes on load");
                restartNoWait();
            }
        }

        AggrGraphDataArrayList expected = new AggrGraphDataArrayList();
        for (int i = 0; i < writtenPoints; i++) {
            point.setTsMillis(ts0 + i * 10_000);
            point.setValue(i);
            expected.addRecord(point);
        }

        stockpileShard.waitForInitializedOrAnyError();
        AggrGraphDataArrayList loaded = readPointsSync(localId);
        logger.info("\ne: {}\nl: {}", expected, loaded);
        assertEquals(expected, loaded);
    }

    @Test(timeout = 60_000)
    public void writeLogsSnapshotAvailableDuringLoading() {
        restart();

        long localId = StockpileLocalId.random();
        long ts0 = Instant.parse("2018-08-20T09:05:00Z").toEpochMilli();

        // Fill merge=true, count=1 to guarantee that point write only once during load process
        AggrPoint point = new AggrPoint();
        point.setStepMillis(10_000);
        point.setMerge(true);
        point.setCount(1);

        int writtenPoints = 0;
        while (stockpileShard.diskStats().get(FileKind.LOG).count() < 90) {
            point.setTsMillis(ts0 + writtenPoints * 10_000);
            point.setValue(writtenPoints);
            pushOnePoint(localId, MetricType.DGAUGE, point).join();
            writtenPoints++;

            int logs = stockpileShard.diskStats().get(FileKind.LOG).count();
            logger.info("shard count logs: {}", logs);
            if (logs >= 90) {
                break;
            }
        }

        restartNoWait();
        int writesOnLoad = 0;
        while (writesOnLoad < 20 || stockpileShard.getLoadState() != StockpileShard.LoadState.DONE) {
            point.setTsMillis(ts0 + writtenPoints * 10_000);
            point.setValue(writtenPoints);
            long txn = pushOnePoint(localId, MetricType.DGAUGE, point)
                .thenApply(Txn::getTxn)
                .exceptionally(throwable -> 0L)
                .join();

            logger.info("write failed for point: {}", point);
            if (txn == 0L) {
                continue;
            }

            writtenPoints++;
            if (stockpileShard.getLoadState() != StockpileShard.LoadState.DONE) {
                logger.info("during shard load, txn {} success write {}", txn, point);
                writesOnLoad++;
            } else if (writesOnLoad < 20) {
                logger.info("shard already loaded, restart one more time and try writes during load, success writes on load");
                restartNoWait();
            }

            if (writesOnLoad % 4 == 0) {
                // don't wait complete force log snapshot, because write can be process in parallel
                stockpileShard.forceLogSnapshot();
            }
        }

        AggrGraphDataArrayList expected = new AggrGraphDataArrayList();
        for (int i = 0; i < writtenPoints; i++) {
            point.setTsMillis(ts0 + i * 10_000);
            point.setValue(i);
            expected.addRecord(point);
        }

        stockpileShard.waitForInitializedOrAnyError();
        AggrGraphDataArrayList loaded = readPointsSync(localId);
        logger.info("\ne: {}\nl: {}", expected, loaded);
        int size = Math.max(expected.length(), loaded.length());
        for (int index = 0; index < size; index++) {
            assertEquals(expected.getAnyPoint(index), loaded.getAnyPoint(index));
        }
        assertEquals(expected, loaded);
    }

    @Test
    public void statisticsByProject() {
        final EProjectId project = EProjectId.GOLOVAN;
        restart();

        assertEquals(0, stockpileShard.indexStats().getTotalByLevel().getStatsByProject(project).getTotalByKinds().metrics);
        assertEquals(0, stockpileShard.indexStats().getTotalByLevel().getStatsByProject(project).getTotalByKinds().records);

        MetricArchiveMutable archive = new MetricArchiveMutable();
        archive.setType(MetricType.DGAUGE);
        archive.setOwnerProjectIdEnum(EProjectId.GOLOVAN);
        archive.addRecord(randomPoint(archive.getType()));

        pushBatchSync(request(StockpileLocalId.random(), archive));
        stockpileShard.forceLogSnapshot().join();
        stockpileShard.forceSnapshot().join();

        assertEquals(1, stockpileShard.indexStats().getTotalByLevel().getStatsByProject(project).getTotalByKinds().metrics);
        assertEquals(1, stockpileShard.indexStats().getTotalByLevel().getStatsByProject(project).getTotalByKinds().records);

        stockpileShard.forceMergeSync(MergeKind.DAILY);

        assertEquals(1, stockpileShard.indexStats().getTotalByLevel().getStatsByProject(project).getTotalByKinds().metrics);
        assertEquals(1, stockpileShard.indexStats().getTotalByLevel().getStatsByProject(project).getTotalByKinds().records);

        stockpileShard.forceMergeSync(MergeKind.ETERNITY);

        assertEquals(1, stockpileShard.indexStats().getTotalByLevel().getStatsByProject(project).getTotalByKinds().metrics);
        assertEquals(1, stockpileShard.indexStats().getTotalByLevel().getStatsByProject(project).getTotalByKinds().records);
        close(archive);
    }

    @Test
    public void ableToMakeForceLogSnapshotDuringDailyMerge() {
        restart();
        long localId = StockpileLocalId.random();
        long now = System.currentTimeMillis();

        AggrGraphDataArrayList expected = new AggrGraphDataArrayList();

        CompletableFuture merge;
        CompletableFuture snapshot;
        do {
            for (int index = 0; index < 100; index++) {
                AggrPoint point = new AggrPoint();
                point.setValue(index);
                point.setTsMillis(now);
                now += 10_000;
                pushOnePoint(localId, MetricType.DGAUGE, point).join();
                expected.addRecord(point);
                // junk
                pushOnePoint(StockpileLocalId.random(), MetricType.DGAUGE, randomPoint(MetricType.DGAUGE));
            }
            stockpileShard.forceSnapshot().join();

            for (int index = 100; index < 110; index++) {
                AggrPoint point = new AggrPoint();
                point.setValue(index);
                point.setTsMillis(now);
                now += 10_000;
                pushOnePoint(localId, MetricType.DGAUGE, point).join();
                expected.addRecord(point);
            }

            merge = stockpileShard.forceMerge(MergeKind.DAILY);
            pushOnePoint(StockpileLocalId.random(), MetricType.DGAUGE, randomPoint(MetricType.DGAUGE)).join();
            snapshot = stockpileShard.forceSnapshot();
            snapshot.join();
        } while (snapshot.isDone() && merge.isDone());

        merge.join();
        restart();

        var result = readPointsSync(localId);
        assertEquals(expected.getRecordCount(), result.getRecordCount());
        assertEquals(expected, result);

        restart();
        var result2 = readPointsSync(localId);
        assertEquals(expected.getRecordCount(), result2.getRecordCount());
        assertEquals(expected, result2);
    }

    @Test
    public void ableToMakeForceLogSnapshotDuringEternityMerge() {
        restart();
        long localId = StockpileLocalId.random();
        long now = System.currentTimeMillis();

        AggrGraphDataArrayList expected = new AggrGraphDataArrayList();

        CompletableFuture merge;
        CompletableFuture snapshot;
        do {
            for (int index = 0; index < 100; index++) {
                AggrPoint point = new AggrPoint();
                point.setValue(index);
                point.setTsMillis(now);
                now += 10_000;
                pushOnePoint(localId, MetricType.DGAUGE, point).join();
                expected.addRecord(point);
                // junk
                pushOnePoint(StockpileLocalId.random(), MetricType.DGAUGE, randomPoint(MetricType.DGAUGE));
            }
            stockpileShard.forceSnapshot().join();

            for (int index = 100; index < 110; index++) {
                AggrPoint point = new AggrPoint();
                point.setValue(index);
                point.setTsMillis(now);
                now += 10_000;
                pushOnePoint(localId, MetricType.DGAUGE, point).join();
                expected.addRecord(point);
            }

            merge = stockpileShard.forceMerge(MergeKind.ETERNITY);
            pushOnePoint(StockpileLocalId.random(), MetricType.DGAUGE, randomPoint(MetricType.DGAUGE)).join();
            snapshot = stockpileShard.forceSnapshot();
            snapshot.join();
        } while (snapshot.isDone() && merge.isDone());

        merge.join();
        var result = readPoints(localId).join();
        assertEquals(expected.getRecordCount(), result.getRecordCount());
        assertEquals(expected, result);

        restart();
        var result2 = readPointsSync(localId);
        assertEquals(expected.getRecordCount(), result2.getRecordCount());
        assertEquals(expected, result2);
    }

    @Test
    public void dontLostRequestsOnLogSnapshot() {
        restart();

        final var maxPoints = 5_000_000;
        final var mask = TsColumn.mask | ValueColumn.mask;
        final var localId = StockpileLocalId.random();
        final var expected = new AggrGraphDataArrayList(mask, maxPoints);
        final var random = ThreadLocalRandom.current();

        CompletableFuture snapshot = completedFuture(null);
        CompletableFuture write = completedFuture(null);
        CompletableFuture<Void> failed = new CompletableFuture<>();

        AtomicLong inFlight = new AtomicLong();
        AggrPoint point = new AggrPoint();
        while (stockpileShard.getStats().writeLogSnapshots < 10 && !failed.isDone() && expected.length() < maxPoints) {
            randomPoint(point, mask);
            expected.addRecord(point);

            var builder = StockpileWriteRequest.newBuilder();
            builder.archiveRef(localId).addRecord(point);

            inFlight.incrementAndGet();
            write = stockpileShard.pushBatch(builder.build())
                .whenComplete((ignore, e) -> {
                    if (e != null) {
                        failed.completeExceptionally(e);
                    } else {
                        inFlight.decrementAndGet();
                    }
                });

            if (snapshot.isDone() && random.nextDouble() < 0.1) {
                snapshot = stockpileShard.forceLogSnapshot();
                continue;
            }

            if (inFlight.get() > 100_000) {
                logger.info("too many inFlight " + inFlight.get() + ", wait");
                write.join();
            }
        }

        expected.sortAndMerge();
        failed.complete(null);
        CompletableFuture.allOf(snapshot, write, failed).join();
        restart();

        logger.debug("written {} points", expected.length());
        var response = stockpileShard.readOne(StockpileMetricReadRequest.newBuilder()
            .setLocalId(localId)
            .build()).join()
            .getTimeseries();

        var result = AggrGraphDataArrayList.of(response);
        assertPoints(expected, result);
    }

    @Test
    public void dontLostRequestOnTwoHoursSnapshot() {
        restart();

        final var maxPoints = 5_000_000;
        final var mask = TsColumn.mask | ValueColumn.mask;
        final var localId = StockpileLocalId.random();
        final var expected = new AggrGraphDataArrayList(mask, maxPoints);
        final var random = ThreadLocalRandom.current();

        CompletableFuture snapshot = completedFuture(null);
        CompletableFuture write = completedFuture(null);
        CompletableFuture<Void> failed = new CompletableFuture<>();

        AtomicLong cntSnapshots = new AtomicLong();
        AtomicLong inFlight = new AtomicLong();
        AggrPoint point = new AggrPoint();
        while (cntSnapshots.get() < 10 && !failed.isDone() && expected.length() < maxPoints) {
            randomPoint(point, mask);
            expected.addRecord(point);

            var builder = StockpileWriteRequest.newBuilder();
            builder.archiveRef(localId).addRecord(point);

            inFlight.incrementAndGet();
            write = stockpileShard.pushBatch(builder.build())
                .whenComplete((ignore, e) -> {
                    if (e != null) {
                        failed.completeExceptionally(e);
                    } else {
                        inFlight.decrementAndGet();
                    }
                });

            if (snapshot.isDone() && random.nextDouble() < 0.5) {
                snapshot = stockpileShard.forceSnapshot()
                    .thenAccept(txn -> {
                        if (txn != null) {
                            cntSnapshots.incrementAndGet();
                        }
                    });
                continue;
            }

            if (inFlight.get() > 100_000) {
                logger.info("too many inFlight " + inFlight.get() + ", wait");
                write.join();
            }
        }

        expected.sortAndMerge();
        failed.complete(null);
        CompletableFuture.allOf(snapshot, write, failed).join();
        restart();

        logger.debug("written {} points", expected.length());
        var response = stockpileShard.readOne(StockpileMetricReadRequest.newBuilder()
            .setLocalId(localId)
            .build()).join()
            .getTimeseries();

        var result = AggrGraphDataArrayList.of(response);
        assertPoints(expected, result);
    }

    @Test
    public void pointsDoNotLostDuringMerge() {
        restart();

        final var maxPoints = 5_000_000;
        final var mask = TsColumn.mask | ValueColumn.mask;
        final var localId = StockpileLocalId.random();
        final var expected = new AggrGraphDataArrayList(mask, maxPoints);
        final var random = ThreadLocalRandom.current();

        CompletableFuture twoHours = completedFuture(null);
        CompletableFuture daily = completedFuture(null);
        CompletableFuture eternity = completedFuture(null);
        CompletableFuture write = completedFuture(null);
        CompletableFuture failed = new CompletableFuture();

        AtomicInteger cntEternity = new AtomicInteger();
        AtomicLong inFlight = new AtomicLong();
        AggrPoint point = new AggrPoint();
        while (cntEternity.get() < 3 && !failed.isDone() && expected.length() < maxPoints) {
            double num = random.nextDouble();

            randomPoint(point, mask);
            expected.addRecord(point);

            var builder = StockpileWriteRequest.newBuilder();
            builder.archiveRef(localId).addRecord(point);
            if (num < 0.1) {
                // noise
                builder.archiveRef(StockpileLocalId.random()).addRecord(randomPoint(point, mask, random));
            }

            write = stockpileShard.pushBatch(builder.build())
                .whenComplete((ignore, e) -> {
                    if (e != null) {
                        failed.completeExceptionally(e);
                    } else {
                        inFlight.decrementAndGet();
                    }
                });

            if (num < 0.5) {
                stockpileShard.forceLogSnapshot();
            }

            if (num < 0.4 && twoHours.isDone()) {
                twoHours = stockpileShard.forceSnapshot();
            }

            if (daily.isDone() && eternity.isDone()) {
                if (num < 0.2) {
                    eternity = stockpileShard.forceMerge(MergeKind.ETERNITY).thenRun(cntEternity::incrementAndGet);
                } else if (num < 0.3) {
                    daily = stockpileShard.forceMerge(MergeKind.DAILY);
                }
            }

            if (inFlight.get() > 100_000) {
                logger.info("too many inFlight " + inFlight.get() + ", wait");
                write.join();
            }
        }

        expected.sortAndMerge();
        failed.complete(null);
        CompletableFuture.allOf(write, twoHours, daily, eternity, failed).join();
        restart();

        logger.debug("written {} points", expected.length());
        var response = stockpileShard.readOne(StockpileMetricReadRequest.newBuilder()
            .setLocalId(localId)
            .build()).join()
            .getTimeseries();

        var result = AggrGraphDataArrayList.of(response);
        assertPoints(expected, result);
    }

    @Test
    public void negativeOwnerShardId() {
        restart();

        long localId = StockpileLocalId.random();
        int ownerShardId = -1141592490;

        MetricArchiveMutable expected = new MetricArchiveMutable();
        expected.setOwnerProjectIdEnum(EProjectId.SOLOMON);
        expected.setOwnerShardId(ownerShardId);
        expected.addRecord(point(System.currentTimeMillis(), 42));
        MetricArchiveImmutable immutable = expected.toImmutableNoCopy();

        pushBatchSync(request(localId, expected));
        {
            var result = read(localId).join();
            assertEquals(ownerShardId, result.getOwnerShardId());
            assertEquals(expected.header(), result.header());
            assertEquals(immutable, result);
            close(result);
        }

        {
            stockpileShard.forceSnapshot().join();
            var result = read(localId).join();
            assertEquals(ownerShardId, result.getOwnerShardId());
            assertEquals(expected.header(), result.header());
            assertEquals(immutable, result);
            close(result);
        }

        {
            stockpileShard.forceMergeSync(MergeKind.DAILY);
            var result = read(localId).join();
            assertEquals(ownerShardId, result.getOwnerShardId());
            assertEquals(expected.header(), result.header());
            assertEquals(immutable, result);
            close(result);
        }

        var statsByOwner = stockpileShard.indexStats()
            .getTotalByLevel()
            .getStatsByProject(EProjectId.SOLOMON)
            .getByOwner();

        assertTrue(statsByOwner.containsKey(ownerShardId));
        close(expected, immutable);
    }

    @Test
    public void txWriteCorrectCountingDuplicate() {
        restart();

        long localId = StockpileLocalId.random();
        int ownerShardId = 142;

        MetricArchiveMutable expected = new MetricArchiveMutable();
        expected.setOwnerProjectIdEnum(EProjectId.SOLOMON);
        expected.setOwnerShardId(ownerShardId);
        expected.addRecord(point(System.currentTimeMillis() - 15_000, 41));
        expected.addRecord(point(System.currentTimeMillis(), 42));

        // push same point multiple times
        IntStream.range(0, 100)
            .mapToObj(ignore -> stockpileShard.pushBatch(StockpileWriteRequest.newBuilder()
                    .addArchiveCopy(localId, expected)
                    .build()))
            .collect(Collectors.collectingAndThen(Collectors.toList(), CompletableFutures::allOfVoid))
            .join();

        String statistics = billingStatistics();
        assertThat(statistics, containsString("RATE stockpile.write.records.rate{kind='DGAUGE', ownerId='142', producer='total', projectId='SOLOMON'} [200]"));
        assertThat(statistics, containsString("RATE stockpile.write.sensors.rate{kind='DGAUGE', ownerId='142', producer='total', projectId='SOLOMON'} [100]"));
        close(expected);
    }

    @Test
    public void memoryLimitWork() {
        restart();

        stockpileShard.setMemoryLimit(2024);
        MetricArchiveMutable archive = new MetricArchiveMutable();
        archive.addRecord(randomPoint(MetricType.DGAUGE));
        while (archive.memorySizeIncludingSelf() < 1024) {
            var point = randomPoint(archive.columnSetMask());
            point.tsMillis = archive.getLastTsMillis() + 10_000;
            archive.addRecord(point);
        }

        // at least one write success
        pushBatchSync(request(StockpileLocalId.random(), archive));

        try {
            IntStream.range(0, 100)
                .parallel()
                .mapToObj(ignore -> {
                    return stockpileShard.pushBatch(StockpileWriteRequest.newBuilder()
                            .addArchiveCopy(StockpileLocalId.random(), archive)
                            .build());
                })
                .collect(Collectors.collectingAndThen(Collectors.toList(), CompletableFutures::allOfVoid))
                .join();
            fail("Memory limit 1024, max overcommit 2048, but was write " + archive.memorySizeIncludingSelf() * 100);
        } catch (CompletionException e) {
            var cause = e.getCause();
            assertTrue(cause instanceof StockpileRuntimeException);
            var se = (StockpileRuntimeException) cause;
            assertEquals(EStockpileStatusCode.RESOURCE_EXHAUSTED, se.getStockpileStatusCode());
        }
        close(archive);
    }

    @Test
    public void lockShardForInit() {
        restart();

        long localId = StockpileLocalId.random();
        assertEquals(StockpileShard.LoadState.DONE, stockpileShard.getLoadState());

        KvLock lock = new KvLock(HostUtils.getFqdn(), 42, TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() + 30_000));
        kikimrKvClientInMem.write(tabletId, 0, KvLock.FILE, KvLock.serialize(lock), MsgbusKv.TKeyValueRequest.EStorageChannel.MAIN, MsgbusKv.TKeyValueRequest.EPriority.REALTIME, 0).join();

        Runnable ensureWriteFailed = () -> {
            var status = pushOnePoint(localId, MetricType.DGAUGE, randomPoint(MetricType.DGAUGE))
                .thenApply(e -> EStockpileStatusCode.OK)
                .exceptionally(e -> {
                    if (e instanceof CompletionException) {
                        e = e.getCause();
                    }

                    if (e instanceof StockpileRuntimeException) {
                        return ((StockpileRuntimeException) e).getStockpileStatusCode();
                    }

                    e.printStackTrace();
                    return EStockpileStatusCode.INTERNAL_ERROR;
                }).join();
            assertEquals(EStockpileStatusCode.SHARD_NOT_READY, status);
        };

        restartNoWait();
        while (stockpileShard.getLoadState() == StockpileShard.LoadState.INIT) {
            ensureWriteFailed.run();
        }

        assertEquals(StockpileShard.LoadState.LOCKED, stockpileShard.getLoadState());
        ensureWriteFailed.run();

        kikimrKvClientInMem.deleteFiles(tabletId, 0, List.of(KvLock.FILE), 0).join();
        stockpileShard.waitForInitializedOrAnyError();
        assertEquals(StockpileShard.LoadState.DONE, stockpileShard.getLoadState());

        var point = randomPoint(MetricType.DGAUGE);
        pushOnePoint(localId, MetricType.DGAUGE, point).join();

        var read = readPointsSync(localId);
        assertEquals(AggrGraphDataArrayList.of(point), read);
    }

    @Test
    public void unableToParseLockFile() {
        kikimrKvClientInMem.write(tabletId, 0, KvLock.FILE, "hi".getBytes(), MsgbusKv.TKeyValueRequest.EStorageChannel.MAIN, MsgbusKv.TKeyValueRequest.EPriority.REALTIME, 0).join();

        restart();
        assertEquals(StockpileShard.LoadState.DONE, stockpileShard.getLoadState());
    }

    @Test
    public void readRangeAttachRead() {
        restart();

        long localId = StockpileLocalId.random();
        long ts0 = System.currentTimeMillis();
        stockpileShard.setMaxCacheSize(100500);

        AggrGraphDataArrayList points = new AggrGraphDataArrayList();
        for (int index = 0; index < 10; index++) {
            var point = point(ts0 + (index * 10_000), index);
            points.addRecord(point);
            pushOnePoint(localId, MetricType.DGAUGE, point).join();
            stockpileShard.forceSnapshotSync();
        }

        // await already running read, because range enough
        kikimrKvClientInMem.pauseReads();
        var init = stockpileShard.readOne(StockpileMetricReadRequest.newBuilder()
            .setLocalId(localId)
            .setFromMillis(ts0 + 50_000)
            .build());

        var append = stockpileShard.readOne(StockpileMetricReadRequest.newBuilder()
            .setLocalId(localId)
            .setFromMillis(ts0 + 90_000)
            .build());

        kikimrKvClientInMem.resumeReads();

        assertEquals(AggrGraphDataArrayList.of(points.slice(5, 10)), AggrGraphDataArrayList.of(init.join().getTimeseries()));
        assertEquals(AggrGraphDataArrayList.of(points.slice(9, 10)), AggrGraphDataArrayList.of(append.join().getTimeseries()));
    }

    @Test
    public void readRangeRepeatDiskRead() {
        restart();

        long localId = StockpileLocalId.random();
        long ts0 = System.currentTimeMillis();
        stockpileShard.setMaxCacheSize(100500);

        AggrGraphDataArrayList points = new AggrGraphDataArrayList();
        for (int index = 0; index < 10; index++) {
            var point = point(ts0 + (index * 10_000), index);
            points.addRecord(point);
            pushOnePoint(localId, MetricType.DGAUGE, point).join();
            stockpileShard.forceSnapshotSync();
        }

        // read from disk twice, because latest read request have larger read range
        kikimrKvClientInMem.pauseReads();
        var init = stockpileShard.readOne(StockpileMetricReadRequest.newBuilder()
            .setLocalId(localId)
            .setFromMillis(ts0 + 90_000)
            .build());

        var append = stockpileShard.readOne(StockpileMetricReadRequest.newBuilder()
            .setLocalId(localId)
            .setFromMillis(ts0 + 50_000)
            .build());

        kikimrKvClientInMem.resumeReads();

        assertEquals(AggrGraphDataArrayList.of(points.slice(9, 10)), AggrGraphDataArrayList.of(init.join().getTimeseries()));
        assertEquals(AggrGraphDataArrayList.of(points.slice(5, 10)), AggrGraphDataArrayList.of(append.join().getTimeseries()));
    }

    @Test
    public void readRangeReuseCache() {
        restart();

        long localId = StockpileLocalId.random();
        long ts0 = System.currentTimeMillis();
        stockpileShard.setMaxCacheSize(100500);

        AggrGraphDataArrayList points = new AggrGraphDataArrayList();
        for (int index = 0; index < 10; index++) {
            var point = point(ts0 + (index * 10_000), index);
            points.addRecord(point);
            pushOnePoint(localId, MetricType.DGAUGE, point).join();
            stockpileShard.forceSnapshotSync();
        }

        // avoid read from disk, because cache already have enough data range
        var init = stockpileShard.readOne(StockpileMetricReadRequest.newBuilder()
            .setLocalId(localId)
            .setFromMillis(ts0 + 50_000)
            .build());

        assertEquals(AggrGraphDataArrayList.of(points.slice(5, 10)), AggrGraphDataArrayList.of(init.join().getTimeseries()));

        kikimrKvClientInMem.pauseReads();
        var fromCache = stockpileShard.readOne(StockpileMetricReadRequest.newBuilder()
            .setLocalId(localId)
            .setFromMillis(ts0 + 90_000)
            .build());

        assertEquals(AggrGraphDataArrayList.of(points.slice(9, 10)), AggrGraphDataArrayList.of(fromCache.join().getTimeseries()));
    }

    @Test
    public void readRangeRefreshCache() {
        restart();

        long localId = StockpileLocalId.random();
        long ts0 = System.currentTimeMillis();
        stockpileShard.setMaxCacheSize(100500);

        AggrGraphDataArrayList points = new AggrGraphDataArrayList();
        for (int index = 0; index < 10; index++) {
            var point = point(ts0 + (index * 10_000), index);
            points.addRecord(point);
            pushOnePoint(localId, MetricType.DGAUGE, point).join();
            stockpileShard.forceSnapshotSync();
        }

        // read from disk if not enough data into cache
        var init = stockpileShard.readOne(StockpileMetricReadRequest.newBuilder()
            .setLocalId(localId)
            .setFromMillis(ts0 + 90_000)
            .build());

        assertEquals(AggrGraphDataArrayList.of(points.slice(9, 10)), AggrGraphDataArrayList.of(init.join().getTimeseries()));

        var fromDisk = stockpileShard.readOne(StockpileMetricReadRequest.newBuilder()
            .setLocalId(localId)
            .setFromMillis(ts0 + 50_000)
            .build());

        assertEquals(AggrGraphDataArrayList.of(points.slice(5, 10)), AggrGraphDataArrayList.of(fromDisk.join().getTimeseries()));
        kikimrKvClientInMem.pauseReads();

        var fromCache = stockpileShard.readOne(StockpileMetricReadRequest.newBuilder()
            .setLocalId(localId)
            .setFromMillis(ts0 + 80_000)
            .build());

        assertEquals(AggrGraphDataArrayList.of(points.slice(8, 10)), AggrGraphDataArrayList.of(fromCache.join().getTimeseries()));
    }

    @Test
    public void multipleDailySnapshots() {
        var opts = stockpileShardGlobals.mergeStrategy.getDaily();
        assumeTrue(opts.isEnableNew() && opts.getSnapshotsLimit() != 1);

        restart();
        long localId = StockpileLocalId.random();

        AggrGraphDataArrayList expected = new AggrGraphDataArrayList();
        AggrPoint point = point(System.currentTimeMillis(), 0);
        while (stockpileShard.snapshotCount(SnapshotLevel.DAILY).orElse(0) <= 2) {
            point.tsMillis += 10_000;
            point.valueNum += 1;

            pushOnePoint(localId, MetricType.DGAUGE, point).join();
            stockpileShard.forceSnapshot().join();
            stockpileShard.forceSnapshotSync();
            stockpileShard.forceMergeSync(MergeKind.DAILY);

            expected.addRecord(point);
        }
        System.out.println("snapshot counts: " + stockpileShard.snapshotCount(SnapshotLevel.DAILY));

        {
            var read = readPointsSync(localId);
            assertEquals(expected, read);
        }

        {
            restart();
            var read = readPointsSync(localId);
            assertEquals(expected, read);
        }

        {
            restart();

            var read = stockpileShard.readOne(StockpileMetricReadRequest.newBuilder()
                .setLocalId(localId)
                .setFromMillis(point.tsMillis - 9000)
                .build())
                .join()
                .getTimeseries();

            assertEquals(AggrGraphDataArrayList.of(read), AggrGraphDataArrayList.of(point));
        }
    }

    @Test
    public void dailyMergeSplitWhole() {
        restart();
        long localId = StockpileLocalId.random();

        pushOnePoint(localId, MetricType.DGAUGE, point("2018-08-20T09:04:30Z", 1)).join();
        pushOnePoint(localId, MetricType.DGAUGE, point("2018-08-20T09:05:30Z", 2)).join();
        pushOnePoint(localId, MetricType.DGAUGE, point("2018-08-20T09:06:30Z", 3)).join();

        stockpileShard.forceSnapshot().join();
        assertEquals(1, stockpileShard.snapshotCount(SnapshotLevel.TWO_HOURS).orElse(0));

        Txn txn = stockpileShard.forceMergeSync(MergeKind.DAILY);
        assertEquals(System.currentTimeMillis(), stockpileShard.latestSnapshotTime(SnapshotLevel.DAILY).getTsOrSpecial(), 1_000);
        assertEquals(SnapshotTs.SpecialTs.NEVER.value, stockpileShard.latestSnapshotTime(SnapshotLevel.ETERNITY).getTsOrSpecial());

        assertEquals(0, stockpileShard.snapshotCount(SnapshotLevel.TWO_HOURS).orElse(0));
        assertEquals(0, stockpileShard.snapshotCount(SnapshotLevel.DAILY).orElse(0));
        assertEquals(1, stockpileShard.snapshotCount(SnapshotLevel.ETERNITY).orElse(0));

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
            point("2018-08-20T09:04:30Z", 1),
            point("2018-08-20T09:05:30Z", 2),
            point("2018-08-20T09:06:30Z", 3)
        );

        {
            var read = readPointsSync(localId);
            assertEquals(expected, read);
        }

        {
            restart();

            var read = readPointsSync(localId);
            assertEquals(expected, read);
        }

        {
            SnapshotIndex index = readSnapshotIndex(txn.getTxn(), SnapshotLevel.ETERNITY);
            long lastTsMillis = index.getContent().findMetricLastTsMillis(localId);
            assertEquals(timeMillis("2018-08-20T09:06:30Z"), lastTsMillis);
        }
    }

    @Test
    public void dailyMergeSplitPart() {
        long now = System.currentTimeMillis();

        restart();
        long localId = StockpileLocalId.random();

        pushOnePoint(localId, MetricType.DGAUGE, point("2018-08-20T09:04:30Z", 1)).join();
        pushOnePoint(localId, MetricType.DGAUGE, point("2018-08-20T09:05:30Z", 2)).join();
        pushOnePoint(localId, MetricType.DGAUGE, point("2018-08-20T09:06:30Z", 3)).join();
        pushOnePoint(localId, MetricType.DGAUGE, point(now, 4)).join();

        stockpileShard.forceSnapshot().join();
        assertEquals(1, stockpileShard.snapshotCount(SnapshotLevel.TWO_HOURS).orElse(0));

        Txn txn = stockpileShard.forceMergeSync(MergeKind.DAILY);
        assertEquals(System.currentTimeMillis(), stockpileShard.latestSnapshotTime(SnapshotLevel.DAILY).getTsOrSpecial(), 1_000);
        assertEquals(SnapshotTs.SpecialTs.NEVER.value, stockpileShard.latestSnapshotTime(SnapshotLevel.ETERNITY).getTsOrSpecial());

        assertEquals(0, stockpileShard.snapshotCount(SnapshotLevel.TWO_HOURS).orElse(0));
        assertEquals(1, stockpileShard.snapshotCount(SnapshotLevel.DAILY).orElse(1));
        assertEquals(1, stockpileShard.snapshotCount(SnapshotLevel.ETERNITY).orElse(1));

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
            point("2018-08-20T09:04:30Z", 1),
            point("2018-08-20T09:05:30Z", 2),
            point("2018-08-20T09:06:30Z", 3),
            point(now, 4)
        );

        {
            var read = readPointsSync(localId);
            assertEquals(expected, read);
        }

        {
            restart();

            var read = readPointsSync(localId);
            assertEquals(expected, read);
        }

        {
            SnapshotIndex index = readSnapshotIndex(txn.getTxn(), SnapshotLevel.DAILY);
            long lastTsMillis = index.getContent().findMetricLastTsMillis(localId);
            assertEquals(now, lastTsMillis);
        }

        {
            SnapshotIndex index = readSnapshotIndex(txn.getTxn(), SnapshotLevel.ETERNITY);
            long lastTsMillis = index.getContent().findMetricLastTsMillis(localId);
            assertEquals(timeMillis("2018-08-20T09:06:30Z"), lastTsMillis);
        }
    }

    @Test
    public void writeOnlyHeaders() {
        restart();
        var localId = StockpileLocalId.random();

        MetricArchiveMutable noHeaders = new MetricArchiveMutable();
        noHeaders.setType(MetricType.DGAUGE);
        noHeaders.addRecord(point("2018-08-20T09:04:30Z", 1));
        noHeaders.addRecord(point(clock.millis(), 2));

        pushBatchSync(request(localId, noHeaders));

        stockpileShard.forceLogSnapshot().join();
        stockpileShard.forceSnapshot().join();
        stockpileShard.forceMergeSync(MergeKind.DAILY);
        clock.passedTime(5, TimeUnit.DAYS);
        stockpileShard.forceMergeSync(MergeKind.DAILY);
        stockpileShard.forceMergeSync(MergeKind.ETERNITY);

        var headers = new MetricHeader(DeleteBeforeField.KEEP, 42, EProjectId.SOLOMON_VALUE, (short) 2, MetricType.METRIC_TYPE_UNSPECIFIED);
        var expected = headers.withKind(MetricType.DGAUGE);

        pushBatchSync(request(localId, new MetricArchiveMutable(headers)));

        assertEquals(expected, readHeader(localId).join());
        stockpileShard.forceLogSnapshot().join();
        assertEquals(expected, readHeader(localId).join());
        stockpileShard.forceSnapshot().join();
        assertEquals(expected, readHeader(localId).join());
        stockpileShard.forceMergeSync(MergeKind.DAILY);
        assertEquals(expected, readHeader(localId).join());
        clock.passedTime(31, TimeUnit.DAYS);
        stockpileShard.forceMergeSync(MergeKind.ETERNITY);
        assertEquals(expected, readHeader(localId).join());
        close(noHeaders);
    }

    @Test
    public void readHugeRangeAttacheToLaterRead() {
        restart();

        int mask = TsColumn.mask | ValueColumn.mask;
        var one = randomPoint(mask);
        var two = randomPoint(mask);
        two.tsMillis = one.tsMillis + 10_000;


        long localId = StockpileLocalId.random();
        pushDeleteBefore(localId, one.tsMillis - 100).join();
        stockpileShard.forceSnapshot().join();
        stockpileShard.forceLogSnapshot().join();

        pushOnePoint(localId, MetricType.DGAUGE, one).join();
        stockpileShard.forceLogSnapshot().join();
        stockpileShard.forceSnapshot().join();

        pushOnePoint(localId, MetricType.DGAUGE, two).join();
        stockpileShard.forceLogSnapshot().join();
        stockpileShard.forceSnapshot().join();

        kikimrKvClientInMem.pauseReads();

        long fromTsMillis = -3740902812961L;

        var request = StockpileMetricReadRequest.newBuilder().setLocalId(localId).setFromMillis(fromTsMillis).build();

        var readOne = readPoints(request);
        // should be attached
        var readTwo = readPoints(request);
        var readTree = readPoints(request);

        kikimrKvClientInMem.resumeReads();

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(one, two);
        expected.sortAndMerge();

        assertEquals(expected, readOne.join());
        assertEquals(expected, readTwo.join());
        assertEquals(expected, readTree.join());
    }

    @Test
    public void allocateUniqueLocalIds() {
        restart();
        var results = IntStream.range(0, 1000)
            .parallel()
            .mapToObj(ignore -> stockpileShard.allocateLocalIds(100, 0))
            .collect(Collectors.collectingAndThen(Collectors.toList(), CompletableFutures::allOf))
            .join();

        var unique = new LongOpenHashSet();
        for (var result : results) {
            for (long localId : result) {
                assertTrue(unique.add(localId));
            }
        }
    }

    @Test
    public void idempotentWriteIgnoreRepeat() {
        restart();

        long ts0 = System.currentTimeMillis();
        long localId = StockpileLocalId.random();

        MetricArchiveMutable archive = new MetricArchiveMutable();
        archive.setType(MetricType.DGAUGE);
        archive.setOwnerShardId(42);
        archive.addRecord(AggrPoint.builder()
                .time(ts0)
                .doubleValue(42)
                .merge(true)
                .count(1)
                .stepMillis(10_000)
                .build());

        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (int index = 0; index < 10; index++) {
            futures.add(stockpileShard.pushBatch(StockpileWriteRequest.newBuilder()
                    .addArchiveCopy(localId, archive)
                    .setProducerId(1)
                    .setProducerSeqNo(42L)
                    .build()));
        }
        CompletableFutures.allOfVoid(futures).join();
        var result = read(localId).join();
        assertEquals(archive.toImmutable(), result);
    }

    @Test
    public void idempotentWriteIgnoreRepeatAfterWrite() {
        restart();

        long ts0 = System.currentTimeMillis();
        long localId = StockpileLocalId.random();

        MetricArchiveMutable archive = new MetricArchiveMutable();
        archive.setType(MetricType.DGAUGE);
        archive.setOwnerShardId(42);
        archive.addRecord(AggrPoint.builder()
                .time(ts0)
                .doubleValue(42)
                .merge(true)
                .count(1)
                .stepMillis(10_000)
                .build());

        for (int index = 0; index < 10; index++) {
            stockpileShard.pushBatch(StockpileWriteRequest.newBuilder()
                    .addArchiveCopy(localId, archive)
                    .setProducerId(1)
                    .setProducerSeqNo(42L)
                    .build())
                    .join();
        }
        var result = read(localId).join();
        assertEquals(archive.toImmutable(), result);
    }

    @Test
    public void idempotentWriteIgnoreObsolete() {
        restart();

        long ts0 = System.currentTimeMillis();
        long localId = StockpileLocalId.random();

        MetricArchiveMutable archive = new MetricArchiveMutable();
        archive.setType(MetricType.DGAUGE);
        archive.setOwnerShardId(42);
        archive.addRecord(AggrPoint.builder()
                .time(ts0)
                .doubleValue(42)
                .merge(true)
                .count(1)
                .stepMillis(10_000)
                .build());

        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (int index = 0; index < 10; index++) {
            futures.add(stockpileShard.pushBatch(StockpileWriteRequest.newBuilder()
                    .addArchiveCopy(localId, archive)
                    .setProducerId(1)
                    .setProducerSeqNo(10 - index)
                    .build()));
        }
        CompletableFutures.allOfVoid(futures).join();
        var result = read(localId).join();
        assertEquals(archive.toImmutable(), result);
    }

    @Test
    public void idempotentWriteIgnoreObsoleteWhenNewAlreadyWriten() {
        restart();

        long ts0 = System.currentTimeMillis();
        long localId = StockpileLocalId.random();

        MetricArchiveMutable archive = new MetricArchiveMutable();
        archive.setType(MetricType.DGAUGE);
        archive.setOwnerShardId(42);
        archive.addRecord(AggrPoint.builder()
                .time(ts0)
                .doubleValue(42)
                .merge(true)
                .count(1)
                .stepMillis(10_000)
                .build());

        for (int index = 0; index < 10; index++) {
            stockpileShard.pushBatch(StockpileWriteRequest.newBuilder()
                    .addArchiveCopy(localId, archive)
                    .setProducerId(1)
                    .setProducerSeqNo(10 - index)
                    .build())
                    .join();
        }
        var result = read(localId).join();
        assertEquals(archive.toImmutable(), result);
    }

    @Test
    public void idempotentWriteSequentialWrite() {
        restart();

        long ts0 = System.currentTimeMillis();
        long localId = StockpileLocalId.random();
        AggrGraphDataArrayList expected = new AggrGraphDataArrayList();

        var point = new AggrPoint();
        point.setTsMillis(ts0);
        point.setMerge(true);
        point.setCount(1);
        point.setValue(42);
        point.setStepMillis(10_000);
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (int index = 1; index < 10; index++) {
            expected.addRecord(point);

            var archive = new MetricArchiveMutable();
            archive.setType(MetricType.DGAUGE);
            archive.setOwnerShardId(11);
            archive.addRecord(point);
            futures.add(stockpileShard.pushBatch(StockpileWriteRequest.newBuilder()
                    .addArchive(localId, archive)
                    .setProducerId(152)
                    .setProducerSeqNo(index)
                    .build()));
            point.tsMillis += point.stepMillis;
        }
        CompletableFutures.allOfVoid(futures).join();

        var result = readPointsSync(localId);
        assertEquals(expected, result);
    }

    @Test
    public void idempotentWriteSequentialWriteSync() {
        restart();

        long ts0 = System.currentTimeMillis();
        long localId = StockpileLocalId.random();
        AggrGraphDataArrayList expected = new AggrGraphDataArrayList();

        var point = new AggrPoint();
        point.setTsMillis(ts0);
        point.setMerge(true);
        point.setCount(1);
        point.setValue(42);
        point.setStepMillis(10_000);
        for (int index = 1; index < 10; index++) {
            expected.addRecord(point);

            var archive = new MetricArchiveMutable();
            archive.setType(MetricType.DGAUGE);
            archive.setOwnerShardId(11);
            archive.addRecord(point);
            stockpileShard.pushBatch(StockpileWriteRequest.newBuilder()
                    .addArchive(localId, archive)
                    .setProducerId(152)
                    .setProducerSeqNo(index)
                    .build())
                    .join();
            point.tsMillis += point.stepMillis;
        }

        var result = readPointsSync(localId);
        assertEquals(expected, result);
    }

    @Test
    public void idempotentWriteIgnoreObsoleteAfterTwoHoursSnapshot() {
        restart();

        long ts0 = System.currentTimeMillis();
        long localId = StockpileLocalId.random();

        MetricArchiveMutable archive = new MetricArchiveMutable();
        archive.setType(MetricType.DGAUGE);
        archive.setOwnerShardId(42);
        archive.addRecord(AggrPoint.builder()
                .time(ts0)
                .doubleValue(42)
                .merge(true)
                .count(1)
                .stepMillis(10_000)
                .build());
        stockpileShard.pushBatch(StockpileWriteRequest.newBuilder()
                .setProducerSeqNo(42L)
                .setProducerId(1)
                .addArchiveCopy(localId, archive)
                .build())
                .join();
        stockpileShard.forceSnapshot().join();

        for (int index = 1; index < 11; index++) {
            stockpileShard.pushBatch(StockpileWriteRequest.newBuilder()
                    .addArchiveCopy(localId, archive)
                    .setProducerId(1)
                    .setProducerSeqNo(index)
                    .build())
                    .join();
        }
        var result = read(localId).join();
        assertEquals(archive.toImmutable(), result);
    }

    @Test
    public void idempotentWritePersistWithLog() {
        assumeThat(StockpileFormat.CURRENT.getFormat(), greaterThanOrEqualTo(StockpileFormat.IDEMPOTENT_WRITE_38.getFormat()));
        restart();

        long ts0 = System.currentTimeMillis();
        long localId = StockpileLocalId.random();

        MetricArchiveMutable archive = new MetricArchiveMutable();
        archive.setType(MetricType.DGAUGE);
        archive.setOwnerShardId(42);
        archive.addRecord(AggrPoint.builder()
                .time(ts0)
                .doubleValue(42)
                .merge(true)
                .count(1)
                .stepMillis(10_000)
                .build());
        stockpileShard.pushBatch(StockpileWriteRequest.newBuilder()
                .setProducerSeqNo(42L)
                .setProducerId(1)
                .addArchiveCopy(localId, archive)
                .build())
                .join();
        restart();

        for (int index = 1; index < 11; index++) {
            stockpileShard.pushBatch(StockpileWriteRequest.newBuilder()
                    .addArchiveCopy(localId, archive)
                    .setProducerId(1)
                    .setProducerSeqNo(index)
                    .build())
                    .join();
        }
        var result = read(localId).join();
        assertEquals(archive.toImmutable(), result);
    }

    @Test
    public void idempotentWritePersistWithLogSnapshot() {
        assumeThat(StockpileFormat.CURRENT.getFormat(), greaterThanOrEqualTo(StockpileFormat.IDEMPOTENT_WRITE_38.getFormat()));
        restart();

        long ts0 = System.currentTimeMillis();
        long localId = StockpileLocalId.random();

        MetricArchiveMutable archive = new MetricArchiveMutable();
        archive.setType(MetricType.DGAUGE);
        archive.setOwnerShardId(42);
        archive.addRecord(AggrPoint.builder()
                .time(ts0)
                .doubleValue(42)
                .merge(true)
                .count(1)
                .stepMillis(10_000)
                .build());
        stockpileShard.pushBatch(StockpileWriteRequest.newBuilder()
                .setProducerSeqNo(42L)
                .setProducerId(1)
                .addArchiveCopy(localId, archive)
                .build())
                .join();
        pushOnePoint(StockpileLocalId.random(), MetricType.DGAUGE, randomPoint(MetricType.DGAUGE)).join();
        stockpileShard.forceLogSnapshot().join();
        restart();

        for (int index = 1; index < 11; index++) {
            stockpileShard.pushBatch(StockpileWriteRequest.newBuilder()
                    .addArchiveCopy(localId, archive)
                    .setProducerId(1)
                    .setProducerSeqNo(index)
                    .build())
                    .join();
        }
        var result = read(localId).join();
        assertEquals(archive.toImmutable(), result);
    }

    @Test
    public void idempotentWritePersistWithTwoHoursSnapshot() {
        assumeThat(StockpileFormat.CURRENT.getFormat(), greaterThanOrEqualTo(StockpileFormat.IDEMPOTENT_WRITE_38.getFormat()));
        restart();

        long ts0 = System.currentTimeMillis();
        long localId = StockpileLocalId.random();

        MetricArchiveMutable archive = new MetricArchiveMutable();
        archive.setType(MetricType.DGAUGE);
        archive.setOwnerShardId(42);
        archive.addRecord(AggrPoint.builder()
                .time(ts0)
                .doubleValue(42)
                .merge(true)
                .count(1)
                .stepMillis(10_000)
                .build());
        stockpileShard.pushBatch(StockpileWriteRequest.newBuilder()
                .setProducerSeqNo(42L)
                .setProducerId(1)
                .addArchiveCopy(localId, archive)
                .build())
                .join();
        pushOnePoint(StockpileLocalId.random(), MetricType.DGAUGE, randomPoint(MetricType.DGAUGE)).join();
        stockpileShard.forceSnapshot().join();
        restart();

        for (int index = 1; index < 11; index++) {
            stockpileShard.pushBatch(StockpileWriteRequest.newBuilder()
                    .addArchiveCopy(localId, archive)
                    .setProducerId(1)
                    .setProducerSeqNo(index)
                    .build())
                    .join();
        }
        var result = read(localId).join();
        assertEquals(archive.toImmutable(), result);
    }

    @Test
    public void dailyMergeBigCount() {
        restart();

        long ts0 = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30);
        long localId = StockpileLocalId.random();

        var archive = new MetricArchiveMutable();
        archive.setType(MetricType.DGAUGE);
        archive.setDecimPolicyId(EDecimPolicy.POLICY_5_MIN_AFTER_7_DAYS.getNumber());
        archive.addRecord(point(ts0, 42, Long.MAX_VALUE));

        pushBatchSync(StockpileWriteRequest.newBuilder()
                .addArchive(localId, archive)
                .build());
        stockpileShard.forceLogSnapshot().join();
        stockpileShard.forceSnapshot().join();
        stockpileShard.forceMerge(MergeKind.DAILY).join();
        stockpileShard.forceMerge(MergeKind.ETERNITY).join();

        var expected = new MetricArchiveMutable(archive.header());

        expected.addRecord(point(InstantUtils.truncate(ts0, TimeUnit.MINUTES.toMillis(5)), 42, Long.MAX_VALUE));

        var result = read(localId).join();
        assertEquals(expected.toImmutableNoCopy(), result);
    }

    @Test
    public void deleteShardByCommandNotExistShard() {
        assumeTrue("Unsupported format", StockpileFormat.CURRENT.ge(StockpileFormat.DELETED_SHARDS_39));
        restart();

        pushBatchSync(StockpileWriteRequest.newBuilder()
                .addDeleteShard(EProjectId.SOLOMON_VALUE, 42)
                .build());
        stockpileShard.forceLogSnapshot().join();
        stockpileShard.forceSnapshot().join();
        stockpileShard.forceMerge(MergeKind.DAILY).join();
        stockpileShard.forceMerge(MergeKind.ETERNITY).join();
    }

    @Test
    public void deleteShardByCommandSameSnapshot() {
        assumeTrue("Unsupported format", StockpileFormat.CURRENT.ge(StockpileFormat.DELETED_SHARDS_39));
        restart();

        long ts0 = System.currentTimeMillis();
        long localId = StockpileLocalId.random();

        var archive = new MetricArchiveMutable();
        archive.setType(MetricType.DGAUGE);
        archive.setDecimPolicyId(EDecimPolicy.POLICY_5_MIN_AFTER_7_DAYS.getNumber());
        archive.setOwnerProjectId(1);
        archive.setOwnerShardId(55);
        archive.addRecord(point(ts0, 42));

        pushBatchSync(StockpileWriteRequest.newBuilder()
                .addArchive(localId, archive)
                .build());

        pushBatchSync(StockpileWriteRequest.newBuilder()
                .addDeleteShard(archive.getOwnerProjectId(), archive.getOwnerShardId())
                .build());

        stockpileShard.forceSnapshot().join();
        stockpileShard.forceMerge(MergeKind.DAILY).join();

        var result = read(localId).join();
        assertEquals(MetricArchiveImmutable.empty, result);
    }

    @Test
    public void deleteShardByCommandDailySnapshot() {
        assumeTrue("Unsupported format", StockpileFormat.CURRENT.ge(StockpileFormat.DELETED_SHARDS_39));
        restart();

        long ts0 = System.currentTimeMillis();
        long localId = StockpileLocalId.random();

        var archive = new MetricArchiveMutable();
        archive.setType(MetricType.DGAUGE);
        archive.setDecimPolicyId(EDecimPolicy.POLICY_5_MIN_AFTER_7_DAYS.getNumber());
        archive.setOwnerProjectId(1);
        archive.setOwnerShardId(55);
        archive.addRecord(point(ts0, 42));

        pushBatchSync(StockpileWriteRequest.newBuilder()
                .addArchive(localId, archive)
                .build());

        stockpileShard.forceSnapshot().join();
        stockpileShard.forceMerge(MergeKind.DAILY).join();

        clock.passedTime(7, TimeUnit.DAYS);
        pushBatchSync(StockpileWriteRequest.newBuilder()
                .addDeleteShard(archive.getOwnerProjectId(), archive.getOwnerShardId())
                .build());

        stockpileShard.forceSnapshot().join();
        stockpileShard.forceMerge(MergeKind.DAILY).join();

        var result = read(localId).join();
        assertEquals(MetricArchiveImmutable.empty, result);
    }

    @Test
    public void deleteShardByCommandEternitySnapshot() {
        assumeTrue("Unsupported format", StockpileFormat.CURRENT.ge(StockpileFormat.DELETED_SHARDS_39));
        restart();

        long ts0 = timeMillis("2020-08-20T09:06:30Z");
        long localId = StockpileLocalId.random();

        var archive = new MetricArchiveMutable();
        archive.setType(MetricType.DGAUGE);
        archive.setDecimPolicyId(EDecimPolicy.POLICY_5_MIN_AFTER_7_DAYS.getNumber());
        archive.setOwnerProjectId(1);
        archive.setOwnerShardId(56);
        archive.addRecord(point(ts0, 42));
        archive.addRecord(point(ts0 + 10_000, 43));
        archive.addRecord(point(ts0 + 20_000, 44));

        pushBatchSync(StockpileWriteRequest.newBuilder()
                .addArchive(localId, archive)
                .build());

        stockpileShard.forceSnapshot().join();
        stockpileShard.forceMerge(MergeKind.DAILY).join();
        stockpileShard.forceMerge(MergeKind.ETERNITY).join();

        clock.passedTime(7, TimeUnit.DAYS);
        pushBatchSync(StockpileWriteRequest.newBuilder()
                .addDeleteShard(archive.getOwnerProjectId(), archive.getOwnerShardId())
                .build());

        stockpileShard.forceSnapshot().join();
        stockpileShard.forceMerge(MergeKind.DAILY).join();
        clock.passedTime(30, TimeUnit.DAYS);
        stockpileShard.forceMerge(MergeKind.ETERNITY).join();

        var result = read(localId).join();
        assertEquals(MetricArchiveImmutable.empty, result);
    }

    @Test
    public void deleteShardByCommandPersistent() {
        assumeTrue("Unsupported format", StockpileFormat.CURRENT.ge(StockpileFormat.DELETED_SHARDS_39));
        restart();

        long ts0 = timeMillis("2020-08-20T09:06:30Z");
        long localId = StockpileLocalId.random();

        var archive = new MetricArchiveMutable();
        archive.setType(MetricType.DGAUGE);
        archive.setDecimPolicyId(EDecimPolicy.POLICY_5_MIN_AFTER_7_DAYS.getNumber());
        archive.setOwnerProjectId(1);
        archive.setOwnerShardId(56);
        archive.addRecord(point(ts0, 42));
        archive.addRecord(point(ts0 + 10_000, 43));
        archive.addRecord(point(ts0 + 20_000, 44));

        pushBatchSync(StockpileWriteRequest.newBuilder()
                .addArchive(localId, archive)
                .build());

        stockpileShard.forceSnapshot().join();
        stockpileShard.forceMerge(MergeKind.DAILY).join();
        stockpileShard.forceMerge(MergeKind.ETERNITY).join();

        clock.passedTime(31, TimeUnit.DAYS);

        pushBatchSync(StockpileWriteRequest.newBuilder()
                .addDeleteShard(archive.getOwnerProjectId(), archive.getOwnerShardId())
                .build());

        restart();
        stockpileShard.forceLogSnapshot().join();
        restart();
        stockpileShard.forceSnapshot().join();
        restart();
        stockpileShard.forceMerge(MergeKind.DAILY).join();
        restart();
        stockpileShard.forceMerge(MergeKind.ETERNITY).join();

        var result = read(localId).join();
        assertEquals(MetricArchiveImmutable.empty, result);
    }

    @Test
    public void evictFromDailyNotWrittenAnymore() {
        restart();

        long ts0 = timeMillis("2020-08-20T09:06:30Z");
        var archive = new MetricArchiveMutable();
        archive.setType(MetricType.DGAUGE);
        archive.setDecimPolicyId(EDecimPolicy.POLICY_5_MIN_AFTER_7_DAYS.getNumber());
        archive.addRecord(point(ts0, 42));

        pushBatchSync(StockpileWriteRequest.newBuilder()
                .addArchive(StockpileLocalId.random(), archive)
                .build());
        stockpileShard.forceSnapshot().join();
        stockpileShard.forceMerge(MergeKind.DAILY).join();
        assertEquals(0, metricsByLevel(SnapshotLevel.DAILY));
        assertEquals(1, metricsByLevel(SnapshotLevel.ETERNITY));

        pushBatchSync(StockpileWriteRequest.newBuilder()
                .addArchive(StockpileLocalId.random(), archive)
                .build());
        stockpileShard.forceSnapshot().join();
        stockpileShard.forceMerge(MergeKind.DAILY).join();
        assertEquals(0, metricsByLevel(SnapshotLevel.DAILY));
        assertEquals(2, metricsByLevel(SnapshotLevel.ETERNITY));
    }

    @Test
    public void evictFromDailyDeleted() {
        restart();

        long ts0 = timeMillis("2020-08-20T09:06:30Z");
        long localId = StockpileLocalId.random();
        var archive = new MetricArchiveMutable();
        archive.setType(MetricType.DGAUGE);
        archive.setDecimPolicyId(EDecimPolicy.POLICY_5_MIN_AFTER_7_DAYS.getNumber());
        archive.addRecord(point(ts0, 42));

        pushBatchSync(StockpileWriteRequest.newBuilder()
                .addArchive(StockpileLocalId.random(), archive)
                .build());
        stockpileShard.forceSnapshot().join();
        stockpileShard.forceMerge(MergeKind.DAILY).join();
        assertEquals(0, metricsByLevel(SnapshotLevel.DAILY));
        assertEquals(1, metricsByLevel(SnapshotLevel.ETERNITY));

        archive = new MetricArchiveMutable();
        archive.setDeleteBefore(DeleteBeforeField.DELETE_ALL);
        pushBatchSync(StockpileWriteRequest.newBuilder()
                .addArchive(localId, archive)
                .build());
        stockpileShard.forceSnapshot().join();
        stockpileShard.forceMerge(MergeKind.DAILY).join();
        assertEquals(0, metricsByLevel(SnapshotLevel.DAILY));
        assertEquals(2, metricsByLevel(SnapshotLevel.ETERNITY));
    }

    @Test
    public void snapshotWithOnlyCommands() {
        stockpileShardGlobals.mergeStrategy.getEternity().setSnapshotsLimit(1);
        stockpileShardGlobals.mergeStrategy.getDaily().setSnapshotsLimit(1);
        restart();

        long ts0 = timeMillis("2020-08-20T09:06:30Z");
        long localId = StockpileLocalId.random();
        var archive = new MetricArchiveMutable();
        archive.setOwnerProjectId(1);
        archive.setOwnerShardId(42);
        archive.setType(MetricType.DGAUGE);
        archive.setDecimPolicyId(EDecimPolicy.POLICY_5_MIN_AFTER_7_DAYS.getNumber());
        archive.addRecord(point(ts0, 42));

        pushBatchSync(StockpileWriteRequest.newBuilder()
                .addArchive(localId, archive)
                .build());
        stockpileShard.forceSnapshot().join();
        stockpileShard.forceMerge(MergeKind.DAILY).join();
        assertEquals(0, metricsByLevel(SnapshotLevel.DAILY));
        assertEquals(1, metricsByLevel(SnapshotLevel.ETERNITY));

        pushBatchSync(StockpileWriteRequest.newBuilder()
                .addDeleteShard(1, 42)
                .build());

        stockpileShard.forceSnapshot().join();
        stockpileShard.forceMerge(MergeKind.DAILY).join();

        stockpileShard.forceMerge(MergeKind.ETERNITY).join();
        assertEquals(0, readPointsSync(localId).length());
        assertEquals(0, metricsByLevel(SnapshotLevel.DAILY));
        assertEquals(0, metricsByLevel(SnapshotLevel.ETERNITY));

        pushBatchSync(StockpileWriteRequest.newBuilder()
                .addArchive(localId, archive)
                .build());
        stockpileShard.forceSnapshot().join();
        stockpileShard.forceMerge(MergeKind.DAILY).join();
        assertEquals(0, metricsByLevel(SnapshotLevel.DAILY));
        assertEquals(1, metricsByLevel(SnapshotLevel.ETERNITY));
    }

    @Test
    public void twoHoursSnapshotOnEmptyLogState() {
        restart();
        var never = stockpileShard.latestSnapshotTime(SnapshotLevel.TWO_HOURS);
        assertEquals(SpecialTs.NEVER.value, never.getTsOrSpecial());

        stockpileShard.forceSnapshot().join();
        var latestSnapshot = stockpileShard.latestSnapshotTime(SnapshotLevel.TWO_HOURS);
        assertEquals(System.currentTimeMillis(), latestSnapshot.getTsOrSpecial(), 30_000);
    }

    private long metricsByLevel(SnapshotLevel level) {
        return stockpileShard.indexStats().getStatsByLevel(level).getTotalByProjects().getTotalByKinds().metrics;
    }

    private String billingStatistics() {
        var supplier = new CompositeMetricSupplier(List.of(stockpileShardGlobals.usage.stats, stockpileShard.indexStats()));

        StringWriter writer = new StringWriter();
        try (MetricTextEncoder e = new MetricTextEncoder(writer, true)) {
            supplier.supply(0, e);
        }
        String result = writer.toString();
        System.out.println(result);
        return result;
    }

    private static void assertPoints(AggrGraphDataArrayList expected, AggrGraphDataArrayList actual) {
        assertEquals(expected.length(), actual.length());
        int index = 0;
        for(; index < expected.length(); index++) {
            var e = expected.getAnyPoint(index);
            var a = actual.getAnyPoint(index);
            assertEquals("index: " + index, e, a);
        }
    }

    public void ensureMemLogsSizeSameAsDiskSize() {
        SizeAndCount disk = SizeAndCount.zero;
        SizeAndCount mem = SizeAndCount.zero;

        for (int index = 0; index < 3; index++) {
            disk = kikimrKvClientInMem.readRangeNames(stockpileShard.kvTabletId, 0, StockpileKvNames.logRange(), 0)
                .join()
                .stream()
                .map(file -> new SizeAndCount(file.getSize(), 1))
                .reduce(SizeAndCount.zero, SizeAndCount::plus);

            mem = stockpileShard.diskStats().get(FileKind.LOG);
            if (disk.size() == mem.size()) {
                break;
            }

            // trigger act to await complete previous operation like after complete write update files
            readPointsSync(StockpileLocalId.random());
        }

        assertEquals(disk.count(), mem.count());
        assertEquals(disk.size(), mem.size());
    }

    private CompletableFuture<Txn> pushOnePoint(long localId, MetricType type, AggrPoint point) {
        var builder = StockpileWriteRequest.newBuilder();
        var archive = builder.archiveRef(localId);
        archive.setType(type);
        archive.addRecord(point);
        var req = builder.build();
        stockpileShard.pushBatch(req);
        return req.getFuture();
    }

    private CompletableFuture<Txn> pushDeleteBefore(long localId, long beforeMillis) {
        var builder = StockpileWriteRequest.newBuilder();
        builder.archiveRef(localId).setDeleteBefore(beforeMillis);
        return stockpileShard.pushBatch(builder.build());
    }

    private static long timeMillis(String time) {
        return Instant.parse(time).toEpochMilli();
    }

    private static Histogram histogram(double[] bounds, long[] buckets) {
        return Histogram.newInstance(bounds, buckets);
    }

    public AggrGraphDataArrayList readPointsSync(long localId) {
        return CompletableFutures.join(readPoints(localId));
    }

    public CompletableFuture<AggrGraphDataArrayList> readPoints(long localId) {
        ThreadLocalTimeout.Handle handle = ThreadLocalTimeout.pushTimeout(1, TimeUnit.MINUTES);
        try {
            return read(localId)
                .thenApply(archive -> {
                    try (archive) {
                        return archive.toAggrGraphDataArrayList();
                    }
                });
        } finally {
            handle.popSafely();
        }
    }

    public CompletableFuture<AggrGraphDataArrayList> readPoints(StockpileMetricReadRequest request) {
        return stockpileShard.readOne(request)
            .thenApply(response -> AggrGraphDataArrayList.of(response.getTimeseries()));
    }

    public CompletableFuture<MetricArchiveImmutable> read(long localId) {
        return readOne(StockpileMetricReadRequest.newBuilder().setLocalId(localId).build());
    }

    public CompletableFuture<MetricHeader> readHeader(long localId) {
        return stockpileShard.readOne(StockpileMetricReadRequest.newBuilder().setLocalId(localId).build())
            .thenApply(StockpileMetricReadResponse::getHeader);
    }

    public CompletableFuture<MetricArchiveImmutable> readOne(StockpileMetricReadRequest request) {
        return stockpileShard.readOne(request)
                .thenApply(response -> {
                    try (var archive = new MetricArchiveMutable(response.getHeader())) {
                        archive.addAll(response.getTimeseries());
                        return archive.toImmutableNoCopy();
                    }
                });
    }

    private AggrGraphDataArrayList listOf(AggrPoint... points) {
        return AggrGraphDataArrayList.of(points);
    }
}
