package ru.yandex.solomon.alert.rule.stubs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.labels.query.Selectors;
import ru.yandex.solomon.labels.shard.ShardKey;
import ru.yandex.solomon.metabase.api.protobuf.EMetabaseStatusCode;
import ru.yandex.solomon.metrics.client.FindRequest;
import ru.yandex.solomon.metrics.client.FindResponse;
import ru.yandex.solomon.metrics.client.LabelNamesRequest;
import ru.yandex.solomon.metrics.client.LabelNamesResponse;
import ru.yandex.solomon.metrics.client.LabelValuesRequest;
import ru.yandex.solomon.metrics.client.LabelValuesResponse;
import ru.yandex.solomon.metrics.client.MetabaseStatus;
import ru.yandex.solomon.metrics.client.MetricNamesRequest;
import ru.yandex.solomon.metrics.client.MetricNamesResponse;
import ru.yandex.solomon.metrics.client.MetricsClient;
import ru.yandex.solomon.metrics.client.ReadManyRequest;
import ru.yandex.solomon.metrics.client.ReadManyResponse;
import ru.yandex.solomon.metrics.client.ReadRequest;
import ru.yandex.solomon.metrics.client.ReadResponse;
import ru.yandex.solomon.metrics.client.ResolveManyRequest;
import ru.yandex.solomon.metrics.client.ResolveManyResponse;
import ru.yandex.solomon.metrics.client.ResolveManyWithNameRequest;
import ru.yandex.solomon.metrics.client.ResolveOneRequest;
import ru.yandex.solomon.metrics.client.ResolveOneResponse;
import ru.yandex.solomon.metrics.client.ResolveOneWithNameRequest;
import ru.yandex.solomon.metrics.client.UniqueLabelsRequest;
import ru.yandex.solomon.metrics.client.UniqueLabelsResponse;
import ru.yandex.solomon.metrics.client.combined.FindAndReadManyRequest;
import ru.yandex.solomon.metrics.client.combined.FindAndReadManyResponse;
import ru.yandex.solomon.selfmon.AvailabilityStatus;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class DcMetricsClientFindOnlyStub implements MetricsClient {
    private final String destination;
    private final List<ShardKey> shards = new ArrayList<>();
    private final AtomicInteger responseIndex = new AtomicInteger(0);
    private final ArrayList<CompletableFuture<FindResponse>> responses = new ArrayList<>();

    public DcMetricsClientFindOnlyStub(String destination) {
        this.destination = destination;
    }

    public void addResponse(CompletableFuture<FindResponse> response) {
        responses.add(response);
    }

    public void addResponse(FindResponse response) {
        addResponse(CompletableFuture.completedFuture(response));
    }

    public int requestCount() {
        return responseIndex.get();
    }

    @Override
    public CompletableFuture<FindResponse> find(FindRequest request) {
        int idx = responseIndex.getAndIncrement();
        return idx < responses.size() ? responses.get(idx) : CompletableFuture.completedFuture(
                new FindResponse(MetabaseStatus.fromCode(
                    EMetabaseStatusCode.RESOURCE_EXHAUSTED, "no predefined responses left")));
    }

    public void addShard(ShardKey shardKey) {
        shards.add(shardKey);
    }

    @Override
    public CompletableFuture<ResolveOneResponse> resolveOne(ResolveOneRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<ResolveOneResponse> resolveOneWithName(ResolveOneWithNameRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<ResolveManyResponse> resolveMany(ResolveManyRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<ResolveManyResponse> resolveManyWithName(ResolveManyWithNameRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<MetricNamesResponse> metricNames(MetricNamesRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<LabelNamesResponse> labelNames(LabelNamesRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<LabelValuesResponse> labelValues(LabelValuesRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<UniqueLabelsResponse> uniqueLabels(UniqueLabelsRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<ReadResponse> read(ReadRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<ReadManyResponse> readMany(ReadManyRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<FindAndReadManyResponse> findAndReadMany(FindAndReadManyRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AvailabilityStatus getMetabaseAvailability() {
        throw new UnsupportedOperationException();
    }

    @Override
    public AvailabilityStatus getStockpileAvailability() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<Labels> metabaseShards(String destination, Selectors selector) {
        return shards.stream().filter(selector::match).map(ShardKey::toLabels);
    }

    @Override
    public String getStockpileHostForShardId(String destination, int shardId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getDestinations() {
        return List.of(destination);
    }

    @Override
    public void close() {
    }
}
