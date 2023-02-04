package ru.yandex.metabase.client.impl;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import io.netty.buffer.UnpooledByteBufAllocator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.monlib.metrics.MetricType;
import ru.yandex.monlib.metrics.encode.spack.format.CompressionAlg;
import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.labels.LabelKeys;
import ru.yandex.solomon.labels.protobuf.LabelConverter;
import ru.yandex.solomon.labels.protobuf.LabelSelectorConverter;
import ru.yandex.solomon.labels.query.Selectors;
import ru.yandex.solomon.metabase.api.protobuf.EMetabaseStatusCode;
import ru.yandex.solomon.metabase.api.protobuf.FindRequest;
import ru.yandex.solomon.metabase.api.protobuf.ResolveManyRequest;
import ru.yandex.solomon.metabase.api.protobuf.ResolveOneRequest;
import ru.yandex.solomon.metabase.api.protobuf.TLabelValuesRequest;
import ru.yandex.solomon.metabase.api.protobuf.TResolveLogsRequest;
import ru.yandex.solomon.model.protobuf.Label;
import ru.yandex.solomon.model.protobuf.Selector;
import ru.yandex.solomon.slog.ResolvedLogMetricsIteratorImpl;
import ru.yandex.solomon.slog.ResolvedLogMetricsRecord;
import ru.yandex.solomon.slog.UnresolvedLogMetaBuilderImpl;
import ru.yandex.solomon.util.protobuf.ByteStrings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.metabase.client.impl.MetabaseClientStub.metric;

/**
 * @author Vladimir Gordiychuk
 */
public class MetabaseClientStubTest {

    private MetabaseClientStub client;

    @Before
    public void setUp() throws Exception {
        client = new MetabaseClientStub(ForkJoinPool.commonPool());
    }

    @After
    public void tearDown() throws Exception {
        client.close();
    }

    @Test
    public void findNone() {
        var response = client.find(FindRequest.newBuilder()
            .addAllSelectors(selectors(""))
            .build())
            .join();
        assertEquals(EMetabaseStatusCode.OK, response.getStatus());
        assertEquals(List.of(), response.getMetricsList());
    }

    @Test
    public void findOne() {
        var alice = metric(MetricType.DGAUGE, Labels.of("name", "alice", "cluster", "one"));
        var bob = metric(MetricType.DGAUGE, Labels.of("name", "bob", "cluster", "two"));

        client.addMetrics(alice, bob);

        {
            var response = client.find(FindRequest.newBuilder()
                .addAllSelectors(selectors("name='al*'"))
                .build())
                .join();

            assertEquals(EMetabaseStatusCode.OK, response.getStatus());
            assertEquals(List.of(alice), response.getMetricsList());
        }

        {
            var response = client.find(FindRequest.newBuilder()
                .addAllSelectors(selectors("cluster='two'"))
                .build())
                .join();

            assertEquals(EMetabaseStatusCode.OK, response.getStatus());
            assertEquals(List.of(bob), response.getMetricsList());
        }
    }

    @Test
    public void findWithMetricName() {
        var alice = metric(MetricType.DGAUGE, Labels.of("sensor", "alice", "cluster", "one"));
        var bob = metric(MetricType.DGAUGE, Labels.of("sensor", "bob", "cluster", "two"));

        List<Label> labelsWithoutName = alice.getLabelsList().stream()
                .filter(label -> !label.getKey().equals(LabelKeys.SENSOR))
                .collect(Collectors.toList());

        var namedAlice = alice.toBuilder()
                .setName("alice")
                .clearLabels()
                .addAllLabels(labelsWithoutName)
                .build();

        client.addMetrics(alice, bob);

        {
            var response = client.find(FindRequest.newBuilder()
                .setNewSelectors(newSelectors("\"alice\"{}"))
                .setFillMetricName(true)
                .build())
                .join();

            assertEquals(EMetabaseStatusCode.OK, response.getStatus());
            assertEquals(List.of(namedAlice), response.getMetricsList());
        }

        {
            var response = client.find(FindRequest.newBuilder()
                .setNewSelectors(newSelectors("{cluster='one'}"))
                .setFillMetricName(true)
                .build())
                .join();

            assertEquals(EMetabaseStatusCode.OK, response.getStatus());
            assertEquals(List.of(namedAlice), response.getMetricsList());
        }
    }

    @Test
    public void resolveOne() {
        var alice = metric(MetricType.DGAUGE, Labels.of("name", "alice", "cluster", "one"));
        var bob = metric(MetricType.DGAUGE, Labels.of("name", "bob", "cluster", "two"));

        client.addMetrics(alice, bob);

        {
            var response = client.resolveOne(ResolveOneRequest.newBuilder()
                .addAllLabels(alice.getLabelsList())
                .build())
                .join();

            assertEquals(EMetabaseStatusCode.OK, response.getStatus());
            assertEquals(alice, response.getMetric());
        }

        {
            var response = client.resolveOne(ResolveOneRequest.newBuilder()
                .addAllLabels(bob.getLabelsList())
                .build())
                .join();

            assertEquals(EMetabaseStatusCode.OK, response.getStatus());
            assertEquals(bob, response.getMetric());
        }
    }

    @Test
    public void resolveMany() {
        var alice = metric(MetricType.DGAUGE, Labels.of("name", "alice", "cluster", "one"));
        var bob = metric(MetricType.DGAUGE, Labels.of("name", "bob", "cluster", "two"));

        client.addMetrics(alice, bob);

        var response = client.resolveMany(ResolveManyRequest.newBuilder()
            .addListLabels(ru.yandex.solomon.model.protobuf.Labels.newBuilder()
                .addAllLabels(alice.getLabelsList())
                .build())
            .build())
            .join();

        assertEquals(EMetabaseStatusCode.OK, response.getStatus());
        assertEquals(List.of(alice), response.getMetricsList());
    }

    @Test
    public void labelValues() {
        var alice = metric(MetricType.DGAUGE, Labels.of("name", "alice", "cluster", "one"));
        var bob = metric(MetricType.DGAUGE, Labels.of("name", "bob", "cluster", "two"));

        client.addMetrics(alice, bob);

        {
            var response = client.labelValues(TLabelValuesRequest.newBuilder()
                .addAllSelectors(selectors("name='a*'"))
                .addLabels("name")
                .build())
                .join();

            assertEquals(EMetabaseStatusCode.OK, response.getStatus());
            assertEquals(List.of("alice"), response.getValuesList()
                .stream()
                .flatMap(v -> v.getValuesList().stream())
                .collect(Collectors.toList()));
        }

        {
            var response = client.labelValues(TLabelValuesRequest.newBuilder()
                .addAllSelectors(selectors("cluster=two"))
                .addLabels("name")
                .build())
                .join();

            assertEquals(EMetabaseStatusCode.OK, response.getStatus());
            assertEquals(List.of("bob"), response.getValuesList()
                .stream()
                .flatMap(v -> v.getValuesList().stream())
                .collect(Collectors.toList()));
        }
    }

    @Test
    public void resolveLog() {
        var alice = metric(MetricType.DGAUGE, Labels.of("name", "alice", "cluster", "one"));
        var bob = metric(MetricType.DGAUGE, Labels.of("name", "bob", "cluster", "one"));

        client.addMetrics(alice, bob);

        int numId = Labels.of("cluster", "one").hashCode();
        try (UnresolvedLogMetaBuilderImpl builder = new UnresolvedLogMetaBuilderImpl(numId, CompressionAlg.LZ4, UnpooledByteBufAllocator.DEFAULT)) {
            builder.onCommonLabels(Labels.of());
            builder.onMetric(MetricType.DGAUGE, Labels.of("name", "alice"), 1, 14);
            builder.onMetric(MetricType.DGAUGE, Labels.of("name", "eva"), 1, 14);
            builder.onMetric(MetricType.IGAUGE, Labels.of("name", "bob"), 100, 14);

            var response = client.resolveLogs(TResolveLogsRequest.newBuilder()
                .setNumId(numId)
                .addUnresolvedLogMeta(ByteStrings.fromByteBuf(builder.build()))
                .build())
                .join();

            assertEquals(EMetabaseStatusCode.OK, response.getStatus());
            assertEquals(1, response.getResolvedLogMetricsCount());
            var record = new ResolvedLogMetricsRecord();
            try (var it = new ResolvedLogMetricsIteratorImpl(ByteStrings.toByteBuf(response.getResolvedLogMetrics(0)))) {
                // alice
                assertTrue(it.next(record));
                assertEquals(MetricType.DGAUGE, record.type);
                assertEquals(Labels.of("name", "alice"), record.labels);
                assertEquals(alice.getMetricId().getShardId(), record.shardId);
                assertEquals(alice.getMetricId().getLocalId(), record.localId);

                // eva
                assertTrue(it.next(record));
                assertEquals(MetricType.DGAUGE, record.type);
                assertEquals(Labels.of("name", "eva"), record.labels);
                assertNotEquals(0, record.shardId);
                assertNotEquals(0, record.localId);

                // bob
                assertTrue(it.next(record));
                assertEquals(MetricType.IGAUGE, record.type);
                assertEquals(Labels.of("name", "bob"), record.labels);
                assertEquals(bob.getMetricId().getShardId(), record.shardId);
                assertEquals(bob.getMetricId().getLocalId(), record.localId);

                assertFalse(it.next(record));
            }
        }

        var find = client.find(FindRequest.newBuilder()
            .addAllSelectors(selectors("cluster='one'"))
            .build())
            .join();

        assertEquals(EMetabaseStatusCode.OK, find.getStatus());
        assertEquals(3, find.getMetricsCount());
        assertEquals(alice, find.getMetrics(0));
        assertEquals(bob.toBuilder().setType(ru.yandex.solomon.model.protobuf.MetricType.IGAUGE).build(), find.getMetrics(1));
        assertEquals(Labels.of("name", "eva", "cluster", "one"), LabelConverter.protoToLabels(find.getMetrics(2).getLabelsList()));
    }

    private List<Selector> selectors(String selectors) {
        return LabelSelectorConverter.selectorsToProto(Selectors.parse(selectors));
    }

    private ru.yandex.solomon.model.protobuf.Selectors newSelectors(String selectors) {
        return LabelSelectorConverter.selectorsToNewProto(Selectors.parse(selectors));
    }
}
