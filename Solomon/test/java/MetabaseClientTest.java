package ru.yandex.metabase.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.grpc.utils.DefaultClientOptions;
import ru.yandex.grpc.utils.InProcessChannelFactory;
import ru.yandex.metabase.client.util.InMemoryMetabaseCluster;
import ru.yandex.solomon.labels.shard.ShardKey;
import ru.yandex.solomon.metabase.api.protobuf.CreateManyRequest;
import ru.yandex.solomon.metabase.api.protobuf.CreateManyResponse;
import ru.yandex.solomon.metabase.api.protobuf.CreateOneRequest;
import ru.yandex.solomon.metabase.api.protobuf.CreateOneResponse;
import ru.yandex.solomon.metabase.api.protobuf.DeleteManyRequest;
import ru.yandex.solomon.metabase.api.protobuf.DeleteManyResponse;
import ru.yandex.solomon.metabase.api.protobuf.EMetabaseStatusCode;
import ru.yandex.solomon.metabase.api.protobuf.FindRequest;
import ru.yandex.solomon.metabase.api.protobuf.FindResponse;
import ru.yandex.solomon.metabase.api.protobuf.Metric;
import ru.yandex.solomon.metabase.api.protobuf.ResolveManyRequest;
import ru.yandex.solomon.metabase.api.protobuf.ResolveManyResponse;
import ru.yandex.solomon.metabase.api.protobuf.ResolveOneRequest;
import ru.yandex.solomon.metabase.api.protobuf.ResolveOneResponse;
import ru.yandex.solomon.metabase.api.protobuf.TLabelNamesRequest;
import ru.yandex.solomon.metabase.api.protobuf.TLabelNamesResponse;
import ru.yandex.solomon.metabase.api.protobuf.TLabelValues;
import ru.yandex.solomon.metabase.api.protobuf.TLabelValuesRequest;
import ru.yandex.solomon.metabase.api.protobuf.TLabelValuesResponse;
import ru.yandex.solomon.metabase.api.protobuf.TResolveLogsRequest;
import ru.yandex.solomon.metabase.api.protobuf.TSliceOptions;
import ru.yandex.solomon.metabase.api.protobuf.TUniqueLabelsRequest;
import ru.yandex.solomon.metabase.api.protobuf.TUniqueLabelsResponse;
import ru.yandex.solomon.metabase.protobuf.LabelValidationFilter;
import ru.yandex.solomon.model.protobuf.Label;
import ru.yandex.solomon.model.protobuf.Labels;
import ru.yandex.solomon.model.protobuf.MatchType;
import ru.yandex.solomon.model.protobuf.MetricId;
import ru.yandex.solomon.model.protobuf.Selector;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vladimir Gordiychuk
 */
public class MetabaseClientTest {
    private InMemoryMetabaseCluster cluster;
    private MetabaseClient client;
    private MetabaseClientOptions options;

    private static List<Label> toLabels(Map<String, String> source) {
        List<Label> result = new ArrayList<>(source.size());
        for (Map.Entry<String, String> entry : source.entrySet()) {
            result.add(Label.newBuilder()
                    .setKey(entry.getKey())
                    .setValue(entry.getValue())
                    .build());
        }
        return result;
    }

    private static Labels toLabelList(Map<String, String> source) {
        return Labels.newBuilder()
                .addAllLabels(toLabels(source))
                .build();
    }

    private static Selector selector(String key, String pattern) {
        return Selector.newBuilder()
                .setMatchType(MatchType.GLOB)
                .setKey(key)
                .setPattern(pattern)
                .build();
    }

    @Before
    public void setUp() throws Exception {
        cluster = InMemoryMetabaseCluster.newBuilder()
                .serverCount(3)
                .inProcess()
                .withShard("noise", "noise", "noise")
                .build();

        options = MetabaseClientOptions.newBuilder(
                DefaultClientOptions.newBuilder()
                        .setRequestTimeOut(10, TimeUnit.SECONDS)
                        .setChannelFactory(new InProcessChannelFactory()))
                .setExpireClusterMetadata(1, TimeUnit.SECONDS)
                .setClusterMetadataRequestRetryDelay(100, TimeUnit.MILLISECONDS)
                .build();

        client = MetabaseClients.create(cluster.getServerList(), options);
        syncClientMetadata();
    }

    @After
    public void tearDown() throws Exception {
        client.close();
        cluster.close();
    }

    @Test
    public void shardNotFoundCreateOne() throws Exception {
        CreateOneResponse response = client.createOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(
                        toLabels(ImmutableMap.of(
                                "project", "foo",
                                "cluster", "bar",
                                "service", "fooBar",
                                "host", "ya",
                                "sensor", "useTime"
                        ))))
                .build())
                .join();

        assertThat(response.getStatus(), equalTo(EMetabaseStatusCode.SHARD_NOT_FOUND));
    }

    @Test
    public void shardNotFoundCreateMany() throws Exception {
        CreateManyResponse response = client.createMany(CreateManyRequest.newBuilder()
                .addMetrics(newMetric(
                        toLabels(ImmutableMap.of(
                                "project", "foo",
                                "cluster", "bar",
                                "service", "fooBar",
                                "host", "ya",
                                "sensor", "useTime"
                        ))))
                .addMetrics(newMetric(
                        toLabels(ImmutableMap.of(
                                "project", "foo",
                                "cluster", "bar",
                                "service", "fooBar",
                                "host", "ya",
                                "sensor", "idleTime"
                        ))))
                .build())
                .join();

        assertThat(response.getStatus(), equalTo(EMetabaseStatusCode.SHARD_NOT_FOUND));
    }

    @Test
    public void shardNotFoundResolveOne() throws Exception {
        ResolveOneResponse response = client.resolveOne(ResolveOneRequest.newBuilder()
                .addAllLabels(toLabels(ImmutableMap.of(
                        "project", "foo",
                        "cluster", "bar",
                        "service", "fooBar",
                        "host", "ya",
                        "sensor", "useTime"
                )))
                .build())
                .join();

        assertThat(response.getStatus(), equalTo(EMetabaseStatusCode.SHARD_NOT_FOUND));
    }

    @Test
    public void shardNotFoundResolveMany() throws Exception {
        ResolveManyResponse response = client.resolveMany(ResolveManyRequest.newBuilder()
                .addListLabels(toLabelList(ImmutableMap.of(
                        "project", "foo",
                        "cluster", "bar",
                        "service", "fooBar",
                        "host", "ya",
                        "sensor", "useTime"
                )))
                .addListLabels(toLabelList(ImmutableMap.of(
                        "project", "foo",
                        "cluster", "bar",
                        "service", "fooBar",
                        "host", "ya",
                        "sensor", "idleTime"
                )))
                .build())
                .join();

        assertThat(response.getStatus(), equalTo(EMetabaseStatusCode.SHARD_NOT_FOUND));
    }

    @Test
    public void shardNotFoundDeleteMany() throws Exception {
        DeleteManyResponse response = client.deleteMany(DeleteManyRequest.newBuilder()
                .addMetrics(newMetric(toLabels(ImmutableMap.of(
                        "project", "foo",
                        "cluster", "bar",
                        "service", "fooBar",
                        "host", "ya",
                        "sensor", "useTime"
                ))))
                .addMetrics(newMetric(toLabels(ImmutableMap.of(
                        "project", "foo",
                        "cluster", "bar",
                        "service", "fooBar",
                        "host", "ya",
                        "sensor", "idleTime"
                ))))
                .build())
                .join();

        assertThat(response.getStatus(), equalTo(EMetabaseStatusCode.SHARD_NOT_FOUND));
    }

    @Test
    public void findEmptyResultEventIfShardNotExists() throws Exception {
        FindResponse response = syncFind(FindRequest.newBuilder()
                .addSelectors(selector("project", "foo"))
                .addSelectors(selector("cluster", "bar"))
                .addSelectors(selector("service", "fooBar"))
                .addSelectors(selector("sensor", "useTime"))
                .build());

        assertThat(response.getMetricsList(), emptyIterable());
    }

    @Test
    public void findEmptyResultMatchMultiShards() throws Exception {
        FindResponse response = syncFind(FindRequest.newBuilder()
                .addSelectors(selector("project", "foo"))
                .addSelectors(selector("cluster", "sas|man"))
                .addSelectors(selector("service", "fooBar"))
                .addSelectors(selector("sensor", "useTime"))
                .build());

        assertThat(response.getMetricsList(), emptyIterable());
    }

    @Test
    public void createOneSuccess() throws Exception {
        cluster.addShard(new ShardKey("foo", "bar", "fooBar"));
        syncClientMetadata();

        CreateOneResponse response = syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(
                        toLabels(ImmutableMap.of(
                                "project", "foo",
                                "cluster", "bar",
                                "service", "fooBar",
                                "host", "ya",
                                "sensor", "useTime"
                        ))))
                .build());

        assertThat(response.getStatus(), equalTo(EMetabaseStatusCode.OK));
        assertThat(response.getMetric(), not(equalTo(Metric.getDefaultInstance())));
        assertThat(response.getMetric().getMetricId(), not(equalTo(MetricId.getDefaultInstance())));
    }

    @Test
    public void createOneUniqueIdForMetric() throws Exception {
        cluster.addShard(new ShardKey("foo", "bar", "fooBar"));
        syncClientMetadata();

        Metric useTime = syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(
                        toLabels(ImmutableMap.of(
                                "project", "foo",
                                "cluster", "bar",
                                "service", "fooBar",
                                "host", "ya",
                                "sensor", "useTime"
                        ))))
                .build())
                .getMetric();

        Metric idleTime = syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(
                        toLabels(ImmutableMap.of(
                                "project", "foo",
                                "cluster", "bar",
                                "service", "fooBar",
                                "host", "ya",
                                "sensor", "idleTime"
                        ))))
                .build())
                .getMetric();

        assertThat(useTime, not(equalTo(idleTime)));
        assertThat(useTime.getMetricId(), not(equalTo(idleTime.getMetricId())));
    }

    @Test
    public void createOneDuplicate() throws Exception {
        cluster.addShard(new ShardKey("foo", "bar", "fooBar"));
        syncClientMetadata();

        List<Label> labels = toLabels(ImmutableMap.of(
                "project", "foo",
                "cluster", "bar",
                "service", "fooBar",
                "host", "ya",
                "sensor", "useTime"
        ));

        syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(labels))
                .build());

        CreateOneResponse response = client.createOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(labels))
                .build())
                .join();

        assertThat(response.getStatus(), equalTo(EMetabaseStatusCode.DUPLICATE));
    }

    @Test
    public void createMany() throws Exception {
        cluster.addShard(new ShardKey("foo", "bar", "fooBar"));
        syncClientMetadata();

        CreateManyResponse response = syncCreateManyOne(CreateManyRequest.newBuilder()
                .addAllCommonLabels(toLabels(ImmutableMap.of(
                        "project", "foo",
                        "cluster", "bar",
                        "service", "fooBar",
                        "host", "ya"
                )))
                .addMetrics(newMetric(toLabels(ImmutableMap.of("sensor", "useTime"))))
                .addMetrics(newMetric(toLabels(ImmutableMap.of("sensor", "idleTime"))))
                .build());

        assertThat(response.getStatus(), equalTo(EMetabaseStatusCode.OK));
        assertThat(response.getMetricsCount(), equalTo(2));
        assertThat(response.getMetrics(0), not(equalTo(response.getMetrics(1))));
        assertThat(
                response.getMetrics(0)
                        .getMetricId()
                        .getShardId(),
                equalTo(
                        response.getMetrics(1)
                                .getMetricId()
                                .getShardId()
                )
        );
    }

    @Test
    public void createManyDuplicate() throws Exception {
        cluster.addShard(new ShardKey("foo", "bar", "fooBar"));
        syncClientMetadata();

        List<Label> labels = toLabels(ImmutableMap.of(
                "project", "foo",
                "cluster", "bar",
                "service", "fooBar",
                "host", "ya",
                "sensor", "useTime"
        ));

        syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(labels))
                .build());

        CreateManyResponse response = client.createMany(CreateManyRequest.newBuilder()
                .addAllCommonLabels(toLabels(ImmutableMap.of(
                        "project", "foo",
                        "cluster", "bar",
                        "service", "fooBar"
                )))
                .addMetrics(newMetric(
                        toLabels(ImmutableMap.of(
                                "host", "ya",
                                "sensor", "useTime"
                        ))))
                .addMetrics(newMetric(labels))
                .build())
                .join();

        assertThat(response.getStatus(), equalTo(EMetabaseStatusCode.DUPLICATE));
    }

    @Test
    public void resolveOneByPartLabelListNotAble() throws Exception {
        cluster.addShard(new ShardKey("foo", "bar", "fooBar"));
        syncClientMetadata();

        List<Label> labels = toLabels(ImmutableMap.of(
                "project", "foo",
                "cluster", "bar",
                "service", "fooBar",
                "host", "ya",
                "sensor", "useTime"
        ));

        syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(labels))
                .build());

        ResolveOneResponse response = client.resolveOne(ResolveOneRequest.newBuilder()
                .addAllLabels(labels.subList(0, labels.size() - 1))
                .build())
                .join();

        assertThat(response.getStatus(), equalTo(EMetabaseStatusCode.NOT_FOUND));
    }

    @Test
    public void resolveByFullLabelList() throws Exception {
        cluster.addShard(new ShardKey("foo", "bar", "fooBar"));
        syncClientMetadata();

        List<Label> labels = toLabels(ImmutableMap.of(
                "project", "foo",
                "cluster", "bar",
                "service", "fooBar",
                "host", "ya",
                "sensor", "useTime"
        ));

        Metric metric = syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(labels))
                .build())
                .getMetric();

        ResolveOneResponse response = client.resolveOne(ResolveOneRequest.newBuilder()
                .addAllLabels(labels)
                .build())
                .join();

        assertThat(response.getStatus(), equalTo(EMetabaseStatusCode.OK));
        assertThat(response.getMetric(), equalTo(metric));
    }

    @Test
    public void resolveMany() throws Exception {
        cluster.addShard(new ShardKey("foo", "bar", "fooBar"));
        syncClientMetadata();

        Metric exist = syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(toLabels(ImmutableMap.of(
                        "project", "foo",
                        "cluster", "bar",
                        "service", "fooBar",
                        "host", "ya",
                        "sensor", "useTime"
                ))))
                .build())
                .getMetric();

        ResolveManyResponse response = client.resolveMany(ResolveManyRequest.newBuilder()
                .addAllCommonLabels(toLabels(ImmutableMap.of(
                        "project", "foo",
                        "cluster", "bar",
                        "service", "fooBar",
                        "host", "ya"
                )))
                .addListLabels(toLabelList(ImmutableMap.of("sensor", "useTime")))
                .addListLabels(toLabelList(ImmutableMap.of("sensor", "idleTime")))
                .build())
                .join();

        assertThat(response.getStatusMessage(), response.getStatus(), equalTo(EMetabaseStatusCode.OK));
        assertThat(response.getMetricsList(), allOf(hasItem(exist), iterableWithSize(1)));
    }

    @Test
    public void deleteMany() throws Exception {
        cluster.addShard(new ShardKey("foo", "bar", "fooBar"));
        syncClientMetadata();

        Metric useTime = syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(
                        toLabels(ImmutableMap.of(
                                "project", "foo",
                                "cluster", "bar",
                                "service", "fooBar",
                                "host", "ya",
                                "sensor", "useTime"
                        ))))
                .build())
                .getMetric();

        Metric idleTime = syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(
                        toLabels(ImmutableMap.of(
                                "project", "foo",
                                "cluster", "bar",
                                "service", "fooBar",
                                "host", "ya",
                                "sensor", "idleTime"
                        ))))
                .build())
                .getMetric();

        Metric noise = syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(
                        toLabels(ImmutableMap.of(
                                "project", "foo",
                                "cluster", "bar",
                                "service", "fooBar",
                                "host", "ya",
                                "sensor", "noise"
                        ))))
                .build())
                .getMetric();

        DeleteManyResponse response = syncDeleteMany(DeleteManyRequest.newBuilder()
                .addMetrics(useTime)
                .addMetrics(idleTime)
                .build());

        assertThat(response.getStatus(), equalTo(EMetabaseStatusCode.OK));
        assertThat(response.getMetricsList(), allOf(hasItem(useTime), hasItem(idleTime), not(hasItem(noise))));
    }

    @Test
    public void exactlyMatchSearchFromOneMetric() throws Exception {
        cluster.addShard(new ShardKey("foo", "cluster_1", "my_service"));
        syncClientMetadata();

        Metric useTime = syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(
                        toLabels(ImmutableMap.of(
                                "project", "foo",
                                "cluster", "cluster_1",
                                "service", "my_service",
                                "host", "ya",
                                "sensor", "useTime"
                        ))))
                .build())
                .getMetric();

        FindResponse response = syncFind(FindRequest.newBuilder()
                .addSelectors(selector("project", "foo"))
                .addSelectors(selector("cluster", "cluster_1"))
                .addSelectors(selector("service", "my_service"))
                .addSelectors(selector("sensor", "useTime"))
                .build());

        assertThat(response.getMetricsList(), allOf(hasItem(useTime), iterableWithSize(1)));
    }

    @Test
    public void crossShardMatchSearchFromDifferentServer() throws Exception {
        cluster.addShard(new ShardKey("foo", "cluster_1", "first"));
        cluster.addShard(new ShardKey("foo", "cluster_1", "second"));
        cluster.addShard(new ShardKey("test", "test", "test"));
        syncClientMetadata();

        Metric first = syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(toLabels(ImmutableMap.of(
                        "project", "foo",
                        "cluster", "cluster_1",
                        "service", "first",
                        "host", "ya",
                        "sensor", "useTime"
                ))))
                .build())
                .getMetric();

        Metric noise = syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(toLabels(ImmutableMap.of(
                        "project", "test",
                        "cluster", "test",
                        "service", "test",
                        "host", "ya",
                        "sensor", "useTime"
                ))))
                .build())
                .getMetric();

        Metric second = syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(toLabels(ImmutableMap.of(
                        "project", "foo",
                        "cluster", "cluster_1",
                        "service", "second",
                        "host", "ya",
                        "sensor", "useTime"
                ))))
                .build())
                .getMetric();

        FindResponse result = syncFind(FindRequest.newBuilder()
                .addSelectors(selector("project", "foo"))
                .addSelectors(selector("cluster", "cluster_1"))
                .addSelectors(selector("service", "*"))
                .addSelectors(selector("sensor", "useTime"))
                .build());

        assertThat(result.getMetricsList(), allOf(hasItems(first), hasItems(second), not(hasItems(noise))));
    }

    @Test
    public void crossShardSearchFromDifferentServerWithLimit() throws Exception {
        cluster.addShard(new ShardKey("foo", "cluster_1", "first"));
        cluster.addShard(new ShardKey("foo", "cluster_1", "second"));
        cluster.addShard(new ShardKey("test", "test", "test"));
        syncClientMetadata();

        Metric first = syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "foo",
                "cluster", "cluster_1",
                "service", "first",
                "host", "ya",
                "sensor", "useTime"
            ))))
            .build())
            .getMetric();

        Metric noise = syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "test",
                "cluster", "test",
                "service", "test",
                "host", "ya",
                "sensor", "useTime"
            ))))
            .build())
            .getMetric();

        Metric second = syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "foo",
                "cluster", "cluster_1",
                "service", "second",
                "host", "ya",
                "sensor", "useTime"
            ))))
            .build())
            .getMetric();

        FindResponse result = syncFind(FindRequest.newBuilder()
            .addSelectors(selector("project", "foo"))
            .addSelectors(selector("cluster", "cluster_1"))
            .addSelectors(selector("service", "*"))
            .addSelectors(selector("sensor", "useTime"))
            .setSliceOptions(TSliceOptions.newBuilder().setLimit(1).build())
            .build());

        assertThat(result.getMetricsCount(), equalTo(1));
        assertThat(result.getMetricsList(), anyOf(hasItems(first), hasItems(second)));
        assertThat(result.getMetricsList(), not(hasItems(noise)));
    }

    @Test
    public void findDeadline() throws Exception {
        cluster.addShard(new ShardKey("foo", "cluster_1", "my_service"));
        syncClientMetadata();

        syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(toLabels(ImmutableMap.of(
                        "project", "foo",
                        "cluster", "cluster_1",
                        "service", "my_service",
                        "host", "ya",
                        "sensor", "useTime"
                ))))
                .build());

        FindResponse response = client.find(FindRequest.newBuilder()
                .setDeadlineMillis(System.currentTimeMillis() - 10)
                .addSelectors(selector("project", "foo"))
                .addSelectors(selector("cluster", "cluster_1"))
                .addSelectors(selector("service", "my_service"))
                .addSelectors(selector("sensor", "useTime"))
                .build()).join();

        assertThat(response.getStatus(), equalTo(EMetabaseStatusCode.DEADLINE_EXCEEDED));
    }

    @Test
    public void labelValuesDeadline() throws Exception {
        cluster.addShard(new ShardKey("foo", "cluster_1", "my_service"));
        syncClientMetadata();

        syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(toLabels(ImmutableMap.of(
                        "project", "foo",
                        "cluster", "cluster_1",
                        "service", "my_service",
                        "host", "ya",
                        "sensor", "useTime"
                ))))
                .build());

        TLabelValuesResponse response = client.labelValues(TLabelValuesRequest.newBuilder()
                .setDeadlineMillis(System.currentTimeMillis() - 100)
                .addSelectors(selector("project", "foo"))
                .addSelectors(selector("cluster", "cluster_1"))
                .addSelectors(selector("service", "my_service"))
                .addSelectors(selector("sensor", "useTime"))
                .build()).join();

        assertThat(response.getStatus(), equalTo(EMetabaseStatusCode.DEADLINE_EXCEEDED));
    }

    @Test
    public void selectiveMetricSearch() throws Exception {
        cluster.addShard(new ShardKey("foo", "cluster_1", "first"));
        cluster.addShard(new ShardKey("foo", "cluster_1", "second"));
        cluster.addShard(new ShardKey("test", "test", "test"));
        syncClientMetadata();

        Metric first = syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(toLabels(ImmutableMap.of(
                        "project", "foo",
                        "cluster", "cluster_1",
                        "service", "first",
                        "host", "ya",
                        "sensor", "useTime"
                ))))
                .build())
                .getMetric();

        Metric noise = syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(toLabels(ImmutableMap.of(
                        "project", "test",
                        "cluster", "test",
                        "service", "test",
                        "host", "ya",
                        "sensor", "useTime"
                ))))
                .build())
                .getMetric();

        Metric second = syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(toLabels(ImmutableMap.of(
                        "project", "foo",
                        "cluster", "cluster_1",
                        "service", "second",
                        "host", "ya",
                        "sensor", "useTime"
                ))))
                .build())
                .getMetric();

        // Emulate full knowledge about cluster shards state
        client.forceUpdateClusterMetaData().join();

        // Not working node should not affect search in that case,
        // because client already prefetch label list and known that on stopped node
        // absent data for search request.
        cluster.forceStopNodeByAddress(cluster.getAddressByShard(new ShardKey("foo", "cluster_1", "second")));

        FindResponse response = syncFind(FindRequest.newBuilder()
                .addSelectors(selector("project", "foo"))
                .addSelectors(selector("cluster", "cluster_1"))
                .addSelectors(selector("service", "first"))
                .addSelectors(selector("sensor", "useTime"))
                .build());

        assertThat(response.getMetricsList(), allOf(hasItem(first), iterableWithSize(1)));
    }

    @Test
    public void selectiveSearchOnNotReadyNode() throws Exception {
        cluster.addShard(new ShardKey("foo", "cluster_1", "first"));
        cluster.addShard(new ShardKey("foo", "cluster_1", "second"));
        cluster.addShard(new ShardKey("test", "test", "test"));
        syncClientMetadata();

        Metric first = syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(toLabels(ImmutableMap.of(
                        "project", "foo",
                        "cluster", "cluster_1",
                        "service", "first",
                        "host", "ya",
                        "sensor", "useTime"
                ))))
                .build())
                .getMetric();

        Metric noise = syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(toLabels(ImmutableMap.of(
                        "project", "test",
                        "cluster", "test",
                        "service", "test",
                        "host", "ya",
                        "sensor", "useTime"
                ))))
                .build())
                .getMetric();

        Metric second = syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(toLabels(ImmutableMap.of(
                        "project", "foo",
                        "cluster", "cluster_1",
                        "service", "second",
                        "host", "ya",
                        "sensor", "useTime"
                ))))
                .build())
                .getMetric();

        // Emulate full knowledge about cluster shards state
        client.forceUpdateClusterMetaData().join();
        cluster.forceStopNodeByAddress(cluster.getAddressByShard(new ShardKey("foo", "cluster_1", "second")));

        FindResponse response = client.find(FindRequest.newBuilder()
                .setDeadlineMillis(System.currentTimeMillis() + TimeUnit.MILLISECONDS.toMillis(5L))
                .addSelectors(selector("project", "foo"))
                .addSelectors(selector("cluster", "cluster_1"))
                .addSelectors(selector("service", "second"))
                .addSelectors(selector("sensor", "useTime"))
                .build()).join();

        assertThat(response.getStatus(), anyOf(
                equalTo(EMetabaseStatusCode.NODE_UNAVAILABLE),
                equalTo(EMetabaseStatusCode.DEADLINE_EXCEEDED)
        ));
    }

    @Test
    public void searchByLabelThatAbsentInPrefetchList() throws Exception {
        cluster.addShard(new ShardKey("foo", "cluster_1", "first"));
        cluster.addShard(new ShardKey("test", "test", "test"));
        syncClientMetadata();

        Metric first = syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(toLabels(ImmutableMap.of(
                        "project", "foo",
                        "cluster", "cluster_1",
                        "service", "first",
                        "host", "kikimr-001",
                        "sensor", "useTime"
                ))))
                .build())
                .getMetric();

        Metric noise = syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(toLabels(ImmutableMap.of(
                        "project", "test",
                        "cluster", "test",
                        "service", "test",
                        "host", "kikirm-002",
                        "sensor", "useTime"
                ))))
                .build())
                .getMetric();

        FindResponse response = syncFind(FindRequest.newBuilder()
                .addSelectors(selector("host", "kikimr-001"))
                .build());

        assertThat(response.getMetricsList(), allOf(hasItem(first), iterableWithSize(1)));
    }

    @Test
    public void findMetricsSlice() {
        cluster.addShard(new ShardKey("foo", "cluster", "service"));
        cluster.addShard(new ShardKey("test", "test", "test"));

        syncClientMetadata();

        Metric metric1 = syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "foo",
                "cluster", "cluster",
                "service", "service",
                "host", "kikimr-man-01",
                "sensor", "useTime"
            ))))
            .build())
            .getMetric();

        Metric metric2 = syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "foo",
                "cluster", "cluster",
                "service", "service",
                "host", "kikimr-man-02",
                "sensor", "useTime"
            ))))
            .build())
            .getMetric();

        Metric metric3 = syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "foo",
                "cluster", "cluster",
                "service", "service",
                "host", "kikimr-man-03",
                "sensor", "useTime"
            ))))
            .build())
            .getMetric();

        Metric metric4 = syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "foo",
                "cluster", "cluster",
                "service", "service",
                "host", "kikimr-man-04",
                "sensor", "useTime"
            ))))
            .build())
            .getMetric();

        Metric test1 = syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "test",
                "cluster", "test",
                "service", "test",
                "host", "ya",
                "sensor", "useTime"
            ))))
            .build())
            .getMetric();

        Metric test2 = syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "test",
                "cluster", "test",
                "service", "test",
                "host", "man",
                "sensor", "useTime"
            ))))
            .build())
            .getMetric();


        Metric metric5 = syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "foo",
                "cluster", "cluster",
                "service", "service",
                "host", "kikimr-man-05",
                "sensor", "useTime"
            ))))
            .build())
            .getMetric();

        Metric metric6 = syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "foo",
                "cluster", "cluster",
                "service", "service",
                "host", "kikimr-man-06",
                "sensor", "useTime"
            ))))
            .build())
            .getMetric();

        Metric metric7 = syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "foo",
                "cluster", "cluster",
                "service", "service",
                "host", "kikimr-man-07",
                "sensor", "useTime"
            ))))
            .build())
            .getMetric();

        Metric metric8 = syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "foo",
                "cluster", "cluster",
                "service", "service",
                "host", "kikimr-man-08",
                "sensor", "useTime"
            ))))
            .build())
            .getMetric();

        FindResponse result = syncFind(FindRequest.newBuilder()
            .setSliceOptions(TSliceOptions.newBuilder().setOffset(3).setLimit(-1).build())
            .addSelectors(selector("project", "foo"))
            .addSelectors(selector("cluster", "cluster"))
            .addSelectors(selector("service", "service"))
            .addSelectors(selector("sensor", "useTime"))
            .build());

        assertThat(result.getMetricsCount(), equalTo(5));
        assertThat(result.getTotalCount(), equalTo(8));
    }

    @Test
    public void autoRefreshClusterStateBySpecifiedTime() throws Exception {
        // Cluster not contains labels at all
        syncClientMetadata();

        cluster.addShard(new ShardKey("foo", "cluster_1", "first"));
        cluster.addShard(new ShardKey("test", "test", "test"));

        TimeUnit.MILLISECONDS.sleep(options.getMetadataExpireMillis() * 4);

        Metric first = syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(toLabels(ImmutableMap.of(
                        "project", "foo",
                        "cluster", "cluster_1",
                        "service", "first",
                        "host", "kikimr-001",
                        "sensor", "useTime"
                ))))
                .build())
                .getMetric();

        Metric noise = syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(toLabels(ImmutableMap.of(
                        "project", "test",
                        "cluster", "test",
                        "service", "test",
                        "host", "kikirm-002",
                        "sensor", "useTime"
                ))))
                .build())
                .getMetric();

        FindResponse response = syncFind(FindRequest.newBuilder()
                .addSelectors(selector("project", "foo"))
                .addSelectors(selector("cluster", "cluster_1"))
                .build());

        assertThat(response.getMetricsList(), allOf(hasItem(first), iterableWithSize(1)));
    }

    @Test
    public void autoRefreshServerStatusFromFailedNodeAfterRetry() throws Exception {
        // Cluster not contains labels at all
        syncClientMetadata();

        cluster.addShard(new ShardKey("foo", "cluster_1", "first"));
        cluster.addShard(new ShardKey("test", "test", "test"));

        // Shutdown now with not interesting for us labels
        cluster.forceStopNodeByAddress(cluster.getServerList().get(1));
        syncClientMetadata();
        cluster.restartNodeByAddress(cluster.getServerList().get(1));
        TimeUnit.MILLISECONDS.sleep(options.getMetadataExpireMillis() * 4);

        Metric first = syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(toLabels(ImmutableMap.of(
                        "project", "foo",
                        "cluster", "cluster_1",
                        "service", "first",
                        "host", "kikimr-001",
                        "sensor", "useTime"
                ))))
                .build())
                .getMetric();

        Metric noise = syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(toLabels(ImmutableMap.of(
                        "project", "test",
                        "cluster", "test",
                        "service", "test",
                        "host", "kikirm-002",
                        "sensor", "useTime"
                ))))
                .build())
                .getMetric();


        FindResponse response = syncFind(FindRequest.newBuilder()
                .addSelectors(selector("project", "foo"))
                .addSelectors(selector("cluster", "cluster_1"))
                .build());

        assertThat(response.getMetricsList(), allOf(hasItem(first), iterableWithSize(1)));
    }

    @Test
    public void findAndDelete() throws Exception {
        cluster.addShard(new ShardKey("solomon", "solomon", "misc"));
        cluster.addShard(new ShardKey("test", "test", "test"));
        syncClientMetadata();


        Metric first = syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(toLabels(ImmutableMap.of(
                        "project", "solomon",
                        "cluster", "solomon",
                        "service", "misc",
                        "host", "solomon-test-001",
                        "sensor", "useTime"
                ))))
                .build())
                .getMetric();

        Metric second = syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(toLabels(ImmutableMap.of(
                        "project", "solomon",
                        "cluster", "solomon",
                        "service", "misc",
                        "host", "solomon-test-002",
                        "sensor", "useTime"
                ))))
                .build())
                .getMetric();

        Metric noise = syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(toLabels(ImmutableMap.of(
                        "project", "test",
                        "cluster", "test",
                        "service", "test",
                        "host", "ya",
                        "sensor", "useTime"
                ))))
                .build())
                .getMetric();

        DeleteManyResponse responseDelete = syncDeleteMany(DeleteManyRequest.newBuilder()
                .addAllMetrics(syncFind(FindRequest.newBuilder()
                        .addSelectors(selector("project", "solomon"))
                        .addSelectors(selector("host", "solomon-test-002"))
                        .build())
                        .getMetricsList())
                .build());

        assertThat(responseDelete.getMetricsList(), allOf(hasItem(second), iterableWithSize(1)));

        DeleteManyResponse response = syncDeleteMany(DeleteManyRequest.newBuilder()
                .addAllMetrics(syncFind(FindRequest.newBuilder()
                        .addSelectors(selector("project", "solomon"))
                        .addSelectors(selector("host", "solomon-test-*"))
                        .build())
                        .getMetricsList())
                .build());

        assertThat(response.getMetricsList(), allOf(hasItem(first), iterableWithSize(1)));
    }

    @Test
    public void labelValuesInsideShard() throws Exception {
        cluster.addShard(new ShardKey("junk", "foo", "bar"));
        cluster.addShard(new ShardKey("test", "test", "test"));
        syncClientMetadata();

        syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(toLabels(ImmutableMap.of(
                        "project", "junk",
                        "cluster", "foo",
                        "service", "bar",
                        "host", "solomon-test-001",
                        "sensor", "useTime"
                ))))
                .build());

        syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(toLabels(ImmutableMap.of(
                        "project", "junk",
                        "cluster", "foo",
                        "service", "bar",
                        "host", "solomon-test-002",
                        "sensor", "useTime"
                ))))
                .build());

        // Noise
        syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "test",
                "cluster", "test",
                "service", "test",
                "host", "ya",
                "sensor", "useTime"
            ))))
            .build());


        TLabelValuesResponse response = syncLabelValues(TLabelValuesRequest.newBuilder()
                .addSelectors(selector("project", "junk"))
                .addSelectors(selector("cluster", "foo"))
                .addSelectors(selector("service", "bar"))
                .addSelectors(selector("sensor", "useTime"))
                .addLabels("host")
                .build());

        assertThat(response.getMetricCount(), equalTo(2));
        assertThat(response.getValuesCount(), equalTo(1));

        TLabelValues hostValues = response.getValues(0);
        assertThat(hostValues.getValuesList(),
                allOf(
                        iterableWithSize(2),
                        hasItems("solomon-test-001"),
                        hasItem("solomon-test-002")
                )
        );
        assertThat(hostValues.getAbsent(), equalTo(false));
        assertThat(hostValues.getMetricCount(), equalTo(2));
        assertThat(hostValues.getTruncated(), equalTo(false));
    }

    @Test
    public void reloadClusterIfShardNotFound() {
        ShardKey movedShard = new ShardKey("junk", "foo", "bar");
        cluster.addShard(movedShard);
        cluster.addShard(new ShardKey("test", "test", "test"));
        syncClientMetadata();

        syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "junk",
                "cluster", "foo",
                "service", "bar",
                "host", "solomon-test-001",
                "sensor", "useTime"
            ))))
            .build());

        syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "junk",
                "cluster", "foo",
                "service", "bar",
                "host", "solomon-test-002",
                "sensor", "useTime"
            ))))
            .build());

        // Noise
        syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "test",
                "cluster", "test",
                "service", "test",
                "host", "ya",
                "sensor", "useTime"
            ))))
            .build());

        cluster.iterate(ThreadLocalRandom.current().nextInt(10));
        cluster.moveShard(movedShard);

        TLabelValuesResponse response = syncLabelValues(TLabelValuesRequest.newBuilder()
            .addSelectors(selector("project", "junk"))
            .addSelectors(selector("cluster", "foo"))
            .addSelectors(selector("service", "bar"))
            .addLabels("host")
            .build());

        assertThat(response.getMetricCount(), equalTo(2));
        assertThat(response.getValuesCount(), equalTo(1));
        TLabelValues hostValues = response.getValues(0);
        assertThat(hostValues.getValuesList(),
            allOf(
                iterableWithSize(2),
                hasItems("solomon-test-001"),
                hasItem("solomon-test-002")
            )
        );
        assertThat(hostValues.getAbsent(), equalTo(false));
        assertThat(hostValues.getMetricCount(), equalTo(2));
        assertThat(hostValues.getTruncated(), equalTo(false));
    }

    @Test
    public void labelValuesInsideShardAllLabelsWithLimitAndTextSearch() throws Exception {
        cluster.addShard(new ShardKey("junk", "foo", "bar"));
        syncClientMetadata();

        syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "junk",
                "cluster", "foo",
                "service", "bar",
                "host", "solomon-test-001",
                "sensor", "useTime"
            ))))
            .build());

        syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "junk",
                "cluster", "foo",
                "service", "bar",
                "host", "kikimr-test-002",
                "sensor", "solomon"
            ))))
            .build());

        syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "junk",
                "cluster", "foo",
                "service", "bar",
                "host", "solomon-test-003"
            ))))
            .build());

        TLabelValuesResponse response = syncLabelValues(TLabelValuesRequest.newBuilder()
            .addSelectors(selector("project", "junk"))
            .addSelectors(selector("cluster", "foo"))
            .addSelectors(selector("service", "bar"))
            .setLimit(2)
            .setTextSearch("solomon")
            .build());

        assertThat(response.getMetricCount(), equalTo(3));

        Map<String, TLabelValues> groups = response
            .getValuesList()
            .stream()
            .collect(Collectors.toMap(TLabelValues::getName, Function.identity()));

        TLabelValues hostValues = groups.get("host");
        assertThat(hostValues.getValuesList(), iterableWithSize(2));
        assertThat(hostValues.getAbsent(), equalTo(false));
        assertThat(hostValues.getMetricCount(), equalTo(3));
        assertThat(hostValues.getTruncated(), equalTo(false));

        TLabelValues metricValues = groups.get("sensor");
        assertThat(metricValues.getValuesList(), iterableWithSize(1));
        assertThat(metricValues.getAbsent(), equalTo(true));
        assertThat(metricValues.getMetricCount(), equalTo(2));
        assertThat(metricValues.getTruncated(), equalTo(false));
    }

    @Test
    public void labelValuesInsideShardParticularLabelWithLimit() throws Exception {
        cluster.addShard(new ShardKey("junk", "foo", "bar"));
        cluster.addShard(new ShardKey("test", "test", "test"));
        syncClientMetadata();

        syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "junk",
                "cluster", "foo",
                "service", "bar",
                "host", "solomon-test-001",
                "sensor", "useTime"
            ))))
            .build());

        syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "junk",
                "cluster", "foo",
                "service", "bar",
                "host", "solomon-test-002",
                "sensor", "useTime"
            ))))
            .build());

        // Noise
        syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "test",
                "cluster", "test",
                "service", "test",
                "host", "ya",
                "sensor", "useTime"
            ))))
            .build());


        TLabelValuesResponse response = syncLabelValues(TLabelValuesRequest.newBuilder()
            .addSelectors(selector("project", "junk"))
            .addSelectors(selector("cluster", "foo"))
            .addSelectors(selector("service", "bar"))
            .addSelectors(selector("sensor", "useTime"))
            .addLabels("host")
            .setLimit(1)
            .build());

        assertThat(response.getMetricCount(), equalTo(2));
        assertThat(response.getValuesCount(), equalTo(1));

        TLabelValues hostValues = response.getValues(0);
        assertThat(hostValues.getValuesList(), iterableWithSize(1));
        assertThat(hostValues.getAbsent(), equalTo(false));
        assertThat(hostValues.getMetricCount(), equalTo(2));
        assertThat(hostValues.getTruncated(), equalTo(true));
    }

    @Test
    public void labelValuesInsideShardParticularLabelWithLimitAndTextSearch() throws Exception {
        cluster.addShard(new ShardKey("junk", "foo", "bar"));
        syncClientMetadata();

        syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "junk",
                "cluster", "foo",
                "service", "bar",
                "host", "solomon-test-001",
                "sensor", "useTime"
            ))))
            .build());

        syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "junk",
                "cluster", "foo",
                "service", "bar",
                "host", "solomon-test-002",
                "sensor", "useTime"
            ))))
            .build());

        syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "junk",
                "cluster", "foo",
                "service", "bar",
                "host", "solomon-pre-003",
                "sensor", "useTime"
            ))))
            .build());

        TLabelValuesResponse response = syncLabelValues(TLabelValuesRequest.newBuilder()
            .addSelectors(selector("project", "junk"))
            .addSelectors(selector("cluster", "foo"))
            .addSelectors(selector("service", "bar"))
            .addSelectors(selector("sensor", "useTime"))
            .addLabels("host")
            .setLimit(1)
            .setTextSearch("SOLOMON-TEST")
            .build());

        assertThat(response.getMetricCount(), equalTo(3));
        assertThat(response.getValuesCount(), equalTo(1));

        TLabelValues hostValues = response.getValues(0);
        assertThat(hostValues.getValuesList(), iterableWithSize(1));
        assertThat(hostValues.getValuesList(), hasItem(containsString("solomon-test-00")));
        assertThat(hostValues.getAbsent(), equalTo(false));
        assertThat(hostValues.getMetricCount(), equalTo(3));
        assertThat(hostValues.getTruncated(), equalTo(true));
    }

    @Test
    public void labelValuesInsideShardParticularLabelWithLimitAndTextSearchFoundInLabelKey() throws Exception {
        cluster.addShard(new ShardKey("junk", "foo", "bar"));
        cluster.addShard(new ShardKey("test", "test", "test"));
        syncClientMetadata();

        syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "junk",
                "cluster", "foo",
                "service", "bar",
                "host", "solomon-test-001",
                "sensor", "useTime"
            ))))
            .build());

        syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "junk",
                "cluster", "foo",
                "service", "bar",
                "host", "solomon-test-002",
                "sensor", "useTime"
            ))))
            .build());

        TLabelValuesResponse response = syncLabelValues(TLabelValuesRequest.newBuilder()
            .addSelectors(selector("project", "junk"))
            .addSelectors(selector("cluster", "foo"))
            .addSelectors(selector("service", "bar"))
            .addSelectors(selector("sensor", "useTime"))
            .addLabels("host")
            .setLimit(1)
            .setTextSearch("EnsO")
            .build());

        assertThat(response.getMetricCount(), equalTo(2));
        assertThat(response.getValuesCount(), equalTo(1));

        TLabelValues hostValues = response.getValues(0);
        assertThat(hostValues.getValuesList(), empty());
        assertThat(hostValues.getAbsent(), equalTo(false));
        assertThat(hostValues.getMetricCount(), equalTo(2));
        assertThat(hostValues.getTruncated(), equalTo(false));
    }

    @Test
    public void labelValuesCrossShard() throws Exception {
        cluster.addShard(new ShardKey("junk", "man", "test"));
        cluster.addShard(new ShardKey("junk", "sas", "test"));
        syncClientMetadata();

        syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(toLabels(ImmutableMap.of(
                        "project", "junk",
                        "cluster", "man",
                        "service", "test",
                        "host", "man-001",
                        "sensor", "useTime"
                ))))
                .build());

        syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(toLabels(ImmutableMap.of(
                        "project", "junk",
                        "cluster", "man",
                        "service", "test",
                        "host", "man-002",
                        "sensor", "useTime"
                ))))
                .build());

        syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(toLabels(ImmutableMap.of(
                        "project", "junk",
                        "cluster", "sas",
                        "service", "test",
                        "host", "sas-001",
                        "sensor", "useTime"
                ))))
                .build());

        TLabelValuesResponse response = syncLabelValues(TLabelValuesRequest.newBuilder()
                .addSelectors(selector("project", "junk"))
                .addSelectors(selector("cluster", "*"))
                .addSelectors(selector("service", "test"))
                .addSelectors(selector("sensor", "useTime"))
                .addLabels("host")
                .build());

        assertThat(response.getMetricCount(), equalTo(3));
        assertThat(response.getValuesCount(), equalTo(1));

        TLabelValues hostValues = response.getValues(0);
        assertThat(hostValues.getValuesList(),
                allOf(
                        iterableWithSize(3),
                        hasItems("man-001"),
                        hasItem("man-002"),
                        hasItem("sas-001")
                )
        );

        assertThat(hostValues.getAbsent(), equalTo(false));
        assertThat(hostValues.getMetricCount(), equalTo(3));
        assertThat(hostValues.getTruncated(), equalTo(false));
    }

    @Test
    public void labelValuesCrossShardWithTextSearch() throws Exception {
        cluster.addShard(new ShardKey("junk", "man", "pre"));
        cluster.addShard(new ShardKey("junk", "sas", "pre"));
        syncClientMetadata();

        syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "junk",
                "cluster", "man",
                "service", "pre",
                "host", "Test_Host",
                "sensor", "useTime"
            ))))
            .build());

        syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "junk",
                "cluster", "man",
                "service", "pre",
                "testLabel", "testValue",
                "sensor", "useTime"
            ))))
            .build());

        syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "junk",
                "cluster", "sas",
                "service", "pre",
                "TesT_Label", "VALUE_00",
                "sensor", "useTime"
            ))))
            .build());

        // noise
        syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "junk",
                "cluster", "sas",
                "service", "pre",
                "host", "sas-001",
                "sensor", "downTime"
            ))))
            .build());

        TLabelValuesResponse response = syncLabelValues(TLabelValuesRequest.newBuilder()
            .addSelectors(selector("project", "junk"))
            .addSelectors(selector("cluster", "*"))
            .addSelectors(selector("service", "pre"))
            .addSelectors(selector("sensor", "useTime"))
            .setTextSearch("test")
            .build());

        Map<String, TLabelValues> groups = response
            .getValuesList()
            .stream()
            .collect(Collectors.toMap(TLabelValues::getName, Function.identity()));

        assertThat(response.getMetricCount(), equalTo(3));

        TLabelValues hostValues = groups.get("host");
        assertThat(hostValues.getValuesList(), iterableWithSize(1));
        assertThat(hostValues.getAbsent(), equalTo(true));
        assertThat(hostValues.getMetricCount(), equalTo(1));
        assertThat(hostValues.getTruncated(), equalTo(false));


        TLabelValues testLabelValues = groups.get("testLabel");
        assertThat(testLabelValues.getValuesList(), iterableWithSize(1));
        assertThat(testLabelValues.getAbsent(), equalTo(true));
        assertThat(testLabelValues.getMetricCount(), equalTo(1));
        assertThat(testLabelValues.getTruncated(), equalTo(false));


        TLabelValues secondTestLabelValues = groups.get("TesT_Label");
        assertThat(secondTestLabelValues.getValuesList(), iterableWithSize(1));
        assertThat(secondTestLabelValues.getAbsent(), equalTo(true));
        assertThat(secondTestLabelValues.getMetricCount(), equalTo(1));
        assertThat(secondTestLabelValues.getTruncated(), equalTo(false));
    }

    @Test
    public void labelValuesCrossShardWithValidationFilter() throws Exception {
        cluster.addShard(new ShardKey("junk", "man", "pre"));
        cluster.addShard(new ShardKey("junk", "man", "invalid\npre"));
        syncClientMetadata();

        syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "junk",
                "cluster", "man",
                "service", "pre",
                "host", "cluster",
                "sensor", "useTime"
            ))))
            .build());

        syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "junk",
                "cluster", "man",
                "service", "pre",
                "test-label", "test\nvalue",
                "sensor", "useTime"
            ))))
            .build());

        syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "junk",
                "cluster", "man",
                "service", "invalid\npre",
                "test-label", "VALUE_00",
                "sensor", "useTime"
            ))))
            .build());

        TLabelValuesResponse response = syncLabelValues(TLabelValuesRequest.newBuilder()
            .addSelectors(selector("project", "junk"))
            .addSelectors(selector("cluster", "man"))
            .addSelectors(selector("service", "*"))
            .addSelectors(selector("sensor", "useTime"))
            .setValidationFilter(LabelValidationFilter.INVALID_ONLY)
            .build());

        Map<String, TLabelValues> groups = response
            .getValuesList()
            .stream()
            .collect(Collectors.toMap(TLabelValues::getName, Function.identity()));

        assertThat(response.getMetricCount(), equalTo(3));

        TLabelValues clusterValues = groups.get("cluster");
        assertThat(clusterValues.getValuesList(), emptyIterable());

        TLabelValues serviceValues = groups.get("service");
        assertThat(serviceValues.getValuesList(), allOf(iterableWithSize(1), hasItem("invalid\npre")));

        TLabelValues hostValues = groups.get("host");
        assertThat(hostValues.getValuesList(), emptyIterable());

        TLabelValues testLabelValues = groups.get("test-label");
        assertThat(testLabelValues.getValuesList(), allOf(iterableWithSize(1), hasItem("test\nvalue")));
        assertThat(testLabelValues.getMetricCount(), equalTo(2));
    }

    @Test
    public void labelValuesCrossShardWithLimit() throws Exception {
        cluster.addShard(new ShardKey("junk", "man", "test"));
        cluster.addShard(new ShardKey("junk", "sas", "test"));
        syncClientMetadata();

        syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "junk",
                "cluster", "man",
                "service", "test",
                "host", "man-001",
                "sensor", "useTime"
            ))))
            .build());

        syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "junk",
                "cluster", "sas",
                "service", "test",
                "host", "sas-001",
                "sensor", "useTime"
            ))))
            .build());

        syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "junk",
                "cluster", "sas",
                "service", "test",
                "host", "sas-002",
                "sensor", "useTime"
            ))))
            .build());

        TLabelValuesResponse response = syncLabelValues(TLabelValuesRequest.newBuilder()
            .addSelectors(selector("project", "junk"))
            .addSelectors(selector("cluster", "*"))
            .addSelectors(selector("service", "test"))
            .addSelectors(selector("sensor", "useTime"))
            .addLabels("host")
            .setLimit(2)
            .build());

        assertThat(response.getStatus(), equalTo(EMetabaseStatusCode.OK));

        assertThat(response.getMetricCount(), equalTo(3));
        assertThat(response.getValuesCount(), equalTo(1));

        TLabelValues hostValues = response.getValues(0);
        assertThat(hostValues.getValuesList(), iterableWithSize(2));
        assertThat(hostValues.getAbsent(), equalTo(false));
        assertThat(hostValues.getMetricCount(), equalTo(3));
        assertThat(hostValues.getTruncated(), equalTo(true));
    }

    @Test
    public void labelNamesInsideShard() throws Exception {
        cluster.addShard(new ShardKey("junk", "foo", "bar"));
        cluster.addShard(new ShardKey("test", "test", "test"));
        syncClientMetadata();

        syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "junk",
                "cluster", "foo",
                "service", "bar",
                "host", "solomon-test-001",
                "sensor", "useTime"
            ))))
            .build());

        syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "junk",
                "cluster", "foo",
                "service", "bar",
                "host", "solomon-test-002",
                "sensor", "useTime"
            ))))
            .build());

        syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "junk",
                "cluster", "foo",
                "service", "bar",
                "dc", "man",
                "path", "useTime"
            ))))
            .build());

        // noise
        syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "test",
                "cluster", "test",
                "service", "test",
                "host", "ya",
                "sensor", "useTime"
            ))))
            .build());

        syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "test",
                "cluster", "test",
                "service", "test",
                "host", "cluster",
                "sensor", "useTime"
            ))))
            .build());

        TLabelNamesResponse response = syncLabelNames(TLabelNamesRequest.newBuilder()
            .addSelectors(selector("project", "junk"))
            .addSelectors(selector("cluster", "foo"))
            .addSelectors(selector("service", "bar"))
            .addSelectors(selector("sensor", "useTime"))
            .build());

        assertThat(response.getNamesCount(), equalTo(1));

        assertThat(response.getNames(0), equalTo("host"));
    }

    @Test
    public void labelNamesCrossShard() throws Exception {
        cluster.addShard(new ShardKey("junk", "man", "test"));
        cluster.addShard(new ShardKey("junk", "sas", "test"));
        syncClientMetadata();

        syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "junk",
                "cluster", "man",
                "service", "test",
                "dc", "man",
                "sensor", "useTime"
            ))))
            .build());

        syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "junk",
                "cluster", "man",
                "service", "test",
                "host", "man-002",
                "sensor", "useTime"
            ))))
            .build());

        syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "junk",
                "cluster", "sas",
                "service", "test",
                "host", "sas-001",
                "sensor", "useTime"
            ))))
            .build());

        TLabelNamesResponse response = syncLabelNames(TLabelNamesRequest.newBuilder()
            .addSelectors(selector("project", "junk"))
            .addSelectors(selector("cluster", "*"))
            .addSelectors(selector("sensor", "useTime"))
            .build());

        assertThat(response.getNamesList(),
            allOf(
                iterableWithSize(3),
                hasItems("service"),
                hasItem("host"),
                hasItem("dc")
            )
        );
    }

    @Test
    public void uniqueLabels() throws Exception {
        cluster.addShard(new ShardKey("solomon", "solomon", "misc"));
        cluster.addShard(new ShardKey("test", "test", "test"));
        syncClientMetadata();

        ImmutableMap<String, String> labels = ImmutableMap.of(
                "project", "solomon",
                "cluster", "solomon",
                "service", "misc",
                "sensor", "freeDiskSpace"
        );

        Metric first = newMetric(toLabels(
                ImmutableMap.<String, String>builder()
                        .putAll(labels)
                        .put("host", "solomon-test-001")
                        .put("disk", "/dev/sda1")
                        .build()
        ));

        Metric second = newMetric(toLabels(
                ImmutableMap.<String, String>builder()
                        .putAll(labels)
                        .put("host", "solomon-test-002")
                        .put("disk", "/dev/sda1")
                        .build()
        ));

        Metric noise = newMetric(toLabels(ImmutableMap.of(
                "project", "test",
                "cluster", "test",
                "service", "test",
                "host", "ya",
                "sensor", "useTime"
        )));

        syncCreateOne(CreateOneRequest.newBuilder().setMetric(first).build());
        syncCreateOne(CreateOneRequest.newBuilder().setMetric(second).build());
        syncCreateOne(CreateOneRequest.newBuilder().setMetric(noise).build());

        TUniqueLabelsRequest request = TUniqueLabelsRequest.newBuilder()
                .addAllNames(Arrays.asList("disk", "host"))
                .addSelectors(selector("project", "solomon"))
                .addSelectors(selector("cluster", "solomon"))
                .addSelectors(selector("service", "misc"))
                .addSelectors(selector("sensor", "freeDiskSpace"))
                .build();

        List<Labels> result = syncUniqueLabels(request).getLabelListsList();
        assertThat(result,
                allOf(
                        iterableWithSize(2),
                        hasItem(toLabelList(ImmutableMap.of(
                                "host", "solomon-test-001",
                                "disk", "/dev/sda1"
                        ))),
                        hasItem(toLabelList(ImmutableMap.of(
                                "host", "solomon-test-002",
                                "disk", "/dev/sda1"
                        )))
                )
        );
    }

    @Test
    public void moveShardFromUnavailableHost() throws InterruptedException {
        ShardKey movedShard = new ShardKey("junk", "foo", "bar");
        cluster.addShard(movedShard);
        cluster.addShard(new ShardKey("test", "test", "test"));
        syncClientMetadata();

        syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "junk",
                "cluster", "foo",
                "service", "bar",
                "host", "solomon-test-001",
                "sensor", "useTime"
            ))))
            .build());

        // Noise
        syncCreateOne(CreateOneRequest.newBuilder()
            .setMetric(newMetric(toLabels(ImmutableMap.of(
                "project", "test",
                "cluster", "test",
                "service", "test",
                "host", "ya",
                "sensor", "useTime"
            ))))
            .build());

        cluster.iterate(ThreadLocalRandom.current().nextInt(10));
        cluster.forceStopNodeByAddress(cluster.getAddressByShard(movedShard));
        cluster.moveShard(movedShard);
        syncClientMetadata();

        TLabelValuesResponse response = syncLabelValues(TLabelValuesRequest.newBuilder()
            .addSelectors(selector("project", "junk"))
            .addSelectors(selector("cluster", "foo"))
            .addSelectors(selector("service", "bar"))
            .addLabels("host")
            .build());

        assertThat(response.getMetricCount(), equalTo(1));
        assertThat(response.getValuesCount(), equalTo(1));
        TLabelValues hostValues = response.getValues(0);
        assertThat(hostValues.getValuesList(),
            allOf(
                iterableWithSize(1),
                hasItems("solomon-test-001")
            )
        );
        assertThat(hostValues.getAbsent(), equalTo(false));
        assertThat(hostValues.getMetricCount(), equalTo(1));
        assertThat(hostValues.getTruncated(), equalTo(false));
    }

    @Test
    public void moveShardFromUnavailableHostCrossShard() throws InterruptedException {
        ShardKey movedShard = new ShardKey("junk", "foo", "bar");
        cluster.addShard(movedShard);
        cluster.addShard(new ShardKey("test", "test", "test"));
        syncClientMetadata();

        syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(toLabels(ImmutableMap.of(
                        "project", "junk",
                        "cluster", "foo",
                        "service", "bar",
                        "host", "solomon-test-001",
                        "sensor", "useTime"
                ))))
                .build());

        // Noise
        syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(toLabels(ImmutableMap.of(
                        "project", "test",
                        "cluster", "test",
                        "service", "test",
                        "host", "ya",
                        "sensor", "useTime"
                ))))
                .build());

        cluster.iterate(ThreadLocalRandom.current().nextInt(10));
        cluster.forceStopNodeByAddress(cluster.getAddressByShard(movedShard));
        cluster.moveShard(movedShard);
        syncClientMetadata();

        syncFind(FindRequest.newBuilder()
                .addSelectors(selector("project", "junk"))
                .addSelectors(selector("cluster", "foo*"))
                .addSelectors(selector("service", "bar"))
                .build());
    }

    @Test
    public void resolveLogsShardNotFound() {
        cluster.addShard(new ShardKey("junk", "foo", "bar"));
        syncClientMetadata();

        var response = client.resolveLogs(TResolveLogsRequest.newBuilder()
            .setNumId(42)
            .addUnresolvedLogMeta(ByteString.copyFromUtf8("log"))
            .build())
            .join();

        assertEquals(EMetabaseStatusCode.SHARD_NOT_FOUND, response.getStatus());
    }

    @Test
    public void resolveLogsAndWrite() {
        var shard = new ShardKey("junk", "foo", "bar");
        cluster.addShard(shard);
        syncClientMetadata();

        var response = client.resolveLogs(TResolveLogsRequest.newBuilder()
            .setNumId(shard.hashCode())
            .addUnresolvedLogMeta(ByteString.copyFromUtf8("hi"))
            .build())
            .join();

        assertNotEquals(EMetabaseStatusCode.SHARD_NOT_FOUND, response.getStatus());
    }

    @Test
    public void allowCreateNewMetrics() {
        var shard = new ShardKey("junk", "foo", "bar");
        int numId = shard.hashCode();
        assertFalse("If shard unknown, no possible create new metrics in it", client.isAllowCreateNew(numId));

        cluster.addShard(shard);
        syncClientMetadata();
        assertTrue("In shard without quota overflow possible create new metrics", client.isAllowCreateNew(numId));

        cluster.setMetricsLimit(shard, 1);
        var metric = syncCreateOne(CreateOneRequest.newBuilder()
                .setMetric(newMetric(
                        toLabels(ImmutableMap.of(
                                "project", "junk",
                                "cluster", "foo",
                                "service", "bar",
                                "name", "alice"
                        ))))
                .build())
                .getMetric();

        syncClientMetadata();
        assertFalse("Limit already reached, not able create new metrics", client.isAllowCreateNew(numId));

        syncDeleteMany(DeleteManyRequest.newBuilder()
                .addMetrics(metric)
                .build());

        syncClientMetadata();
        assertTrue("After delete metric, possible to more", client.isAllowCreateNew(numId));
    }

    private CreateOneResponse syncCreateOne(CreateOneRequest request) {
        return syncSuccessRun(request,
                client::createOne,
                CreateOneResponse::getStatus,
                CreateOneResponse::getStatusMessage
        );
    }

    private CreateManyResponse syncCreateManyOne(CreateManyRequest request) {
        return syncSuccessRun(request,
                client::createMany,
                CreateManyResponse::getStatus,
                CreateManyResponse::getStatusMessage
        );
    }

    private <Request, Response> Response syncSuccessRun(Request request,
                                                        Function<Request, CompletableFuture<Response>> fn,
                                                        Function<Response, EMetabaseStatusCode> status,
                                                        Function<Response, String> message) {
        Response response = fn.apply(request).join();

        if (status.apply(response) != EMetabaseStatusCode.OK) {
            throw new IllegalStateException(status.apply(response) + ": " + message.apply(response));
        }

        return response;
    }

    private FindResponse syncFind(FindRequest request) {
        return syncSuccessRun(request,
                client::find,
                FindResponse::getStatus,
                FindResponse::getStatusMessage
        );
    }

    private DeleteManyResponse syncDeleteMany(DeleteManyRequest request) {
        return syncSuccessRun(request,
                client::deleteMany,
                DeleteManyResponse::getStatus,
                DeleteManyResponse::getStatusMessage);
    }

    private TLabelValuesResponse syncLabelValues(TLabelValuesRequest request) {
        return syncSuccessRun(request,
                client::labelValues,
                TLabelValuesResponse::getStatus,
                TLabelValuesResponse::getStatusMessage);
    }

    private TLabelNamesResponse syncLabelNames(TLabelNamesRequest request) {
        return syncSuccessRun(request,
            client::labelNames,
            TLabelNamesResponse::getStatus,
            TLabelNamesResponse::getStatusMessage);
    }

    private TUniqueLabelsResponse syncUniqueLabels(TUniqueLabelsRequest request) {
        return syncSuccessRun(request,
                client::uniqueLabels,
                TUniqueLabelsResponse::getStatus,
                TUniqueLabelsResponse::getStatusMessage);
    }

    private Metric newMetric(List<Label> labels) {
        return Metric.newBuilder()
                .addAllLabels(labels)
                .build();
    }

    private void syncClientMetadata() {
        client.forceUpdateClusterMetaData().join();
    }
}
