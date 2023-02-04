package ru.yandex.solomon.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.TextFormat;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import yandex.monitoring.slog.metric.TMetric;
import yandex.monitoring.slog.metric.TShardData;

import ru.yandex.monlib.metrics.MetricType;
import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.codec.archive.MetricArchiveMutable;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.column.CountColumn;
import ru.yandex.solomon.model.point.column.MergeColumn;
import ru.yandex.solomon.model.point.column.StepColumn;
import ru.yandex.solomon.model.point.column.StockpileColumns;
import ru.yandex.solomon.model.protobuf.HistogramConverter;
import ru.yandex.solomon.model.protobuf.LogHistogramConverter;
import ru.yandex.solomon.model.protobuf.MetricTypeConverter;
import ru.yandex.solomon.model.protobuf.SummaryConverter;
import ru.yandex.solomon.protos.TLabel;
import ru.yandex.solomon.slog.LogDataHeader;
import ru.yandex.solomon.slog.LogDataIterator;
import ru.yandex.solomon.slog.LogDataIteratorImpl;
import ru.yandex.solomon.slog.UnresolvedLogMetaHeader;
import ru.yandex.solomon.slog.UnresolvedLogMetaIteratorImpl;
import ru.yandex.solomon.slog.UnresolvedLogMetaRecord;
import ru.yandex.solomon.slog.compression.DecodeStream;
import ru.yandex.solomon.slog.compression.EncodeStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vladimir Gordiychuk
 */
@RunWith(Parameterized.class)
public class SlogParsingTest {

    @Parameterized.Parameter
    public Path dir;

    @Parameterized.Parameters(name = "{0}")
    public static List<Path> parameters() throws IOException {
        List<Path> result = new ArrayList<>();
        try (var stream = Files.list(Path.of("."))) {
            var it = stream.iterator();
            while (it.hasNext()) {
                var path = it.next();
                result.add(path);
            }
        }
        assertNotEquals("Not found test data", 0, result.size());
        return result;
    }

    @Test
    public void validMetaHeader() {
        var expected = metrics();
        var bytes = read("meta.bin");
        var header = new UnresolvedLogMetaHeader(Unpooled.wrappedBuffer(bytes));
        assertEquals("numId", expected.getNumId(), header.numId);
        assertEquals("metricsCount", expected.getMetricsCount(), header.metricsCount);
        assertEquals("pointsCount", expected.getMetricsList().stream().mapToInt(TMetric::getPointsCount).sum(), header.pointsCount);
    }

    @Test
    public void validDataHeader() {
        var expected = metrics();
        var bytes = read("data.bin");
        var header = new LogDataHeader(Unpooled.wrappedBuffer(bytes));
        assertEquals("numId", expected.getNumId(), header.numId);
        assertEquals("commonTsMillis", expected.getCommonTimeMillis(), header.commonTsMillis);
        assertEquals("numId", expected.getStepMillis(), header.stepMillis);
        assertEquals("metricsCount", expected.getMetricsCount(), header.metricsCount);
        assertEquals("pointsCount", expected.getMetricsList().stream().mapToInt(TMetric::getPointsCount).sum(), header.pointsCount);
    }

    @Test
    public void parsing() {
        var metrics = metrics();
        var meta = read("meta.bin");
        var data = read("data.bin");

        var metricsIt = Expected.of(metrics).iterator();
        var metaRecord = new UnresolvedLogMetaRecord();
        try (var metaIt = new UnresolvedLogMetaIteratorImpl(Unpooled.wrappedBuffer(meta));
             var dataIt = LogDataIterator.create(Unpooled.wrappedBuffer(data)))
        {
            while (metaIt.next(metaRecord)) {
                assertTrue(metricsIt.hasNext());
                var expected = metricsIt.next();

                assertEquals(expected.type, metaRecord.type);
                assertEquals(expected.labels, metaRecord.labels);
                assertEquals(expected.archive.getRecordCount(), metaRecord.points);
                assertNotEquals(0, metaRecord.dataSize);

                assertTrue(dataIt.hasNext());
                var archive = new MetricArchiveMutable();
                dataIt.next(archive);
                assertEquals(expected.archive, archive);
            }
        }
        assertFalse(metricsIt.hasNext());
    }

    @Test
    public void dataSizeValid() {
        var metrics = metrics();
        var meta = read("meta.bin");
        var data = read("data.bin");
        var metricsIt = Expected.of(metrics).iterator();

        ByteBuf dataBuffer = Unpooled.wrappedBuffer(data);
        var dataHeader = new LogDataHeader(dataBuffer);
        var dataDecode = DecodeStream.create(dataHeader.compressionAlg, dataBuffer);
        var metaRecord = new UnresolvedLogMetaRecord();
        try (var metaIt = new UnresolvedLogMetaIteratorImpl(ByteBufAllocator.DEFAULT.buffer().writeBytes(meta)))
        {
            while (metaIt.next(metaRecord)) {
                assertTrue(metricsIt.hasNext());
                var expected = metricsIt.next();

                assertEquals(expected.type, metaRecord.type);
                assertEquals(expected.labels, metaRecord.labels);
                assertEquals(expected.archive.getRecordCount(), metaRecord.points);

                try (var encoder = EncodeStream.create(dataHeader.compressionAlg, ByteBufAllocator.DEFAULT)) {
                    encoder.write(dataDecode, metaRecord.dataSize);
                    var buffer = encoder.finishStream();
                    try (var dataIt = new LogDataIteratorImpl(dataHeader, buffer)) {
                        assertTrue(dataIt.hasNext());
                        var archive = new MetricArchiveMutable();
                        dataIt.next(archive);
                        assertEquals(expected.archive, archive);
                    } finally {
                        buffer.release();
                    }
                }
            }
        }
    }

    private byte[] read(String fileName) {
        try {
            return Files.readAllBytes(dir.resolve(fileName));
        } catch (Throwable e) {
            throw new RuntimeException("Failed to read file: " + fileName, e);
        }
    }

    private TShardData metrics() {
        try {
            var content = read("metrics.txt");
            var builder = TShardData.newBuilder();
            TextFormat.merge(new String(content, StandardCharsets.UTF_8), builder);
            return builder.build();
        } catch (TextFormat.ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private static Labels toLabels(List<TLabel> labels) {
        var builder = Labels.builder(labels.size());
        for (var proto : labels) {
            builder.add(proto.getName(), proto.getValue());
        }
        return builder.build();
    }

    private static class Expected {
        private MetricType type;
        private Labels labels;
        private MetricArchiveMutable archive = new MetricArchiveMutable();

        private static Expected of(int numId, long commonTime, long stepMillis, Labels commonLabels, TMetric metrics) {
            var result = new Expected();
            result.type = MetricTypeConverter.fromProto(metrics.getType());
            result.labels = commonLabels.addAll(toLabels(metrics.getLabelsList()));

            int mask = StockpileColumns.minColumnSet(metrics.getType()) | StepColumn.mask;
            if (metrics.getMerge()) {
                mask |= MergeColumn.mask | CountColumn.mask;
            }

            result.archive.setType(metrics.getType());
            result.archive.setOwnerShardId(numId);

            var point = new AggrPoint();
            point.columnSet = mask;
            point.stepMillis = stepMillis;
            for (var proto : metrics.getPointsList()) {
                point.tsMillis = proto.getTimestampsMillis();
                if (point.tsMillis == 0) {
                    point.tsMillis = commonTime;
                }
                point.valueNum = proto.getDoubleValue();
                point.valueDenom = proto.getDenom();
                point.histogram = HistogramConverter.fromProto(proto.getHistogram());
                point.logHistogram = LogHistogramConverter.fromProto(proto.getLogHistogram());
                point.summaryDouble = SummaryConverter.fromProto(proto.getSummaryDouble());
                point.summaryInt64 = SummaryConverter.fromProto(proto.getSummaryInt64());
                point.longValue = proto.getLongValue();
                point.merge = metrics.getMerge();
                point.count = proto.getCount();
                result.archive.addRecordData(mask, point);
            }

            return result;
        }

        private static List<Expected> of(TShardData data) {
            List<Expected> result = new ArrayList<>(data.getMetricsCount());
            var commonLabels = toLabels(data.getCommonLabelsList());
            for (var metric : data.getMetricsList()) {
                result.add(Expected.of(data.getNumId(), data.getCommonTimeMillis(), data.getStepMillis(), commonLabels, metric));
            }
            return result;
        }
    }
}
