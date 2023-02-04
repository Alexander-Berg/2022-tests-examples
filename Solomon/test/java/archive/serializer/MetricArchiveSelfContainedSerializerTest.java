package ru.yandex.solomon.codec.archive.serializer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.solomon.codec.archive.MetricArchiveImmutable;
import ru.yandex.solomon.codec.archive.MetricArchiveMutable;
import ru.yandex.solomon.codec.archive.header.MetricHeader;
import ru.yandex.solomon.codec.serializer.HeapStockpileSerializer;
import ru.yandex.solomon.codec.serializer.StockpileDeserializer;
import ru.yandex.solomon.codec.serializer.StockpileFormat;
import ru.yandex.solomon.model.protobuf.MetricType;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.solomon.model.point.AggrPoints.point;
import static ru.yandex.solomon.util.CloseableUtils.close;

/**
 * @author Vladimir Gordiychuk
 */
@RunWith(Parameterized.class)
public class MetricArchiveSelfContainedSerializerTest {
    @Parameterized.Parameter
    public StockpileFormat format;

    @Parameterized.Parameters(name = "{0}")
    public static Object[] data() {
        return StockpileFormat.values();
    }

    @Test
    public void mutableSerializeDeserialize() {
        MetricArchiveMutable source = new MetricArchiveMutable();
        source.setOwnerProjectId(1);
        source.setOwnerShardId(42);
        source.addRecord(point("2015-10-21T12:13:16Z", 1));
        source.addRecord(point("2015-10-21T12:13:30Z", 2));
        source.addRecord(point("2015-10-21T12:13:50Z", 3));

        MetricArchiveMutable result = deserializeAsMutable(serialize(source));
        assertThat(result, equalTo(source));
        close(source, result);
    }

    @Test
    public void immutableSerializeDeserialize() {
        MetricArchiveMutable source = new MetricArchiveMutable();
        source.setOwnerProjectId(1);
        source.setOwnerShardId(42);
        source.addRecord(point("2015-10-21T12:13:16Z", 1));
        source.addRecord(point("2015-10-21T12:13:30Z", 2));
        source.addRecord(point("2015-10-21T12:13:50Z", 3));
        MetricArchiveImmutable expected = source.toImmutable();

        MetricArchiveImmutable result = deserializeAsImmutable(serialize(expected));
        assertThat(result, equalTo(expected));
        close(source, expected, result);
    }

    @Test
    public void mutableAsImmutable() {
        MetricArchiveMutable source = new MetricArchiveMutable();
        source.setOwnerProjectId(1);
        source.setOwnerShardId(42);
        source.addRecord(point("2015-10-21T12:13:16Z", 1));
        source.addRecord(point("2015-10-21T12:13:30Z", 2));
        source.addRecord(point("2015-10-21T12:13:50Z", 3));

        MetricArchiveImmutable result = deserializeAsImmutable(serialize(source));
        var immutable = source.toImmutable();
        assertThat(result, equalTo(immutable));
        close(source, result, immutable);
    }

    @Test
    public void immutableAsMutable() {
        MetricArchiveMutable source = new MetricArchiveMutable();
        source.setOwnerProjectId(1);
        source.setOwnerShardId(42);
        source.addRecord(point("2015-10-21T12:13:16Z", 1));
        source.addRecord(point("2015-10-21T12:13:30Z", 2));
        source.addRecord(point("2015-10-21T12:13:50Z", 3));

        var immutable = source.toImmutable();
        MetricArchiveMutable result = deserializeAsMutable(serialize(immutable));
        assertThat(result, equalTo(source));
        close(source, result, immutable);
    }

    @Test
    public void serializeDeserializeKind() {
        for (MetricType type : MetricType.values()) {
            if (type == MetricType.UNRECOGNIZED) {
                continue;
            }

            MetricArchiveMutable source = new MetricArchiveMutable(MetricHeader.defaultValue, format);
            source.setType(type);

            byte[] serialized = serialize(source);
            MetricArchiveMutable m = deserializeAsMutable(serialized);
            assertThat(m.getType(), equalTo(type));
            assertThat(m, equalTo(source));

            MetricArchiveImmutable i = deserializeAsImmutable(serialized);
            assertThat(i.getType(), equalTo(type));
            var sourceImmutable = source.toImmutable();
            assertThat(i, equalTo(sourceImmutable));
            close(source, m, i, sourceImmutable);
        }
    }

    private byte[] serialize(MetricArchiveMutable archive) {
        var s = new HeapStockpileSerializer();
        MetricArchiveMutableSelfContainedSerializer.makeSerializerForVersion(format).serializeWithLength(archive, s);
        return s.build();
    }

    private byte[] serialize(MetricArchiveImmutable archive) {
        var s = new HeapStockpileSerializer();
        MetricArchiveImmutableSelfContainedSerializer.makeSerializerForVersion(format).serializeWithLength(archive, s);
        return s.build();
    }

    private MetricArchiveMutable deserializeAsMutable(byte[] bytes) {
        return MetricArchiveMutableSelfContainedSerializer.makeSerializerForVersion(format)
                .deserializeWithLength(new StockpileDeserializer(bytes));
    }

    private MetricArchiveImmutable deserializeAsImmutable(byte[] bytes) {
        return MetricArchiveImmutableSelfContainedSerializer.makeSerializerForVersion(format)
                .deserializeWithLength(new StockpileDeserializer(bytes));
    }
}
