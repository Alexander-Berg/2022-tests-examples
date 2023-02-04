package ru.yandex.stockpile.ser.test.convert;

import ru.yandex.misc.lang.ShortUtils;
import ru.yandex.salmon.proto.StockpileCanonicalProto;
import ru.yandex.solomon.codec.archive.MetricArchiveGeneric;
import ru.yandex.solomon.codec.archive.MetricArchiveMutable;
import ru.yandex.solomon.codec.archive.header.MetricHeader;
import ru.yandex.solomon.model.point.column.StockpileColumnSet;
import ru.yandex.stockpile.server.shard.cache.MetricSnapshot;

/**
 * @author Vladimir Gordiychuk
 */
public class ArchiveConverters {
    public static StockpileCanonicalProto.Archive toProto(MetricArchiveGeneric archive) {
        return StockpileCanonicalProto.Archive.newBuilder()
                .setHeader(toProto(archive.header()))
                .addAllColumns(archive.columnSet().canonicalColumns())
                .addAllRecords(ArchiveRecordConverters.toProto(archive))
                .build();
    }

    public static StockpileCanonicalProto.Archive toProto(MetricSnapshot snapshot) {
        return StockpileCanonicalProto.Archive.newBuilder()
            .setHeader(toProto(snapshot.header()))
            .addAllColumns(snapshot.columnSet().canonicalColumns())
            .addAllRecords(ArchiveRecordConverters.toProto(snapshot))
            .build();
    }

    public static MetricArchiveMutable fromProto(StockpileCanonicalProto.Archive proto) {
        MetricArchiveMutable archive = new MetricArchiveMutable(fromProto(proto.getHeader()));
        StockpileColumnSet columnSet = StockpileColumnSet.fromCanonicalColumns(proto.getColumnsList());
        archive.ensureCapacity(columnSet.columnSetMask(), proto.getRecordsCount());

        for (StockpileCanonicalProto.ArchiveRecord record : proto.getRecordsList()) {
            archive.addRecordData(columnSet.columnSetMask(), ArchiveRecordConverters.fromProto(record));
        }
        return archive;
    }

    private static StockpileCanonicalProto.MetricHeader toProto(MetricHeader header) {
        return StockpileCanonicalProto.MetricHeader.newBuilder()
                .setDeleteBefore(header.getDeleteBefore())
                .setOwnerProjectId(header.getOwnerProjectId())
                .setOwnerShardId(header.getOwnerShardId())
                .setDecimPolicyId(header.getDecimPolicyId())
                .setType(header.getType())
                .build();
    }

    private static MetricHeader fromProto(StockpileCanonicalProto.MetricHeader proto) {
        return new MetricHeader(
                proto.getDeleteBefore(),
                proto.getOwnerProjectId(),
                proto.getOwnerShardId(),
                ShortUtils.toShortExact(proto.getDecimPolicyId()),
                proto.getType());
    }
}
