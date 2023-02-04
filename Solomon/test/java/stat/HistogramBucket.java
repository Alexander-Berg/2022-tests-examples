package ru.yandex.solomon.math.stat;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.solomon.model.timeseries.GraphData;

/**
 * @author Vladimir Gordiychuk
 */
@ParametersAreNonnullByDefault
public class HistogramBucket implements Comparable<HistogramBucket> {
    private final double bucketLimit;
    private final GraphData counts;

    public HistogramBucket(double bucketLimit, GraphData counts) {
        this.bucketLimit = bucketLimit;
        this.counts = counts;
    }

    @Override
    public int compareTo(HistogramBucket o) {
        return Double.compare(bucketLimit, o.bucketLimit);
    }

    public double getBucketLimit() {
        return bucketLimit;
    }

    public GraphData getCounts() {
        return counts;
    }
}
