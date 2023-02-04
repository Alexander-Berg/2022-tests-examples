package ru.yandex.stockpile.server.shard.test;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import io.netty.util.ResourceLeakDetector;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContextManager;

import ru.yandex.kikimr.client.kv.inMem.KikimrKvClientInMem;
import ru.yandex.misc.concurrent.CompletableFutures;
import ru.yandex.solomon.codec.archive.MetricArchiveMutable;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.column.StockpileColumn;
import ru.yandex.solomon.model.protobuf.MetricType;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.solomon.model.timeseries.MetricTypeTransfers;
import ru.yandex.stockpile.client.shard.StockpileLocalId;
import ru.yandex.stockpile.client.shard.StockpileShardId;
import ru.yandex.stockpile.server.shard.StockpileMetricReadRequest;
import ru.yandex.stockpile.server.shard.StockpileMetricReadResponse;
import ru.yandex.stockpile.server.shard.StockpileShard;
import ru.yandex.stockpile.server.shard.StockpileShardGlobals;
import ru.yandex.stockpile.server.shard.StockpileWriteRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static ru.yandex.solomon.model.point.AggrPointDataTestSupport.randomMask;
import static ru.yandex.solomon.model.point.AggrPointDataTestSupport.randomPoint;
import static ru.yandex.solomon.util.CloseableUtils.close;

/**
 * @author Vladimir Gordiychuk
 */
@RunWith(Parameterized.class)
@ContextConfiguration(classes = {
        StockpileShardTestContext.class
})
public class StockpileShardMetricTypeTest {
    private static TestContextManager testContextManager;

    @Rule
    public TestName testName = new TestName();
    @Rule
    public Timeout globalTimeout = Timeout.builder()
            .withLookingForStuckThread(true)
            .withTimeout(1, TimeUnit.MINUTES)
            .build();

    @Autowired
    private KikimrKvClientInMem kikimrKvClientInMem;
    @Autowired
    private StockpileShardGlobals shardGlobals;
    private StockpileShard shard;

    @Parameterized.Parameter
    public MetricType type;

    @Parameterized.Parameters(name = "{0}")
    public static Object[] data() {
        return Stream.of(MetricType.values())
                .filter(k -> k != MetricType.UNRECOGNIZED && k != MetricType.METRIC_TYPE_UNSPECIFIED)
                .toArray();
    }

    @BeforeClass
    public static void beforeClass() {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED);
        testContextManager = new TestContextManager(StockpileShardMetricTypeTest.class);
    }

    @Before
    public void setUp() throws Exception {
        testContextManager.prepareTestInstance(this);

        final long tabletId = kikimrKvClientInMem.createKvTablet();
        shard = new StockpileShard(shardGlobals, StockpileShardId.random(), tabletId, testName.getMethodName());
        shard.start();
        shard.waitForInitializedOrAnyError();
    }

    @After
    public void tearDown() {
        shard.stop();
    }

    @Test
    public void writeUnsortedBatchReadSorted() {
        final long localId = StockpileLocalId.random();
        MetricArchiveMutable source = new MetricArchiveMutable();
        source.setType(type);
        for (int index = 0; index < 100; index++) {
            // points can be unsorted by random
            source.addRecord(randomPoint(type));
        }

        writeSync(localId, source);
        StockpileMetricReadResponse result = readSync(localId);

        source.sortAndMerge();
        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(MetricTypeTransfers.of(type, result.getHeader().getType(), source.iterator()));
        assertEquals(expected, AggrGraphDataArrayList.of(result.getTimeseries()));
        close(source);
    }

    @Test
    public void writeUnsortedByOnePointReadSorted() {
        final long localId = StockpileLocalId.random();
        AggrGraphDataArrayList source = new AggrGraphDataArrayList();

        for (int index = 0; index < 100; index++) {
            AggrPoint point = randomPoint(type);
            writeSync(localId, point);
            source.addRecord(point);
        }

        source.sortAndMerge();
        StockpileMetricReadResponse result = readSync(localId);

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(MetricTypeTransfers.of(type, result.getHeader().getType(), source.iterator()));
        assertEquals(expected, AggrGraphDataArrayList.of(result.getTimeseries()));
    }

    @Test
    public void writeHeaderSaved() {
        MetricType expectedType = type != MetricType.RATE
                ? type
                : MetricType.DGAUGE;

        final long localId = StockpileLocalId.random();
        writeSync(localId, randomPoint(type));
        StockpileMetricReadResponse result = readSync(localId);
        assertEquals(expectedType, result.getHeader().getType());
    }

    @Test
    public void readHeaderSavedFromCache() {
        MetricType expectedType = type != MetricType.RATE
                ? type
                : MetricType.DGAUGE;
        final long localId = StockpileLocalId.random();
        writeSync(localId, randomPoint(type));

        StockpileMetricReadResponse fromDisk = readSync(localId);
        assertEquals(expectedType, fromDisk.getHeader().getType());

        writeSync(localId, randomPoint(type));
        writeSync(localId, randomPoint(type));

        StockpileMetricReadResponse fromCache = readSync(localId);
        assertEquals(expectedType, fromCache.getHeader().getType());
    }

    @Test
    public void readFromCache() {
        final long localId = StockpileLocalId.random();
        final int mask = randomMask(type) & ~StockpileColumn.MERGE.mask();
        MetricArchiveMutable source = new MetricArchiveMutable();
        source.setType(type);
        for (int index = 0; index < 100; index++) {
            // points can be unsorted by random
            source.addRecord(randomPoint(mask));
        }
        source.sortAndMerge();

        writeSync(localId, source);
        StockpileMetricReadResponse fromDisk = readSync(localId);
        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(MetricTypeTransfers.of(type, fromDisk.getHeader().getType(), source.iterator()));
        assertEquals(expected, AggrGraphDataArrayList.of(fromDisk.getTimeseries()));

        StockpileMetricReadResponse fromCache = readSync(localId);
        assertEquals(expected, AggrGraphDataArrayList.of(fromCache.getTimeseries()));
    }

    @Test
    public void readFromCachePlusDisk() {
        final long localId = StockpileLocalId.random();
        final int mask = randomMask(type) & ~StockpileColumn.MERGE.mask();

        AggrGraphDataArrayList source = new AggrGraphDataArrayList();
        for (int index = 0; index < 100; index++) {
            AggrPoint point = randomPoint(mask);
            writeSync(localId, point);
            source.addRecord(point);
        }

        StockpileMetricReadResponse fromDisk = readSync(localId);
        assertFalse(fromDisk.getTimeseries().isEmpty());

        for (int index = 0; index < 100; index++) {
            AggrPoint point = randomPoint(mask);
            writeSync(localId, point);
            source.addRecord(point);
        }

        StockpileMetricReadResponse fromCacheAndDisk = readSync(localId);
        source.sortAndMerge();
        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(MetricTypeTransfers.of(type, fromDisk.getHeader().getType(), source.iterator()));
        assertEquals(expected, AggrGraphDataArrayList.of(fromCacheAndDisk.getTimeseries()));
    }

    private void writeSync(long localId, AggrPoint... points) {
        var archive = new MetricArchiveMutable();
        archive.setType(type);
        for (AggrPoint point : points) {
            archive.addRecord(point);
        }

        shard.pushBatch(StockpileWriteRequest.newBuilder()
                .addArchive(localId, archive)
                .build())
                .join();
    }

    private void writeSync(long localId, MetricArchiveMutable archive) {
        shard.pushBatch(StockpileWriteRequest.newBuilder()
                .addArchiveCopy(localId, archive)
                .build())
                .join();
    }

    private StockpileMetricReadResponse readSync(long localId) {
        return readSync(StockpileMetricReadRequest.newBuilder()
                .setLocalId(localId)
                .build());
    }

    private StockpileMetricReadResponse readSync(StockpileMetricReadRequest request) {
        return CompletableFutures.join(shard.readOne(request));
    }
}
