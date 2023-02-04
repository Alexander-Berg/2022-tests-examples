package ru.yandex.metabase.client.util;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.labels.protobuf.LabelConverter;
import ru.yandex.solomon.labels.protobuf.LabelSelectorConverter;
import ru.yandex.solomon.labels.query.Selectors;
import ru.yandex.solomon.metabase.api.protobuf.EMetabaseStatusCode;
import ru.yandex.solomon.metabase.api.protobuf.Metric;
import ru.yandex.solomon.metabase.api.protobuf.TLabelValuesRequest;
import ru.yandex.solomon.model.protobuf.MetricId;
import ru.yandex.solomon.util.labelStats.LabelStatsCollectors;
import ru.yandex.solomon.util.labelStats.LabelValuesStats;

/**
 * @author Vladimir Gordiychuk
 */
class InMemoryMetabaseShard {
    private final ConcurrentMap<Labels, Metric> metricByLabels = new ConcurrentHashMap<>();
    private final ConcurrentMap<MetricId, Metric> metricById = new ConcurrentHashMap<>();
    private volatile int metricsLimit = 1_000_000;

    Metric create(Metric metric) {
        Labels labels = LabelConverter.protoToLabels(metric.getLabelsList());

        // fill default params
        Metric.Builder builder = metric.toBuilder()
                .setMetricId(generateMetricId(metric));

        if (builder.getCreatedAtMillis() == 0) {
            builder.setCreatedAtMillis(System.currentTimeMillis());
        }

        return insert(labels, builder.build());
    }

    @Nullable
    Metric resolve(Labels labels) {
        return metricByLabels.get(labels);
    }

    @Nullable
    Metric delete(Metric metric) {
        Labels labels = LabelConverter.protoToLabels(metric.getLabelsList());
        Metric keyToDelete = metricByLabels.remove(labels);
        if (keyToDelete == null) {
            return null;
        }

        metricById.remove(keyToDelete.getMetricId());
        return keyToDelete;
    }

    Stream<Metric> find(Selectors selector) {
        return metricById.values()
                .stream()
                .filter(metric -> match(metric, selector));
    }

    int metricsCount() {
        return metricByLabels.size();
    }

    int metricsLimit() {
        return metricsLimit;
    }

    void setMetricsLimit(int limit) {
        this.metricsLimit = limit;
    }

    LabelValuesStats labelValues(TLabelValuesRequest request) {
        Selectors selector = LabelSelectorConverter.protoToSelectors(request.getSelectorsList());
        Set<String> labelValues = new HashSet<>(request.getLabelsList());

        LabelValuesStats result = metricById.values()
                .parallelStream()
                .map(metric -> LabelConverter.protoToLabels(metric.getLabelsList()))
                .filter(selector::match)
                .collect(LabelStatsCollectors.toLabelValuesStats(labelValues));
        result.filter(request.getTextSearch());
        result.limit(request.getLimit());
        return result;
    }

    @Nullable
    Metric read(Metric metric) {
        return metricById.get(metric.getMetricId());
    }

    private Metric insert(Labels labels, Metric metric) {
        Metric prevMetric;
        prevMetric = metricByLabels.putIfAbsent(labels, metric);
        if (prevMetric != null) {
            throw new MetabaseRuntimeException("Not unique labels: " + labels, EMetabaseStatusCode.DUPLICATE);
        }

        prevMetric = metricById.putIfAbsent(metric.getMetricId(), metric);
        if (prevMetric != null) {
            throw new MetabaseRuntimeException("Non unique metricId: " + metric.getMetricId(), EMetabaseStatusCode.DUPLICATE);
        }

        return metric;
    }

    private boolean match(Metric metric, Selectors selector) {
        return selector.match(LabelConverter.protoToLabels(metric.getLabelsList()));
    }

    private MetricId generateMetricId(Metric metric) {
        MetricId.Builder builder = metric.getMetricId().toBuilder();

        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (builder.getShardId() == 0) {
            builder.setShardId(random.nextInt(1, 4096));
        }

        if (builder.getLocalId() == 0) {
            builder.setLocalId(random.nextLong());
        }

        return builder.build();
    }
}
