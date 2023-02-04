package ru.yandex.solomon.model.type;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Vladimir Gordiychuk
 */
public class LogHistogramTest {

    @Test
    public void equalWithSameBuckets() throws Exception {
        LogHistogram first = LogHistogram.ofBuckets(1, 2, 3);
        LogHistogram second = LogHistogram.ofBuckets(1, 2, 3);

        Assert.assertThat(first, CoreMatchers.equalTo(second));
    }

    @Test
    public void notEqualWithDiffBuckets() throws Exception {
        LogHistogram first = LogHistogram.ofBuckets(1, 2, 3);
        LogHistogram second = LogHistogram.ofBuckets(3, 2, 1);

        Assert.assertThat(first, CoreMatchers.not(CoreMatchers.equalTo(second)));
    }

    @Test
    public void equalWithSameCountZeroAndBuckets() throws Exception {
        LogHistogram first = LogHistogram.newBuilder()
            .setBuckets(new double[]{1, 2, 3})
            .setCountZero(5)
            .build();

        LogHistogram second = LogHistogram.newBuilder()
            .setBuckets(new double[]{1, 2, 3})
            .setCountZero(5)
            .build();

        Assert.assertThat(first, CoreMatchers.equalTo(second));
    }

    @Test
    public void notEqualWithDiffCountZero() throws Exception {
        LogHistogram first = LogHistogram.newBuilder()
            .setBuckets(new double[]{1, 2, 3})
            .setCountZero(5)
            .build();

        LogHistogram second = LogHistogram.newBuilder()
            .setBuckets(new double[]{1, 2, 3})
            .setCountZero(15)
            .build();

        Assert.assertThat(first, CoreMatchers.not(CoreMatchers.equalTo(second)));
    }

    @Test
    public void notEqualWithDiffStartPower() throws Exception {
        LogHistogram first = LogHistogram.newBuilder()
            .setBuckets(new double[]{1, 2, 3})
            .setStartPower(10)
            .build();

        LogHistogram second = LogHistogram.newBuilder()
            .setBuckets(new double[]{1, 2, 3})
            .setStartPower(20)
            .build();

        Assert.assertThat(first, CoreMatchers.not(CoreMatchers.equalTo(second)));
    }

    @Test
    public void notEqualWithDiffLogBase() throws Exception {
        LogHistogram first = LogHistogram.newBuilder()
            .setBuckets(new double[]{1, 2, 3})
            .setBase(1.5)
            .build();

        LogHistogram second = LogHistogram.newBuilder()
            .setBuckets(new double[]{1, 2, 3})
            .setBase(2)
            .build();

        Assert.assertThat(first, CoreMatchers.not(CoreMatchers.equalTo(second)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void notAbleToCreateHistogramWithTooManyBuckets() throws Exception {
        LogHistogram histogram =
            LogHistogram.newBuilder()
                .setBuckets(new double[]{1, 2, 3})
                .setStartPower(10)
                .setCountZero(1)
                .setMaxBucketsSize(1000)
                .build();

        Assert.fail("Max buckets size affect merge process, and should be able configure outsize, but with limitation. " +
            "We can't allow to user set huge enough max bucket size because it will affect memory usage"
        );
    }

    @Test
    public void truncateLeadingZeroWithUpdateStartPower() throws Exception {
        LogHistogram histogram =
            LogHistogram.newBuilder()
                .setBuckets(new double[]{0, 0, 0, 2, 1, 3})
                .setCountZero(0)
                .setStartPower(-5)
                .build();

        LogHistogram expected =
            LogHistogram.newBuilder()
                .setBuckets(new double[]{2, 1, 3})
                .setCountZero(0)
                .setStartPower(-2)
                .build();

        Assert.assertThat("StartPower it's mechanize to reduce count value in buckets and as a result save memory, "
                + "so, if user specify data with leading zero, we cant truncate it and update startPower",
            histogram, CoreMatchers.equalTo(expected)
        );
    }

    @Test
    public void truncateTailingZero() throws Exception {
        LogHistogram histogram =
            LogHistogram.newBuilder()
                .setBuckets(new double[]{2, 1, 3, 0, 0, 0, 0})
                .setCountZero(0)
                .setStartPower(0)
                .build();

        LogHistogram expected =
            LogHistogram.newBuilder()
                .setBuckets(new double[]{2, 1, 3})
                .setCountZero(0)
                .setStartPower(0)
                .build();

        Assert.assertThat("Tailing zeros in buckets waste a memory and can be safe truncate without " +
            "affect application logic",
            histogram, CoreMatchers.equalTo(expected)
        );
    }

    @Test
    public void truncateLeadingAndTailingZero() throws Exception {
        LogHistogram histogram =
            LogHistogram.newBuilder()
                .setBuckets(new double[]{0, 0, 0, 2, 0, 0, 0, 1, 3, 0, 0, 0, 0})
                .setCountZero(0)
                .setStartPower(0)
                .build();

        LogHistogram expected =
            LogHistogram.newBuilder()
                .setBuckets(new double[]{2, 0, 0, 0, 1, 3})
                .setCountZero(0)
                .setStartPower(3)
                .build();

        Assert.assertThat(histogram, CoreMatchers.equalTo(expected));
    }

    @Test
    public void truncateBucketsWithOnlyZerosToEmpty() throws Exception {
        LogHistogram histogram =
            LogHistogram.newBuilder()
                .setBuckets(new double[]{0, 0, 0, 0, 0, 0, 0})
                .setCountZero(3)
                .setStartPower(10)
                .build();

        LogHistogram expected =
            LogHistogram.newBuilder()
                .setBuckets(new double[0])
                .setCountZero(3)
                .setStartPower(0)
                .build();

        Assert.assertThat(histogram, CoreMatchers.equalTo(expected));
    }
}
