package ru.yandex.metabase.client.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.Throwables;
import io.grpc.stub.StreamObserver;

import ru.yandex.monitoring.metabase.MetabaseServiceGrpc;
import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.monlib.metrics.labels.validate.LabelValidationFilter;
import ru.yandex.solomon.labels.protobuf.LabelConverter;
import ru.yandex.solomon.labels.protobuf.LabelSelectorConverter;
import ru.yandex.solomon.labels.protobuf.LabelValidationFilterConverter;
import ru.yandex.solomon.labels.query.Selectors;
import ru.yandex.solomon.labels.query.ShardSelectors;
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
import ru.yandex.solomon.metabase.api.protobuf.TLabelValuesRequest;
import ru.yandex.solomon.metabase.api.protobuf.TLabelValuesResponse;
import ru.yandex.solomon.metabase.api.protobuf.TServerStatusRequest;
import ru.yandex.solomon.metabase.api.protobuf.TServerStatusResponse;
import ru.yandex.solomon.metabase.api.protobuf.TShardStatus;
import ru.yandex.solomon.metabase.api.protobuf.TSliceOptions;
import ru.yandex.solomon.metabase.api.protobuf.TUniqueLabelsRequest;
import ru.yandex.solomon.metabase.api.protobuf.TUniqueLabelsResponse;
import ru.yandex.solomon.model.protobuf.Label;
import ru.yandex.solomon.model.protobuf.MetricId;
import ru.yandex.solomon.util.collection.Slicer;
import ru.yandex.solomon.util.labelStats.LabelStatsConverter;
import ru.yandex.solomon.util.labelStats.LabelValuesStats;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

/**
 * @author Vladimir Gordiychuk
 */
@ParametersAreNonnullByDefault
public class InMemoryMetabaseService extends MetabaseServiceGrpc.MetabaseServiceImplBase {
    private final ConcurrentMap<ShardKey, InMemoryMetabaseShard> shards = new ConcurrentHashMap<>();

    InMemoryMetabaseService() {
    }

    void addShard(ShardKey shardKey) {
        addShard(shardKey, new InMemoryMetabaseShard());
    }

    void addShard(ShardKey shardKey, InMemoryMetabaseShard shard) {
        InMemoryMetabaseShard old = shards.putIfAbsent(shardKey, shard);
        if (old != null) {
            throw new IllegalStateException("Shard with key " + shardKey + " already exists");
        }
    }

    void setMetricsLimit(ShardKey key, int limit) {
        shards.get(key).setMetricsLimit(limit);
    }

    InMemoryMetabaseShard getShard(ShardKey shardKey) {
        return shards.get(shardKey);
    }

    void removeShard(ShardKey shardKey) {
        shards.remove(shardKey);
    }

    public boolean hasShard(ShardKey shardKey) {
        return shards.containsKey(shardKey);
    }

    @Override
    public void serverStatus(TServerStatusRequest request, StreamObserver<TServerStatusResponse> responseObserver) {
        responseObserver.onNext(safeServerStatus());
        responseObserver.onCompleted();
    }

    @Nonnull
    private TServerStatusResponse safeServerStatus() {
        try {
            return TServerStatusResponse.newBuilder()
                    .setStatus(EMetabaseStatusCode.OK)
                    .addAllPartitionStatus(shards.entrySet().stream()
                            .map(entry -> TShardStatus.newBuilder()
                                .setNumId(entry.getKey().hashCode())
                                .setShardId(entry.getKey().toUniqueId())
                                .addAllLabels(LabelConverter.labelsToProtoList(entry.getKey().toLabels()))
                                .setMetricCount(entry.getValue().metricsCount())
                                .setMetricLimit(entry.getValue().metricsLimit())
                                .setReady(true)
                                .build())
                            .collect(toList()))
                    .setTotalPartitionCount(shards.size())
                    .setTotalPartitionCountKnown(true)
                    .build();
        } catch (Throwable e) {
            return TServerStatusResponse.newBuilder()
                    .setStatus(classifyError(e))
                    .setStatusMessage(Throwables.getStackTraceAsString(e))
                    .build();
        }
    }

    @Override
    public void createOne(CreateOneRequest request, StreamObserver<CreateOneResponse> responseObserver) {
        responseObserver.onNext(safeCreateOne(request));
        responseObserver.onCompleted();
    }

    @Nonnull
    private CreateOneResponse safeCreateOne(CreateOneRequest request) {
        try {
            return CreateOneResponse.newBuilder()
                    .setStatus(EMetabaseStatusCode.OK)
                    .setMetric(create(request.getMetric()))
                    .build();
        } catch (Throwable e) {
            return CreateOneResponse.newBuilder()
                    .setStatus(classifyError(e))
                    .setStatusMessage(Throwables.getStackTraceAsString(e))
                    .build();
        }
    }

    @Override
    public void createMany(CreateManyRequest request, StreamObserver<CreateManyResponse> responseObserver) {
        responseObserver.onNext(safeCreateMany(request));
        responseObserver.onCompleted();
    }

    @Nonnull
    private CreateManyResponse safeCreateMany(CreateManyRequest request) {
        if (request.getMetricsCount() == 0) {
            return CreateManyResponse.newBuilder()
                    .setStatus(EMetabaseStatusCode.INVALID_REQUEST)
                    .setStatusMessage("Request does not contain metrics to create")
                    .build();
        }

        try {
            int shardId = ThreadLocalRandom.current().nextInt(1, 4096);
            return CreateManyResponse.newBuilder()
                    .setStatus(EMetabaseStatusCode.OK)
                    .addAllMetrics(request.getMetricsList()
                            .stream()
                            .map(metric -> {
                                Metric updatedMetric = fillWithDefaultNewMetric(
                                        shardId, request.getCreatedAtMillis(),
                                        request.getCommonLabelsList(), metric);
                                return create(updatedMetric);
                            })
                            .collect(toList()))
                    .build();
        } catch (Throwable e) {
            return CreateManyResponse.newBuilder()
                    .setStatus(classifyError(e))
                    .setStatusMessage(Throwables.getStackTraceAsString(e))
                    .build();
        }

    }

    @Override
    public void resolveOne(ResolveOneRequest request, StreamObserver<ResolveOneResponse> responseObserver) {
        responseObserver.onNext(safeResolveOne(request));
        responseObserver.onCompleted();
    }

    @Nonnull
    private ResolveOneResponse safeResolveOne(ResolveOneRequest request) {
        try {
            Metric metric = resolve(request.getLabelsList());

            if (metric != null) {
                return ResolveOneResponse.newBuilder()
                        .setStatus(EMetabaseStatusCode.OK)
                        .setMetric(metric)
                        .build();
            }

            return ResolveOneResponse.newBuilder()
                    .setStatus(EMetabaseStatusCode.NOT_FOUND)
                    .build();
        } catch (Throwable e) {
            return ResolveOneResponse.newBuilder()
                    .setStatus(classifyError(e))
                    .setStatusMessage(Throwables.getStackTraceAsString(e))
                    .build();
        }
    }

    @Override
    public void resolveMany(ResolveManyRequest request, StreamObserver<ResolveManyResponse> responseObserver) {
        responseObserver.onNext(safeResolveMany(request));
        responseObserver.onCompleted();
    }

    @Nonnull
    private ResolveManyResponse safeResolveMany(ResolveManyRequest request) {
        if (request.getListLabelsCount() == 0) {
            return ResolveManyResponse.newBuilder()
                    .setStatus(EMetabaseStatusCode.INVALID_REQUEST)
                    .setStatusMessage("Request not contain labels at all")
                    .build();
        }

        try {
            return ResolveManyResponse.newBuilder()
                    .setStatus(EMetabaseStatusCode.OK)
                    .addAllMetrics(request.getListLabelsList()
                            .stream()
                            .map(labelList -> {
                                int size = labelList.getLabelsCount() + request.getCommonLabelsCount();
                                List<Label> labels = new ArrayList<>(size);
                                labels.addAll(labelList.getLabelsList());
                                labels.addAll(request.getCommonLabelsList());
                                return resolve(labels);
                            })
                            .filter(Objects::nonNull)
                            .collect(toList()))
                    .build();
        } catch (Throwable e) {
            return ResolveManyResponse.newBuilder()
                    .setStatus(classifyError(e))
                    .setStatusMessage(Throwables.getStackTraceAsString(e))
                    .build();
        }
    }

    @Override
    public void deleteMany(DeleteManyRequest request, StreamObserver<DeleteManyResponse> responseObserver) {
        responseObserver.onNext(safeDeleteMany(request));
        responseObserver.onCompleted();
    }

    @Nonnull
    private DeleteManyResponse safeDeleteMany(DeleteManyRequest request) {
        if (request.getMetricsCount() == 0) {
            return DeleteManyResponse.newBuilder()
                    .setStatus(EMetabaseStatusCode.INVALID_REQUEST)
                    .setStatusMessage("Request not contain metric identity to delete")
                    .build();
        }

        try {
            return DeleteManyResponse.newBuilder()
                    .setStatus(EMetabaseStatusCode.OK)
                    .addAllMetrics(request.getMetricsList()
                            .stream()
                            .map(metric -> resolveShard(metric).delete(metric))
                            .filter(Objects::nonNull)
                            .collect(toList()))
                    .build();
        } catch (Throwable e) {
            return DeleteManyResponse.newBuilder()
                    .setStatus(classifyError(e))
                    .setStatusMessage(Throwables.getStackTraceAsString(e))
                    .build();
        }
    }

    @Override
    public void find(FindRequest request, StreamObserver<FindResponse> responseObserver) {
        responseObserver.onNext(safeFind(request));
        responseObserver.onCompleted();
    }

    @Nonnull
    private FindResponse safeFind(FindRequest request) {
        try {
            Selectors selector = LabelSelectorConverter.protoToSelectors(request.getSelectorsList());
            Selectors shardSelector = ShardSelectors.onlyShardKey(selector);

            List<Metric> rawMetrics = shards.entrySet()
                .stream()
                .filter(entry -> shardSelector.match(entry.getKey().toLabels()))
                .flatMap(entry -> entry.getValue().find(selector))
                .collect(toList());

            int totalCount = rawMetrics.size();
            TSliceOptions sliceOptions = request.getSliceOptions();

            return FindResponse.newBuilder()
                .setStatus(EMetabaseStatusCode.OK)
                .addAllMetrics(Slicer.slice(rawMetrics, sliceOptions.getOffset(), sliceOptions.getLimit()))
                .setTotalCount(totalCount)
                .build();
        } catch (Throwable e) {
            return FindResponse.newBuilder()
                    .setStatus(classifyError(e))
                    .setStatusMessage(Throwables.getStackTraceAsString(e))
                    .build();
        }
    }

    @Override
    public void labelValues(TLabelValuesRequest request, StreamObserver<TLabelValuesResponse> responseObserver) {
        responseObserver.onNext(safeLabelValues(request));
        responseObserver.onCompleted();
    }

    private TLabelValuesResponse safeLabelValues(TLabelValuesRequest request) {
        try {
            Selectors selector = LabelSelectorConverter.protoToSelectors(request.getSelectorsList());
            Selectors shardSelector = ShardSelectors.onlyShardKey(selector);

            LabelValidationFilter validationFilter =
                LabelValidationFilterConverter.protoToFilter(request.getValidationFilter());

            List<Map.Entry<ShardKey, InMemoryMetabaseShard>> matchedShards = shards.entrySet().stream()
                .filter(entry -> shardSelector.match(entry.getKey().toLabels())).collect(toList());

            LabelValuesStats result =
                matchedShards.stream()
                    .map(entry -> entry.getValue().labelValues(request))
                    .collect(LabelValuesStats::new, LabelValuesStats::combine, LabelValuesStats::combine);
            result.filter(request.getTextSearch());
            result.filter(validationFilter);
            result.limit(request.getLimit());

            if (matchedShards.isEmpty()) {
                return TLabelValuesResponse.newBuilder()
                    .setStatus(EMetabaseStatusCode.SHARD_NOT_FOUND)
                    .build();
            }
            return LabelStatsConverter.toProto(result);
        } catch (Throwable e) {
            return TLabelValuesResponse.newBuilder()
                    .setStatus(classifyError(e))
                    .setStatusMessage(Throwables.getStackTraceAsString(e))
                    .build();
        }
    }

    @Override
    public void labelNames(TLabelNamesRequest request, StreamObserver<TLabelNamesResponse> responseObserver) {
        responseObserver.onNext(safeLabelNames(request));
        responseObserver.onCompleted();
    }

    @Nonnull
    private TLabelNamesResponse safeLabelNames(TLabelNamesRequest request) {
        try {
            Selectors selector = LabelSelectorConverter.protoToSelectors(request.getSelectorsList());
            Selectors shardSelector = ShardSelectors.onlyShardKey(selector);

            return TLabelNamesResponse.newBuilder()
                .setStatus(EMetabaseStatusCode.OK)
                .addAllNames(shards.entrySet()
                    .stream()
                    .filter(entry -> shardSelector.match(entry.getKey().toLabels()))
                    .flatMap(entry -> entry.getValue().find(selector))
                    .flatMap(x -> x.getLabelsList().stream())
                    .map(Label::getKey)
                    .distinct()
                    .filter(name -> !selector.hasKey(name))
                    .collect(toList()))
                .build();
        } catch (Throwable e) {
            return TLabelNamesResponse.newBuilder()
                .setStatus(classifyError(e))
                .setStatusMessage(Throwables.getStackTraceAsString(e))
                .build();
        }
    }

    @Override
    public void uniqueLabels(TUniqueLabelsRequest request, StreamObserver<TUniqueLabelsResponse> responseObserver) {
        responseObserver.onNext(safeUniqueLabels(request));
        responseObserver.onCompleted();
    }

    private TUniqueLabelsResponse safeUniqueLabels(TUniqueLabelsRequest request) {
        try {
            Selectors selector = LabelSelectorConverter.protoToSelectors(request.getSelectorsList());
            Selectors shardSelector = ShardSelectors.onlyShardKey(selector);

            return TUniqueLabelsResponse.newBuilder()
                    .setStatus(EMetabaseStatusCode.OK)
                    .addAllLabelLists(shards.entrySet()
                            .stream()
                            .filter(entry -> shardSelector.match(entry.getKey().toLabels()))
                            .flatMap(entry -> entry.getValue().find(selector))
                            .map(x -> x.getLabelsList().stream()
                                    .filter(label -> request.getNamesList().contains(label.getKey()))
                                    .collect(collectingAndThen(toList(), labels -> {
                                        return ru.yandex.solomon.model.protobuf.Labels.newBuilder()
                                                .addAllLabels(labels).build();
                                    })))
                            .distinct()
                            .collect(toList()))
                    .build();
        } catch (Throwable e) {
            return TUniqueLabelsResponse.newBuilder()
                    .setStatus(classifyError(e))
                    .setStatusMessage(Throwables.getStackTraceAsString(e))
                    .build();
        }
    }

    @Nullable
    private Metric resolve(List<Label> protoLabels) {
        Labels labels = LabelConverter.protoToLabels(protoLabels);

        if (labels.isEmpty()) {
            throw new MetabaseRuntimeException("Label list can't be empty", EMetabaseStatusCode.INVALID_REQUEST);
        }

        return resolveShard(labels).resolve(labels);
    }

    @Nonnull
    private Metric fillWithDefaultNewMetric(int shardId, long createdAtMillis, List<Label> commonLabels, Metric metric) {
        Metric.Builder builder = metric.toBuilder();
        if (builder.getCreatedAtMillis() == 0) {
            builder.setCreatedAtMillis(createdAtMillis);
        }

        MetricId.Builder metricId = builder.getMetricIdBuilder();
        if (metricId.getShardId() == 0) {
            metricId.setShardId(shardId);
        }

        if (metricId.getLocalId() == 0) {
            metricId.setLocalId(ThreadLocalRandom.current().nextLong());
        }

        builder.addAllLabels(commonLabels);
        return builder.build();
    }

    @Nonnull
    private Metric create(Metric metric) {
        return resolveShard(metric)
                .create(metric);
    }

    @Nonnull
    private EMetabaseStatusCode classifyError(Throwable e) {
        if (e instanceof MetabaseRuntimeException) {
            return ((MetabaseRuntimeException) e).getStatusCode();
        }

        return EMetabaseStatusCode.INTERNAL_ERROR;
    }

    @Nonnull
    private InMemoryMetabaseShard resolveShard(Metric metric) {
        Labels labels = LabelConverter.protoToLabels(metric.getLabelsList());
        if (labels.isEmpty()) {
            throw new MetabaseRuntimeException("Label list can't be empty", EMetabaseStatusCode.INVALID_REQUEST);
        }

        return resolveShard(labels);
    }

    @Nonnull
    private InMemoryMetabaseShard resolveShard(Labels labels) {
        InMemoryMetabaseShard result = shards.get(ShardKey.get(labels));
        if (result == null) {
            throw new MetabaseRuntimeException("Shard not found by labels: " + labels,
                    EMetabaseStatusCode.SHARD_NOT_FOUND);
        }

        return result;
    }
}
