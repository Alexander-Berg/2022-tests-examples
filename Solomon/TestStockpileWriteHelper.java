package ru.yandex.solomon.coremon.stockpile;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.solomon.codec.archive.MetricArchiveMutable;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.column.StockpileColumns;
import ru.yandex.solomon.model.protobuf.MetricType;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.stockpile.client.shard.StockpileMetricId;


/**
 * @author Sergey Polovko
 */
@ParametersAreNonnullByDefault
public final class TestStockpileWriteHelper implements CoremonShardStockpileWriteHelper {
    private final Map<StockpileMetricId, MetricArchiveMutable> data = new HashMap<>();

    @Override
    public void addPoint(int shardId, long localId, AggrPoint point, int decimPolicyId, MetricType type) {
        Objects.requireNonNull(type);
        StockpileColumns.ensureColumnSetValid(type, point.columnSet);

        StockpileMetricId id = new StockpileMetricId(shardId, localId);
        data.computeIfAbsent(id, ignore -> {
            MetricArchiveMutable archive = new MetricArchiveMutable();
            archive.setDecimPolicyId((short) decimPolicyId);
            archive.setType(type);
            return archive;
        }).addRecord(point);
    }

    @Override
    public CompletableFuture<Void> write() {
        return CompletableFuture.completedFuture(null);
    }

    public MetricArchiveMutable getArchive(int shardId, long localId) {
        StockpileMetricId id = new StockpileMetricId(shardId, localId);
        MetricArchiveMutable archive = data.get(id);
        if (archive == null) {
            return null;
        }

        return new MetricArchiveMutable(archive);
    }

    public AggrGraphDataArrayList getPoints(int shardId, long localId) {
        MetricArchiveMutable archive = getArchive(shardId, localId);
        if (archive == null) {
            return null;
        }

        return archive.toAggrGraphDataArrayList();
    }

    public void clear() {
        data.clear();
    }

    public int size() {
        return data.size();
    }
}
