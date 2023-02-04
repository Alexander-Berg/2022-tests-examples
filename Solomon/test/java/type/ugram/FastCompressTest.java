package ru.yandex.solomon.model.type.ugram;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hamcrest.Matchers;
import org.junit.Test;

import ru.yandex.solomon.model.type.Histogram;

import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.solomon.model.type.ugram.UgramHelper.bucket;
import static ru.yandex.solomon.model.type.ugram.UgramHelper.ugram;

/**
 * @author Vladimir Gordiychuk
 */
public class FastCompressTest {

    @Test
    public void compatibility_SimpleCompressSingleItetation() {
        compress(
            List.of(
                bucket(1.0, 2.0, 1.0),
                bucket(2.0, 3.0, 1.0),
                bucket(3.0, 4.0, 1.0),

                bucket(4.0, 5.0, 1.0),
                bucket(5.0, 6.0, 1.0),
                bucket(6.0, 7.0, 1.0),

                bucket(7.0, 8.0, 1.0),
                bucket(8.0, 9.0, 1.0)),
            ugram(
                bucket(1.0, 3.0, 1.0 + 1.0),
                bucket(3.0, 5.0, 1.0 + 1.0),
                bucket(5.0, 7.0, 1.0 + 1.0),
                bucket(7.0, 8.0, 1.0),
                bucket(8.0, 9.0, 1.0)
            ));
    }

    @Test
    public void compatibility_AnotherCompressSingleItetation() {
        compress(
            List.of(
                bucket(1.0, 2.0, 1.0),
                bucket(2.0, 3.0, 2.0),
                bucket(3.0, 4.0, 3.0), // merge this
                bucket(4.0, 5.0, 4.0), // and this
                bucket(5.0, 6.0, 5.0),
                bucket(6.0, 7.0, 6.0),
                bucket(7.0, 8.0, 7.0),  // this
                bucket(8.0, 9.0, 8.0),  // and this
                bucket(9.0, 10.0, 9.0)),
            ugram(
                bucket(1.0, 2.0, 1.0),
                bucket(2.0, 3.0, 2.0),
                bucket(3.0, 5.0, 3.0 + 4.0),
                bucket(5.0, 6.0, 5.0),
                bucket(6.0, 7.0, 6.0),
                bucket(7.0, 9.0, 7.0 + 8.0),
                bucket(9.0, 10.0, 9.0)
            )
        );
    }

    @Test
    public void compatibility_AnotherCompressSingleItetation2() {
        compress(
            List.of(
                bucket(5.0, 10.0, 1.0),
                bucket(10.0, 12.0, 3.0),  // merge this
                bucket(15.0, 20.0, 3.0),  // and this
                bucket(30.0, 30.0, 2.0),
                bucket(30.0, 40.0, 3.0)
            ),
            ugram(
                bucket(5.0, 10.0, 1.0),
                bucket(10.0, 20.0, 3.0 + 3.0),
                bucket(30.0, 30.0, 2.0),
                bucket(30.0, 40.0, 3.0)
            )
        );
    }

    @Test
    public void compatibility_TwoPointsCompressSingleItetation() {
        compress(
            List.of(
                bucket(1.0, 1.0, 1.0),
                bucket(49.0, 50.0, 1.0),  // merge this
                bucket(50.0, 51.0, 1.0),  // and this
                bucket(100.0, 100.0, 1.0),
                bucket(200.0, 200.0, 1.0)
            ),
            ugram(
                bucket(1.0, 1.0, 1.0),
                bucket(49.0, 51.0, 2.0),
                bucket(100.0, 100.0, 1.0),
                bucket(200.0, 200.0, 1.0)
            )
        );
    }

    @Test
    public void compatibility_CompressBucketsWithSameWeight() {
        List<Bucket> source = genBuckets(1, 200);
        assertEquals(199, source.size());

        FastCompress.compress(source, 100);
        assertEquals(199.0, weightSum(source), 0.01);
        assertThat(source.size(), lessThanOrEqualTo(100));

        // compress buckets on start, then at the end
        assertEquals(source.get(0).size(), source.get(0).weight, 0.0);
        assertEquals(source.get(25).size(), source.get(25).weight, 0.0);

        var lf4 = source.get(source.size() - 4);
        assertEquals(lf4.size(), lf4.weight, 0.01);

        // this is uncompressed "tail"
        assertBucketEqual(bucket(199.0, 200.0, 1.0), source.get(source.size() - 1));
    }

    @Test
    public void threeBuckets() {
        compress(
            List.of(
                bucket(0, 5, 42),
                bucket(5, 10, 4),
                bucket(10, 15, 0)),
            ugram(
                bucket(0, 10, 46),
                bucket(10, 15, 0)
            ));
    }

    @Test
    public void fourBuckets() {
        compress(
            List.of(
                bucket(10.0, 30.0, 8.0),
                bucket(30.0, 40.0, 3.0),
                bucket(40.0, 45.0, 5.0),
                bucket(45.0, 50.0, 10.0)),
            ugram(
                bucket(10, 40, 11),
                bucket(40, 45, 5),
                bucket(45, 50, 10)
            ));
    }

    @Test
    public void fourBucketsSpikeFirst() {
        compress(
            List.of(
                bucket(10.0, 30.0, 42.0),
                bucket(30.0, 40.0, 3.0),
                bucket(40.0, 45.0, 5.0),
                bucket(45.0, 50.0, 10.0)),
            ugram(
                bucket(10, 30, 42),
                bucket(30, 45, 8),
                bucket(45, 50, 10)
            ));
    }

    @Test
    public void fourBucketsSpikeSecond() {
        compress(
            List.of(
                bucket(10.0, 30.0, 2.0),
                bucket(30.0, 40.0, 142.0),
                bucket(40.0, 45.0, 5.0),
                bucket(45.0, 50.0, 10.0)),
            ugram(
                bucket(10, 30, 2),
                bucket(30, 45, 147),
                bucket(45, 50, 10)
            ));
    }

    public void assertBucketEqual(Bucket expected, Bucket actual) {
        System.out.println("assert " + expected + " == " + actual);
        assertEquals(expected.upperBound, actual.upperBound, 0.0);
        assertEquals(expected.lowerBound, actual.lowerBound, 0.0);
        assertEquals(expected.weight, actual.weight, 0.0);
    }

    @Test
    public void compatibility_CompressBucketsWithOnePike() {
        List<Bucket> source = new ArrayList<>();

        source.add(bucket(1, 2, 1));
        source.add(bucket(2, 3, 1000));
        source.addAll(genBuckets(3, 300));

        double expectWeightSum = weightSum(source);
        FastCompress.compress(source, 100);

        assertEquals(expectWeightSum, weightSum(source), 0.0);
        assertThat(source.size(), lessThanOrEqualTo(100));

        // second bucket "protects" first from merge
        assertBucketEqual(bucket(1, 2, 1), source.get(0));

        // too heavy for merge
        assertBucketEqual(bucket(2, 3, 1000), source.get(1));

        // simple bucket, was merged many times
        assertBucketEqual(bucket(3, 7, 4), source.get(2));
    }

    @Test
    public void compressRandomData() {
        for (int index = 0; index < 100; index++) {
            var buckets = genBuckets(ThreadLocalRandom.current().nextInt(10, 100));
            try {
                var result = buckets.stream()
                        .map(bucket -> Bucket.create(bucket.lowerBound, bucket.upperBound, bucket.weight))
                        .collect(Collectors.toList());

                FastCompress.compress(result, 10);
                assertThat(result.size(), Matchers.lessThanOrEqualTo(buckets.size()));
            } catch (Throwable e) {
                buckets.forEach(System.out::println);
                throw new RuntimeException(e);
            }
        }
    }

    private double weightSum(List<Bucket> buckets) {
        return buckets.stream().mapToDouble(bucket -> bucket.weight).sum();
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

    private List<Bucket> genBuckets(int start, int stop) {
        return IntStream.range(start, stop)
            .mapToObj(idx -> Bucket.create(idx, idx + 1, 1.0))
            .collect(Collectors.toList());
    }


    private void compress(List<Bucket> buckets, Histogram expected) {
        List<Bucket> list = new ArrayList<>(buckets);
        FastCompress.compress(list, 100);
        var actual = ugram(list);
        assertEquals(expected, actual);
    }
}
