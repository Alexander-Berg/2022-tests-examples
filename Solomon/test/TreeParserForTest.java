package ru.yandex.solomon.coremon.stockpile.test;


import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.springframework.stereotype.Component;

import ru.yandex.monlib.metrics.MetricType;
import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.monlib.metrics.labels.LabelsBuilder;
import ru.yandex.solomon.metrics.parser.MetricConsumer;
import ru.yandex.solomon.metrics.parser.TreeParser;

/**
 * @author Maksim Leonov (nohttp@)
 */
@ParametersAreNonnullByDefault
@Component
public class TreeParserForTest implements TreeParser {

    private final String metricNameLabel;
    private Map<ByteBuf, List<MetricPoint>> requests = new IdentityHashMap<>();

    public TreeParserForTest(String metricNameLabel) {
        this.metricNameLabel = metricNameLabel;
    }

    @Override
    public void parseAndProcess(
        Labels commonLabels,
        ByteBuf bytes,
        MetricConsumer metricConsumer,
        ErrorListener errorListener,
        FormatListener formatListener,
        boolean onlyNewFormatWrites)
    {
        // TODO (rorewillo@): support onlyNewFormatWrites
        Assert.assertTrue(requests.containsKey(bytes));

        List<MetricPoint> points = requests.get(bytes);
        metricConsumer.ensureCapacity(points.size());
        for (MetricPoint point : points) {
            final Labels labelsForMetric;
            if (commonLabels.isEmpty()) {
                labelsForMetric = point.metricLabels;
            } else {
                LabelsBuilder builder = point.metricLabels.toBuilder();
                labelsForMetric = builder.addAll(commonLabels).build();
            }

            if (!metricNameLabel.isEmpty() && !labelsForMetric.hasKey(metricNameLabel)) {
                throw new IllegalStateException("unknown metric name, expected \"" + metricNameLabel + "\": " + labelsForMetric);
            }

            metricConsumer.onMetricBegin(point.type, labelsForMetric, point.memOnly);
            metricConsumer.onPoint(point.instantMillis, point.value);
        }
    }

    public ByteBuf addMetricBatch(MetricPoint... points) {
        ByteBuf key = Unpooled.copyLong(requests.size());
        requests.put(key, List.of(points));
        return key;
    }

    public static class MetricPoint {
        private final Labels metricLabels;
        private final double value;
        private final boolean memOnly;
        private final MetricType type;
        private final long instantMillis;

        public MetricPoint(Labels metricLabels, double value, boolean memOnly, MetricType type, long instantMillis) {
            this.metricLabels = metricLabels;
            this.value = value;
            this.memOnly = memOnly;
            this.type = type;
            this.instantMillis = instantMillis;
        }

        public double getValue() {
            return value;
        }

        public Labels getKey() {
            return metricLabels;
        }
    }
}
