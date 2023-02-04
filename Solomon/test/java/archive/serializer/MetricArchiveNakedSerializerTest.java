package ru.yandex.solomon.codec.archive.serializer;

import com.google.protobuf.ByteString;
import org.junit.Test;

import ru.yandex.solomon.codec.archive.MetricArchiveImmutable;
import ru.yandex.solomon.codec.archive.MetricArchiveMutable;
import ru.yandex.solomon.codec.serializer.StockpileDeserializer;
import ru.yandex.solomon.codec.serializer.StockpileFormat;
import ru.yandex.solomon.model.point.AggrPoint;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.solomon.util.CloseableUtils.close;

/**
 * @author Vladimir Gordiychuk
 */
public class MetricArchiveNakedSerializerTest {

    @Test
    public void emptyArchive() throws Exception {
        MetricArchiveImmutable archive = MetricArchiveImmutable.empty;
        test(StockpileFormat.CURRENT, archive);
        test(StockpileFormat.MIN, archive);
        close(archive);
    }

    @Test
    public void singlePoint() throws Exception {
        MetricArchiveMutable mutable = new MetricArchiveMutable();
        mutable.addRecord(AggrPoint.builder()
                .time("2017-07-17T13:31:12Z")
                .doubleValue(123)
                .build());

        MetricArchiveImmutable archive = mutable.toImmutable();
        test(StockpileFormat.CURRENT, archive);
        test(StockpileFormat.MIN, archive);
        close(mutable, archive);
    }

    private void test(StockpileFormat format, MetricArchiveImmutable sourceArchive) {
        ByteString data = serialize(format, sourceArchive);
        MetricArchiveImmutable deserializedArchive = deserialize(format, data);
        assertThat(deserializedArchive, equalTo(sourceArchive));
        close(deserializedArchive);
    }

    private ByteString serialize(StockpileFormat format, MetricArchiveImmutable archive) {
        return MetricArchiveNakedSerializer
                .serializerForFormatSealed(format)
                .serializeToByteString(archive);
    }

    private MetricArchiveImmutable deserialize(StockpileFormat format, ByteString data) {
        StockpileDeserializer deserializer = new StockpileDeserializer(data);
        return MetricArchiveNakedSerializer.serializerForFormatSealed(format).deserializeToEof(deserializer);
    }
}
