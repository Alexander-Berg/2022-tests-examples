package ru.yandex.stockpile.ser.test.convert;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import ru.yandex.salmon.proto.StockpileCanonicalProto;
import ru.yandex.solomon.codec.serializer.StockpileFormat;
import ru.yandex.stockpile.server.data.chunk.ChunkIndex;
import ru.yandex.stockpile.server.data.index.SnapshotIndexContent;
import ru.yandex.stockpile.server.data.index.SnapshotIndexProperties;
import ru.yandex.stockpile.server.shard.SnapshotReason;

/**
 * @author Vladimir Gordiychuk
 */
public class IndexConverters {
    public static StockpileCanonicalProto.Index toProto(SnapshotIndexContent snapshotIndexContent) {
        StockpileCanonicalProto.Index.Builder r = StockpileCanonicalProto.Index.newBuilder();
        r.setFormat(snapshotIndexContent.getFormat().getFormat());
        r.setTsMillis(snapshotIndexContent.getTsMillis());
        r.setRecordCount(snapshotIndexContent.getRecordCount());
        r.setSnapshotReason(snapshotIndexContent.getSnapshotReason().name());
        r.setMetricCount(snapshotIndexContent.getMetricCount());

        List<StockpileCanonicalProto.ChunkIndex> chunks = Arrays.stream(snapshotIndexContent.getChunks())
                .map(chunkIndex -> {
                    StockpileCanonicalProto.ChunkIndex.Builder chunkIndexProto = StockpileCanonicalProto.ChunkIndex.newBuilder();
                    for (int i = 0; i < chunkIndex.metricCount(); ++i) {
                        StockpileCanonicalProto.ChunkIndexRecord.Builder record = chunkIndexProto.addRecordsBuilder();
                        record.setMetricId(chunkIndex.getLocalIdsSortedArray()[i]);
                        record.setLastTsMillis(chunkIndex.getLastTssMillisArray()[i]);
                        record.setSize(chunkIndex.getSize(i));
                    }
                    return chunkIndexProto.build();
                })
                .collect(Collectors.toList());
        r.addAllChunks(chunks);

        return r.build();
    }

    public static SnapshotIndexContent fromProto(StockpileCanonicalProto.Index index) {
        ChunkIndex[] chunks = index.getChunksList().stream()
                .map(chunkIndex -> {
                    ChunkIndex r = new ChunkIndex();
                    for (StockpileCanonicalProto.ChunkIndexRecord record : chunkIndex.getRecordsList()) {
                        r.addMetric(record.getMetricId(), record.getLastTsMillis(), record.getSize());
                    }
                    return r;
                })
                .toArray(ChunkIndex[]::new);
        var properties = new SnapshotIndexProperties()
            .setCreatedAt(index.getTsMillis())
            .setRecordCount(index.getRecordCount())
            .setSnapshotReason(SnapshotReason.valueOf(index.getSnapshotReason()))
            .setMetricCount(index.getMetricCount());
        return new SnapshotIndexContent(
                StockpileFormat.byNumber(index.getFormat()),
                properties,
                chunks);
    }
}
