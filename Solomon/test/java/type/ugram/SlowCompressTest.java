package ru.yandex.solomon.model.type.ugram;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.junit.Test;

import ru.yandex.solomon.model.type.Histogram;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.solomon.model.type.ugram.UgramHelper.bucket;
import static ru.yandex.solomon.model.type.ugram.UgramHelper.ugram;

/**
 * @author Vladimir Gordiychuk
 */
public class SlowCompressTest {

    @Test
    public void compatibility_SingleIterationFirstPair() {
        compress(
            List.of(
                bucket(1.0, 2.0, 1.0),
                bucket(2.0, 3.0, 1.0),
                bucket(5.5, 6.0, 1.0),
                bucket(6.0, 6.1, 1.0),
                bucket(8.0, 9.0, 1.0)),
            ugram(
                bucket(1.0, 3.0, 1.0 + 1.0),
                bucket(5.5, 6.0, 1.0),
                bucket(6.0, 6.1, 1.0),
                bucket(8.0, 9.0, 1.0)
            ));
    }

    @Test
    public void compatibility_SingleIterationLastPair() {
        compress(
            List.of(
                bucket(1.0, 2.0, 1.0),
                bucket(2.5, 3.0, 1.0),
                bucket(3.0, 3.1, 1.0),
                bucket(4.0, 5.0, 1.0), // this
                bucket(5.0, 6.0, 1.0)), // and this
            ugram(
                bucket(1.0, 2.0, 1.0),
                bucket(2.5, 3.0, 1.0),
                bucket(3.0, 3.1, 1.0),
                bucket(4.0, 6.0, 1.0 + 1.0)
            ));
    }

    @Test
    public void compatibility_Compress() {
        compress(
            List.of(
                bucket(1.0, 2.0, 1.0),
                bucket(2.0, 3.0, 1.0),
                bucket(3.0, 4.0, 1.0)),
            ugram(
                bucket(1.0, 3.0, 2.0),
                bucket(3.0, 4.0, 1.0)
            ));
    }

    @Test
    public void compressRandomData() {
        for (int index = 0; index < 100; index++) {
            var buckets = genBuckets(ThreadLocalRandom.current().nextInt(10, 100));
            try {
                var result = buckets.stream()
                        .map(bucket -> Bucket.create(bucket.lowerBound, bucket.upperBound, bucket.weight))
                        .collect(Collectors.toList());

                SlowCompress.compress(result, 10);
                assertThat(result.size(), Matchers.lessThanOrEqualTo(10));
            } catch (Throwable e) {
                buckets.forEach(System.out::println);
                throw new RuntimeException(e);
            }
        }
    }

    private void compress(List<Bucket> buckets, Histogram expected) {
        List<Bucket> list = new ArrayList<>(buckets);
        SlowCompress.compress(list, expected.count());
        var actual = ugram(list);
        assertEquals(expected, actual);
    }

    private List<Bucket> genBuckets(int maxCount) {
        var random = ThreadLocalRandom.current();
        List<Bucket> result = new ArrayList<>(maxCount);

        double lowerBound = random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE - maxCount);
        for (int index = 0; index < maxCount; index++) {
            if (lowerBound == Double.MAX_VALUE) {
                return result;
            }

            double upperBound = random.nextDouble(lowerBound, Double.MAX_VALUE);
            double weight = random.nextDouble(Double.MAX_VALUE);
            result.add(Bucket.create(lowerBound, upperBound, weight));
            lowerBound = upperBound;
        }

        return result;
    }

}
