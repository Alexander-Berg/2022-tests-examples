package ru.yandex.solomon.coremon.meta;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.labels.query.Selectors;
import ru.yandex.solomon.util.collection.CloseableIterator;
import ru.yandex.solomon.util.labelStats.LabelValuesStats;


/**
 * @author Sergey Polovko
 */
public class TestMetricsCollection<Metric extends CoremonMetric> implements MetricsCollection<Metric> {

    private final Map<Labels, Metric> metrics = new HashMap<>();


    public TestMetricsCollection(Metric... metrics) {
        this(Arrays.asList(metrics));
    }

    public TestMetricsCollection(List<Metric> metrics) {
        for (Metric metric : metrics) {
            this.metrics.put(metric.getLabels(), metric);
        }
    }

    @Nullable
    @Override
    public Metric getOrNull(Labels key) {
        return metrics.get(key);
    }

    @Override
    public boolean has(Labels key) {
        return metrics.containsKey(key);
    }

    @Override
    public CompletableFuture<Void> put(Metric metric) {
        metrics.put(metric.getLabels(), metric);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> putAll(CoremonMetricArray metrics) {
        for (int i = 0; i < metrics.size(); i++) {
            @SuppressWarnings("unchecked")
            Metric metric = (Metric) new FileCoremonMetric(metrics.get(i));
            this.metrics.put(metrics.getLabels(i), metric);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> removeAll(Collection<Labels> keys) {
        for (Labels key : keys) {
            metrics.remove(key);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public int searchMetrics(Selectors selectors, int offset, int limit, Consumer<CoremonMetric> fn) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int searchLabels(Selectors selectors, int offset, int limit, Consumer<Labels> fn) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int searchCount(Selectors metricSelector) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SearchIteratorResult searchIterator(Selectors selectors) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> labelNames() {
        throw new UnsupportedOperationException();
    }

    @Override
    public LabelValuesStats labelStats(Set<String> requestedNames) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long searchIndexSize() {
        return 0;
    }

    @Override
    public long searchIndexCacheSize() {
        return 0;
    }

    @Override
    public int size() {
        return metrics.size();
    }

    @Override
    public long memorySizeIncludingSelf() {
        return 0;
    }

    @Override
    public CloseableIterator<Metric> iterator() {
        return CloseableIterator.of(metrics.values().iterator());
    }
}
