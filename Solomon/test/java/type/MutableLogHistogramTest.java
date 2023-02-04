package ru.yandex.solomon.model.type;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Vladimir Gordiychuk
 */
public class MutableLogHistogramTest {

    @Test
    public void zeroValueAccumulateAsCount() throws Exception {
        MutableLogHistogram mutable = new MutableLogHistogram()
            .addHistogram(LogHistogram.newBuilder()
                .setBuckets(new double[]{1, 2, 3})
                .setCountZero(12)
                .build());

        mutable.addValue(0);
        mutable.addValue(0);
        mutable.addValue(0);

        LogHistogram expected =
            LogHistogram.newBuilder()
                .setBuckets(new double[]{1, 2, 3})
                .setCountZero(12 + 3)
                .build();

        Assert.assertThat(mutable.toImmutable(), CoreMatchers.equalTo(expected));
    }

    @Test
    public void reduceBase() {
        var one = LogHistogram.newInstance()
            .setBase(2)
            .setStartPower(0)
            .addBucket(2)
            .build();

        var two = LogHistogram.newInstance()
            .setBase(1.5)
            .addBucket(1);

        MutableLogHistogram mutable = new MutableLogHistogram();
        mutable.addHistogram(one);
        mutable.addHistogram(two);
    }

    @Test
    @Ignore("It's not valid behaviour, we should count only zero values, and restrict negative values")
    public void negativeValuesInterpretAsZero() throws Exception {
        MutableLogHistogram mutable = new MutableLogHistogram()
            .addHistogram(LogHistogram.newBuilder()
                .setBuckets(new double[]{2})
                .setCountZero(0)
                .build());

        mutable.addValue(0);
        mutable.addValue(-1);
        mutable.addValue(-2);
        mutable.addValue(-123);

        LogHistogram expected =
            LogHistogram.newBuilder()
                .setBuckets(new double[]{2})
                .setCountZero(4)
                .build();

        Assert.assertThat(mutable.toImmutable(), CoreMatchers.equalTo(expected));
    }

    @Test
    public void addMultipleValuesExpendsUp() throws Exception {
        LogHistogram source = LogHistogram.newBuilder()
            .setBuckets(new double[]{10, 20, 30, 40, 50, 60, 70})
            .setCountZero(100)
            .setStartPower(-1)
            .build();

        MutableLogHistogram mutable = new MutableLogHistogram().addHistogram(source);
        mutable.addValues(1.5, 30, 10, 0, Double.NaN, 8.0); // bucket form: [[0, 0, 1, 0, 0, 0, 2, 0, 0, 1], 1, -1]

        LogHistogram expected = LogHistogram.newBuilder()
            .setBuckets(new double[]{10, 20, (30 + 1), 40, 50, 60, (70 + 2), 0, 0, 1})
            .setCountZero(100 + 1)
            .setStartPower(-1)
            .build();

        Assert.assertThat(mutable.toImmutable(), CoreMatchers.equalTo(expected));
    }

    @Test
    public void addValuesToEmptyHistogram() throws Exception {
        MutableLogHistogram mutable = new MutableLogHistogram()
            .addHistogram(LogHistogram.newBuilder().setMaxBucketsSize(10).build());
        mutable.addValues(0, 0, 0, 0, 1, 10, 2, 3, 4, 5, 6, 7, 8, 9);

        LogHistogram expected = LogHistogram.newBuilder()
            .setBuckets(new double[]{1, 1, 1, 2, 2, 3})
            .setCountZero(4)
            .setStartPower(0)
            .setMaxBucketsSize(10)
            .build();

        Assert.assertThat(mutable.toImmutable(), CoreMatchers.equalTo(expected));
    }

    @Test
    public void addValuesToEmptyHistogramActualizeStartPower() throws Exception {
        MutableLogHistogram mutable = new MutableLogHistogram()
            .addHistogram(LogHistogram.newBuilder().setMaxBucketsSize(10).build());
        mutable.addValues(100);
        mutable.addValues(101);
        mutable.addValues(102);

        LogHistogram expected = LogHistogram.newBuilder()
            .setBuckets(new double[]{3})
            .setCountZero(0)
            .setStartPower(11)
            .setMaxBucketsSize(10)
            .build();

        Assert.assertThat(mutable.toImmutable(), CoreMatchers.equalTo(expected));
    }

    public LogHistogram merge(LogHistogram left, LogHistogram right) {
        var mutable = new MutableLogHistogram();
        mutable.addHistogram(left);
        mutable.addHistogram(right);
        var result = mutable.toImmutable();
        System.out.println(left + " + " + right + " = " + result);
        return result;
    }
}
