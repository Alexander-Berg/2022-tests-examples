package ru.yandex.solomon.model.point.column;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.model.type.LogHistogram;

/**
 * @author Vladimir Gordiychuk
 */
public class LogHistogramColumnTest {

    @Test
    public void mergeHistogramWhenLeftNull() throws Exception {
        LogHistogram right = LogHistogram.ofBuckets(1, 2, 3);
        LogHistogram result = merge(null, right);

        Assert.assertThat(result, CoreMatchers.equalTo(right));
    }

    @Test
    public void mergeHistogramWhenRightNull() throws Exception {
        LogHistogram left = LogHistogram.ofBuckets(1, 2, 3);
        LogHistogram result = merge(left, null);

        Assert.assertThat(result, CoreMatchers.equalTo(left));
    }

    @Test
    public void mergeBothNullHistogram() throws Exception {
        LogHistogram result = merge(null, null);
        Assert.assertThat(result, CoreMatchers.nullValue());
    }

    @Test
    public void mergeWithLeftEmptyHistogram() throws Exception {
        LogHistogram left = LogHistogram.ofBuckets();
        LogHistogram right = LogHistogram.ofBuckets(1, 2, 3);
        LogHistogram result = merge(left, right);

        Assert.assertThat(result, CoreMatchers.equalTo(right));
    }

    @Test
    public void mergeWithRightEmptyHistogram() throws Exception {
        LogHistogram left = LogHistogram.ofBuckets(1, 2, 3);
        LogHistogram right = LogHistogram.ofBuckets();
        LogHistogram result = merge(left, right);

        Assert.assertThat(result, CoreMatchers.equalTo(left));
    }

    @Test
    public void mergeHistogramWithSameSize() throws Exception {
        LogHistogram left = LogHistogram.ofBuckets(1, 2, 3);
        LogHistogram right = LogHistogram.ofBuckets(5, 1, 4);

        LogHistogram wait = LogHistogram.ofBuckets(6, 3, 7);
        LogHistogram result = merge(left, right);

        Assert.assertThat(result, CoreMatchers.equalTo(wait));
    }

    @Test
    public void mergeHistogramWithDiffSize() throws Exception {
        LogHistogram left = LogHistogram.ofBuckets(4, 6, 27);
        LogHistogram right = LogHistogram.ofBuckets(20, 4, 2, 1.0, 3.0);

        LogHistogram wait = LogHistogram.ofBuckets(24, 10, 29, 1, 3);
        LogHistogram result = merge(left, right);

        Assert.assertThat(result, CoreMatchers.equalTo(wait));
    }

    @Test
    public void mergeCountZero() throws Exception {
        LogHistogram left = LogHistogram.newBuilder()
            .setBuckets(new double[]{2, 4, 1})
            .setCountZero(10)
            .build();

        LogHistogram right = LogHistogram.newBuilder()
            .setBuckets(new double[]{1, 1, 1})
            .setCountZero(22)
            .build();

        LogHistogram wait = LogHistogram.newBuilder()
            .setBuckets(new double[]{3, 5, 2})
            .setCountZero(32)
            .build();

        LogHistogram result = merge(left, right);

        Assert.assertThat(result, CoreMatchers.equalTo(wait));
    }

    @Test
    public void mergeWithDifferentStartPowerExtendUp() throws Exception {
        LogHistogram left = LogHistogram.newBuilder()
            .setBuckets(new double[]{2, 3, 4})
            .setStartPower(1) // 0, 2, 3, 4
            .build();

        LogHistogram right = LogHistogram.newBuilder()
            .setBuckets(new double[]{10, 20, 30})
            .setStartPower(2) // 0, 0, 10, 20, 30
            .build();

        LogHistogram wait = LogHistogram.newBuilder()
            .setBuckets(new double[]{2, 10 + 3, 20 + 4, 30})
            .setStartPower(1) // 0, 2, 13, 24, 30
            .build();

        LogHistogram result = merge(left, right);
        Assert.assertThat(result, CoreMatchers.equalTo(wait));
    }

    @Test
    public void mergeWithDifferentStartPowerExtendDown() throws Exception {
        LogHistogram left = LogHistogram.newBuilder()
            .setBuckets(new double[]{2, 3, 4})
            .setStartPower(1)
            .build();

        LogHistogram right = LogHistogram.newBuilder()
            .setBuckets(new double[]{10, 20, 30, 40})
            .setStartPower(-2)
            .build();

        LogHistogram wait = LogHistogram.newBuilder()
            .setBuckets(new double[]{10, 20, 30, 40 + 2, 3, 4})
            .setStartPower(-2)
            .build();

        LogHistogram result = merge(left, right);
        Assert.assertThat(result, CoreMatchers.equalTo(wait));
    }

    @Test
    public void mergeWithDifferentStartPowerExtendUpAndDown() throws Exception {
        LogHistogram left = LogHistogram.newBuilder()
            .setBuckets(new double[]{2, 3, 4})
            .setStartPower(1)
            .build();

        LogHistogram right = LogHistogram.newBuilder()
            .setBuckets(new double[]{10, 20, 30, 40, 50})
            .setStartPower(0)
            .build();

        LogHistogram wait = LogHistogram.newBuilder()
            .setBuckets(new double[]{10, 20 + 2, 30 + 3, 40 + 4, 50})
            .setStartPower(0)
            .build();

        LogHistogram result = merge(left, right);
        Assert.assertThat(result, CoreMatchers.equalTo(wait));
    }

    @Test
    public void mergeWithDifferentStartPowerExtendUpWithOverflow() throws Exception {
        LogHistogram left = LogHistogram.newBuilder()
            .setBuckets(new double[]{0, 2, 3, 4})
            .setStartPower(1)
            .setMaxBucketsSize(10)
            .build();

        LogHistogram right = LogHistogram.newBuilder()
            .setBuckets(new double[]{0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110})
            .setStartPower(1)
            .setMaxBucketsSize(10)
            .build();

        LogHistogram wait = LogHistogram.newBuilder()
            .setBuckets(new double[]{(10+20)+(2+3), 30+4, 40, 50, 60, 70, 80, 90, 100, 110})
            .setStartPower(3)
            .setMaxBucketsSize(10)
            .build();

        LogHistogram result = merge(left, right);
        Assert.assertThat(result, CoreMatchers.equalTo(wait));
    }

    @Test
    public void mergeWithDifferentStartPowerExtendDownWithOverflow() throws Exception {
        LogHistogram left = LogHistogram.newBuilder()
            .setBuckets(new double[]{0, 2, 3, 4, 5})
            .setCountZero(6)
            .setStartPower(1)
            .setMaxBucketsSize(10)
            .build();

        LogHistogram right = LogHistogram.newBuilder()
            .setBuckets(new double[]{0, 10, 20, 30, 40, 50, 60, 70, 80})
            .setCountZero(5)
            .setStartPower(-6)
            .setMaxBucketsSize(10)
            .build();

        LogHistogram wait = LogHistogram.newBuilder()
            .setBuckets(new double[]{(0+10+20), 30, 40, 50, 60, (70+0), (80+2), (0+3), (0+4), (0+5)})
            .setStartPower(-4)
            .setCountZero(6 + 5)
            .setMaxBucketsSize(10)
            .build();

        LogHistogram result = merge(left, right);
        Assert.assertThat(result, CoreMatchers.equalTo(wait));
    }

    @Test
    public void mergeWithDifferentStartPowerExtendDownWithOverflowAndSourceSum() throws Exception {
        LogHistogram left = LogHistogram.newBuilder()
            .setBuckets(new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9})
            .setStartPower(1)
            .setMaxBucketsSize(10)
            .build();

        LogHistogram right = LogHistogram.newBuilder()
            .setBuckets(new double[]{10, 20, 30, 40, 50, 60, 70, 80})
            .setStartPower(-10)
            .setMaxBucketsSize(10)
            .build();

        // I still not ensure that it's correct behaviour that the same as GOLOVAN have,
        // because significant metric can move from up to down,
        // but current implementation concentrate only on high values
        LogHistogram wait = LogHistogram.newBuilder()
            .setBuckets(new double[]{(10 + 20 + 30 + 40 + 50 + 60 + 70 + 80), 1, 2, 3, 4, 5, 6, 7, 8, 9})
            .setStartPower(0)
            .setMaxBucketsSize(10)
            .build();

        LogHistogram result = merge(left, right);

        Assert.assertThat(result, CoreMatchers.equalTo(wait));
    }

    @Test
    public void changeMaxBucketsLeadToSkipMerge() throws Exception {
        LogHistogram left = LogHistogram.newBuilder()
            .setBuckets(new double[]{1, 2, 3})
            .setStartPower(1)
            .setMaxBucketsSize(3)
            .build();

        LogHistogram right = LogHistogram.newBuilder()
            .setBuckets(new double[]{10, 20, 30, 40, 50, 60, 70, 80})
            .setStartPower(12)
            .setMaxBucketsSize(10)
            .build();

        LogHistogram result = merge(left, right);
        Assert.assertThat(result, CoreMatchers.equalTo(right));
    }

    @Test
    public void mergeWhenChangedBaseToHighValue() throws Exception {
        // bounds: 1, 2, 4, 8, 16, 32, 64, 128, etc
        LogHistogram first = LogHistogram.newBuilder()
            .setBuckets(new double[]{1, 0, 4}) // 1 value in 4 bounds, 4 value in 16 bounds
            .setMaxBucketsSize(5)
            .setStartPower(2)
            .setBase(2)
            .setCountZero(2)
            .build();

        // bounds: 1, 4, 16, 64, 256, 1024, etc
        LogHistogram second = LogHistogram.newBuilder()
            .setBuckets(new double[]{3, 2}) // 3 value in 16 bound, 2 value in 64 bound
            .setMaxBucketsSize(5)
            .setStartPower(2)
            .setBase(4)
            .setCountZero(5)
            .build();

        // bounds: 1, 4, 16, 64, 256, 1024, etc
        LogHistogram expect = LogHistogram.newBuilder()
            .setBuckets(new double[]{1, (4 + 3), 2})
            .setMaxBucketsSize(5)
            .setStartPower(1)
            .setBase(4)
            .setCountZero(5 + 2)
            .build();

        LogHistogram result = merge(first, second);
        Assert.assertThat(result, CoreMatchers.equalTo(expect));
    }

    @Test
    public void mergeWhenChangedBaseToLowValue() throws Exception {
        // bounds: 1, 4, 16, 64, 256, 1024, etc
        LogHistogram first = LogHistogram.newBuilder()
            .setBuckets(new double[]{3, 2}) // 3 value in 16 bound, 2 value in 64 bound
            .setMaxBucketsSize(5)
            .setStartPower(2)
            .setBase(4)
            .setCountZero(5)
            .build();

        // bounds: 1, 2, 4, 8, 16, 32, 64, 128, etc
        LogHistogram second = LogHistogram.newBuilder()
            .setBuckets(new double[]{1, 0, 4}) // 1 value in 4 bounds, 4 value in 16 bounds
            .setMaxBucketsSize(5)
            .setStartPower(2)
            .setBase(2)
            .setCountZero(2)
            .build();

        // bounds: 1, 2, 4, 8, 16, 32, 64, 128, etc
        LogHistogram expect = LogHistogram.newBuilder()
            .setBuckets(new double[]{1, 0, (4 + 3), 0, 2})
            .setMaxBucketsSize(5)
            .setStartPower(2)
            .setBase(2)
            .setCountZero(5 + 2)
            .build();

        LogHistogram result = merge(first, second);
        Assert.assertThat(result, CoreMatchers.equalTo(expect));
    }


    private LogHistogram merge(LogHistogram left, LogHistogram right) {
        var result = LogHistogramColumn.merge(left, right);
        System.out.println("l: " + left);
        System.out.println("r: " + right);
        System.out.println("s: " + result);
        return result;
    }
}
