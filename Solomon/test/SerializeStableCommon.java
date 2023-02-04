package ru.yandex.stockpile.ser.test;

import java.io.File;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.salmon.proto.StockpileCanonicalProto;
import ru.yandex.solomon.codec.archive.MetricArchiveImmutable;
import ru.yandex.solomon.codec.archive.serializer.MetricArchiveNakedSerializer;
import ru.yandex.solomon.codec.serializer.StockpileFormat;
import ru.yandex.stockpile.ser.test.convert.ArchiveConverters;
import ru.yandex.stockpile.ser.test.convert.IndexConverters;
import ru.yandex.stockpile.ser.test.convert.LogEntryConverters;
import ru.yandex.stockpile.server.data.index.SnapshotIndexContent;
import ru.yandex.stockpile.server.data.index.SnapshotIndexContentSerializer;
import ru.yandex.stockpile.server.data.log.StockpileLogEntryContent;
import ru.yandex.stockpile.server.data.log.StockpileLogEntryContentSerializer;

/**
 * @author Stepan Koltsov
 */
@ParametersAreNonnullByDefault
public class SerializeStableCommon {
    public static final SerializeStableContext<MetricArchiveImmutable, StockpileCanonicalProto.Archive> multiArchive =
        new SerializeStableContext<>(
            MetricArchiveNakedSerializer::serializerForFormatSealed,
            StockpileCanonicalProto.Archive.getDefaultInstance(),
            ArchiveConverters::toProto,
            proto -> ArchiveConverters.fromProto(proto).toImmutableNoCopy(),
            (message, format) -> message);

    public static final SerializeStableContext<StockpileLogEntryContent, StockpileCanonicalProto.LogEntry> logEntry =
        new SerializeStableContext<>(
            StockpileLogEntryContentSerializer::serializerForFormat,
            StockpileCanonicalProto.LogEntry.getDefaultInstance(),
            LogEntryConverters::toProto,
            LogEntryConverters::fromProto,
            (message, format) -> message);

    public static final SerializeStableContext<SnapshotIndexContent, StockpileCanonicalProto.Index> index =
        new SerializeStableContext<>(
            SnapshotIndexContentSerializer::contentSerializerForFormat,
            StockpileCanonicalProto.Index.getDefaultInstance(),
            IndexConverters::toProto,
            IndexConverters::fromProto,
            (message, format) -> {
                StockpileCanonicalProto.Index.Builder b = message.toBuilder();
                b.setFormat(format.getFormat());
                return b.build();
            });

    public static final File dataDir = new File("services/stockpile/bin/src/ser/test");

    @Nonnull
    public static File protoFile(String base) {
        return new File(dataDir, base + ".pbtext");
    }

    @Nonnull
    public static File binFile(String base, StockpileFormat format) {
        return new File(dataDir, base + "-" + format.getFormat() + ".bin");
    }

    public static SerializeStableTestDef[] tests() {
        return new SerializeStableTestDef[] {
            new SerializeStableTestDef("MultiArchive", multiArchive, StockpileFormat.MIN),
            new SerializeStableTestDef("MultiArchiveFromSummaryInt64_27", multiArchive, StockpileFormat.MIN),
            new SerializeStableTestDef("MultiArchiveFromSummaryDouble_27", multiArchive, StockpileFormat.MIN),
            new SerializeStableTestDef("MultiArchiveFromHistogramDoubleBounds_28", multiArchive, StockpileFormat.MIN),
            new SerializeStableTestDef("MultiArchiveMetricType", multiArchive, StockpileFormat.MIN),
            new SerializeStableTestDef("MultiArchiveSummaryDouble_33", multiArchive, StockpileFormat.MIN),
            new SerializeStableTestDef("MultiArchiveSummaryInt64_33", multiArchive, StockpileFormat.MIN),
            new SerializeStableTestDef("LogEntry", logEntry, StockpileFormat.MIN),
            new SerializeStableTestDef("Index", index, StockpileFormat.MIN),
        };
    }
}
