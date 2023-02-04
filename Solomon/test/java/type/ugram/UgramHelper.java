package ru.yandex.solomon.model.type.ugram;

import java.util.Arrays;
import java.util.List;

import ru.yandex.solomon.model.type.Histogram;

/**
 * @author Vladimir Gordiychuk
 */
public class UgramHelper {

    public static Bucket bucket(double lower, double upper, double weight) {
        return Bucket.create(lower, upper, weight);
    }

    public static Histogram ugram(Bucket... buckets) {
        return ugram(Arrays.asList(buckets));
    }

    public static Histogram ugram(List<Bucket> buckets) {
        var result = Histogram.newInstance();
        double prev = 0;
        int idx = 0;
        for (var bucket : buckets) {
            if (Double.compare(bucket.lowerBound, prev) != 0) {
                result.setUpperBound(idx, bucket.lowerBound);
                result.setBucketValue(idx, 0);
                idx++;
            }
            result.setUpperBound(idx, bucket.upperBound);
            result.setBucketValue(idx, Math.round(bucket.weight));
            prev = bucket.upperBound;
            idx++;
        }
        return result;
    }
}
