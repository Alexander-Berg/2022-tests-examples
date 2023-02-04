package ru.yandex.solomon.alert.unroll;


import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.labels.query.Selectors;
import ru.yandex.solomon.metabase.api.protobuf.EMetabaseStatusCode;
import ru.yandex.solomon.metrics.client.CrossDcMetricsClient;
import ru.yandex.solomon.metrics.client.DcMetricsClient;
import ru.yandex.solomon.metrics.client.MetricsClient;
import ru.yandex.solomon.metrics.client.SolomonClientStub;
import ru.yandex.solomon.metrics.client.UniqueLabelsRequest;
import ru.yandex.solomon.metrics.client.cache.UnrollClient;
import ru.yandex.solomon.metrics.client.cache.UnrollClientImpl;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.solomon.ut.ManualClock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Vladimir Gordiychuk
 */
public class UnrollClientImplTest {
    private static final Duration expireTime = Duration.ofMinutes(1);

    private Unit alice;
    private Unit bob;
    private ManualClock clock;
    private UnrollClient client;

    @Before
    public void setUp() {
        alice = new Unit("alice");
        bob = new Unit("bob");
        var metricsClient = new CrossDcMetricsClient(ImmutableMap.of(alice.name, alice.client, bob.name, bob.client));
        clock = new ManualClock();
        client = new UnrollClientImpl(expireTime, metricsClient, clock.asTicker());
    }

    @After
    public void tearDown() {
        alice.close();
        bob.close();
    }

    @Test
    public void emptyUnroll() {
        var response = client.uniqueLabels(UniqueLabelsRequest.newBuilder()
            .setSelectors(Selectors.parse("host='storage-*-001'"))
            .setLabels(Set.of("host"))
            .build())
            .join();

        assertTrue(response.isOk());
        assertEquals(Set.of(), response.getUniqueLabels());
    }

    @Test
    public void unrollAllSuccess() {
        var one = Labels.of("host", "storage-vla-001");
        var two = Labels.of("host", "storage-sas-001");
        alice.solomon.addMetric(one, AggrGraphDataArrayList.empty());
        bob.solomon.addMetric(two, AggrGraphDataArrayList.empty());
        var response = client.uniqueLabels(UniqueLabelsRequest.newBuilder()
            .setSelectors(Selectors.parse("host='storage-*-001'"))
            .setLabels(Set.of("host"))
            .build())
            .join();

        assertTrue(response.isOk());
        assertTrue(response.isAllDestSuccess());
        assertEquals(Set.of(one, two), response.getUniqueLabels());
    }

    @Test
    public void unrollPartlySuccess() {
        var one = Labels.of("host", "storage-vla-001");
        var two = Labels.of("host", "storage-sas-001");
        alice.solomon.addMetric(one, AggrGraphDataArrayList.empty());
        bob.solomon.addMetric(two, AggrGraphDataArrayList.empty());

        {
            bob.solomon.getMetabase().predefineStatusCode(EMetabaseStatusCode.SHARD_NOT_READY);
            var response = client.uniqueLabels(UniqueLabelsRequest.newBuilder()
                .setSelectors(Selectors.parse("host='storage-*-001'"))
                .setLabels(Set.of("host"))
                .build())
                .join();

            assertTrue(response.isOk());
            assertFalse(response.isAllDestSuccess());
            assertEquals(Set.of(one), response.getUniqueLabels());
        }

        {
            bob.solomon.getMetabase().predefineStatusCode(EMetabaseStatusCode.OK);
            var response = client.uniqueLabels(UniqueLabelsRequest.newBuilder()
                .setSelectors(Selectors.parse("host='storage-*-001'"))
                .setLabels(Set.of("host"))
                .build())
                .join();

            assertTrue(response.isOk());
            assertTrue(response.isAllDestSuccess());
            assertEquals(Set.of(one, two), response.getUniqueLabels());
        }
    }

    @Test
    public void cacheResponse() {
        var one = Labels.of("host", "storage-vla-001");
        var two = Labels.of("host", "storage-sas-001");
        alice.solomon.addMetric(one, AggrGraphDataArrayList.empty());
        bob.solomon.addMetric(two, AggrGraphDataArrayList.empty());

        {
            bob.solomon.getMetabase().predefineStatusCode(EMetabaseStatusCode.SHARD_NOT_READY);
            var response = client.uniqueLabels(UniqueLabelsRequest.newBuilder()
                .setSelectors(Selectors.parse("host='storage-*-001'"))
                .setLabels(Set.of("host"))
                .build())
                .join();

            assertTrue(response.isOk());
            assertFalse(response.isAllDestSuccess());
        }

        {
            bob.solomon.getMetabase().predefineStatusCode(EMetabaseStatusCode.OK);
            var response = client.uniqueLabels(UniqueLabelsRequest.newBuilder()
                .setSelectors(Selectors.parse("host='storage-*-001'"))
                .setLabels(Set.of("host"))
                .build())
                .join();

            assertTrue(response.isOk());
            assertTrue(response.isAllDestSuccess());
            assertEquals(Set.of(one, two), response.getUniqueLabels());
        }

        var tree = Labels.of("host", "storage-iva-001");
        alice.solomon.addMetric(tree, AggrGraphDataArrayList.empty());

        {
            var response = client.uniqueLabels(UniqueLabelsRequest.newBuilder()
                .setSelectors(Selectors.parse("host='storage-*-001'"))
                .setLabels(Set.of("host"))
                .build())
                .join();

            assertTrue(response.isOk());
            assertTrue(response.isAllDestSuccess());
            assertEquals(Set.of(one, two), response.getUniqueLabels());
        }

        clock.passedTime(expireTime.toMillis() * 2, TimeUnit.MILLISECONDS);
        {
            var response = client.uniqueLabels(UniqueLabelsRequest.newBuilder()
                .setSelectors(Selectors.parse("host='storage-*-001'"))
                .setLabels(Set.of("host"))
                .build())
                .join();

            assertTrue(response.isOk());
            assertTrue(response.isAllDestSuccess());
            assertEquals(Set.of(one, two, tree), response.getUniqueLabels());
        }
    }

    private static class Unit {
        private final String name;
        private final SolomonClientStub solomon;
        private final MetricsClient client;

        Unit(String name) {
            this.name = name;
            this.solomon = new SolomonClientStub();
            this.client = new DcMetricsClient(name, solomon.getMetabase(), solomon.getStockpile());
        }

        public void close() {
            solomon.close();
        }
    }
}
