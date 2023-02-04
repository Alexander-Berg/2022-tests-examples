package ru.yandex.stockpile.ser.test.convert;

import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.AbstractLongComparator;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrays;

import ru.yandex.salmon.proto.StockpileCanonicalProto;
import ru.yandex.solomon.codec.archive.MetricArchiveMutable;
import ru.yandex.stockpile.client.shard.StockpileLocalId;
import ru.yandex.stockpile.server.data.DeletedShardSet;
import ru.yandex.stockpile.server.data.log.StockpileLogEntryContent;

/**
 * @author Vladimir Gordiychuk
 */
public class LogEntryConverters {

    public static StockpileCanonicalProto.LogEntry toProto(StockpileLogEntryContent logEntryContent) {
        StockpileCanonicalProto.LogEntry.Builder r = StockpileCanonicalProto.LogEntry.newBuilder();
        long[] keys = logEntryContent.getDataByMetricId().keySet().toLongArray();
        LongArrays.quickSort(keys, new AbstractLongComparator() {
            @Override
            public int compare(long a, long b) {
                return StockpileLocalId.compare(a, b);
            }
        });

        for (long metricId : keys) {
            r.addMetrics(StockpileCanonicalProto.LogEntryMetric.newBuilder()
                    .setMetricId(metricId)
                    .setArchive(ArchiveConverters.toProto(logEntryContent.archiveRefByLocalId(metricId)))
                    .build());
        }

        return r.build();
    }

    public static StockpileLogEntryContent fromProto(StockpileCanonicalProto.LogEntry proto) {

        Long2ObjectOpenHashMap<MetricArchiveMutable> dataByMetricId = new Long2ObjectOpenHashMap<>();

        for (StockpileCanonicalProto.LogEntryMetric metric : proto.getMetricsList()) {
            MetricArchiveMutable archive = ArchiveConverters.fromProto(metric.getArchive());
            dataByMetricId.put(metric.getMetricId(), archive);
        }

        return new StockpileLogEntryContent(dataByMetricId, new DeletedShardSet(), new Int2LongOpenHashMap());
    }
}
