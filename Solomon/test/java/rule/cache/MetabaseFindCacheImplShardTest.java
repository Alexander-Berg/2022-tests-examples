package ru.yandex.solomon.alert.rule.cache;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import ru.yandex.monlib.metrics.MetricType;
import ru.yandex.solomon.labels.LabelsFormat;
import ru.yandex.solomon.labels.query.Selectors;
import ru.yandex.solomon.metrics.client.DcMetricsClient;
import ru.yandex.solomon.metrics.client.FindResponse;
import ru.yandex.solomon.metrics.client.SolomonClientStub;
import ru.yandex.solomon.metrics.client.cache.FindCacheOptions;
import ru.yandex.solomon.metrics.client.cache.MetabaseFindCacheImpl;
import ru.yandex.solomon.model.MetricKey;
import ru.yandex.solomon.model.StockpileKey;
import ru.yandex.solomon.model.protobuf.MetricId;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.stockpile.client.shard.StockpileLocalId;
import ru.yandex.stockpile.client.shard.StockpileShardId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Vladimir Gordiychuk
 */
public class MetabaseFindCacheImplShardTest {
    @Rule
    public TestName testName = new TestName();
    private SolomonClientStub solomon;
    private DcMetricsClient client;
    private MetabaseFindCacheImpl cache;

    @Before
    public void setUp() throws Exception {
        solomon = new SolomonClientStub();
        var dcClient = new DcMetricsClient(
                testName.getMethodName(),
                solomon.getMetabase(),
                solomon.getStockpile());

        cache = new MetabaseFindCacheImpl(dcClient, FindCacheOptions.newBuilder().build());
    }

    @After
    public void tearDown() throws Exception {
        solomon.close();
    }

    @Test
    public void shardAbsent() {
        var response = find("project=test", 10);
        assertTrue(response.isOk());
        assertEquals(List.of(), response.getMetrics());
    }

    @Test
    public void onlyOneShard() {
        var metricKey = addMetric("project=p, cluster=c, service=s, name=alice");

        List<String> selectors = List.of(
                "project=p, cluster=c, service=s, name=alice",
                "project=p, cluster=c, service=s",
                "project=p, cluster=c, name=alice",
                "project=p, cluster=*, name=alice");

        for (var str : selectors) {
            var response = find(str, 10);
            assertTrue(str, response.isOk());
            assertEquals(str, List.of(metricKey), response.getMetrics());
            assertFalse(response.isTruncated());
        }
    }

    @Test
    public void multiShard() {
        var one = addMetric("project=p, cluster=c, service=s-1, name=alice");
        var two = addMetric("project=p, cluster=c, service=s-1, name=bob");
        var tree = addMetric("project=p, cluster=c, service=s-2, name=alice");

        List<String> selectors = List.of(
                "project=p, cluster=c, name=alice|bob",
                "project=p, cluster=c, service=s-*, name=alice|bob",
                "project=p, cluster=c, service=s-*",
                "project=p, service=s-*"
        );

        for (var str : selectors) {
            var response = find(str, 10);
            assertTrue(str, response.isOk());
            assertFalse(response.isTruncated());

            var actual = new ArrayList<>(response.getMetrics());
            actual.sort(Comparator.comparing(o -> o.getLabels().toString()));

            var expected = new ArrayList<>(List.of(one, two, tree));
            expected.sort(Comparator.comparing(o -> o.getLabels().toString()));

            assertEquals(str, expected, actual);
        }
    }

    @Test
    public void multiShardLimited() {
        addMetric("project=p, cluster=c, service=s-1, name=alice");
        addMetric("project=p, cluster=c, service=s-1, name=bob");
        addMetric("project=p, cluster=c, service=s-2, name=alice");

        var response = find("project=p, name=*", 1);
        assertTrue(response.isOk());
        assertTrue(response.isTruncated());
        assertEquals(1, response.getMetrics().size());
    }


    private FindResponse find(String selector, int limit) {
        return cache.find(Selectors.parse(selector), limit, 30_000, 30_000).join();
    }

    private MetricKey addMetric(String labelsStr) {
        var labels = LabelsFormat.parse(labelsStr);
        var stockpileKey = new StockpileKey(testName.getMethodName(), StockpileShardId.random(42), StockpileLocalId.random());
        var metricKey = new MetricKey(MetricType.DGAUGE, labels, stockpileKey);

        var metricId = MetricId.newBuilder().setShardId(stockpileKey.getShardId()).setLocalId(stockpileKey.getLocalId()).build();
        solomon.addMetric(metricId, "", labels, ru.yandex.solomon.model.protobuf.MetricType.DGAUGE, AggrGraphDataArrayList.empty());
        return metricKey;
    }
}
