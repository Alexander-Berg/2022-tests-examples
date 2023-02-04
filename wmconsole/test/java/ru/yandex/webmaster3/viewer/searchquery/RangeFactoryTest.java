package ru.yandex.webmaster3.viewer.searchquery;

import java.util.Set;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.webmaster3.storage.searchquery.AggregatePeriod;
import ru.yandex.webmaster3.storage.searchquery.RangeFactory;

/**
 * @author aherman
 */
@SuppressWarnings("Duplicates")
public class RangeFactoryTest {
    private static final LocalDate LD_2015_11_01 = new LocalDate("2015-11-01");
    private static final LocalDate LD_2015_11_30 = new LocalDate("2015-11-30");
    private static final LocalDate LD_2015_12_01 = new LocalDate("2015-12-01");
    private static final LocalDate LD_2015_12_14 = new LocalDate("2015-12-14");
    private static final LocalDate LD_2015_12_17 = new LocalDate("2015-12-17");
    private static final LocalDate LD_2015_12_20 = new LocalDate("2015-12-20");
    private static final LocalDate LD_2015_12_21 = new LocalDate("2015-12-21");
    private static final LocalDate LD_2015_12_27 = new LocalDate("2015-12-27");
    private static final LocalDate LD_2015_12_28 = new LocalDate("2015-12-28");
    private static final LocalDate LD_2015_12_31 = new LocalDate("2015-12-31");
    private static final LocalDate LD_2016_01_01 = new LocalDate("2016-01-01");
    private static final LocalDate LD_2016_01_02 = new LocalDate("2016-01-02");
    private static final LocalDate LD_2016_01_03 = new LocalDate("2016-01-03");
    private static final LocalDate LD_2016_01_04 = new LocalDate("2016-01-04");
    private static final LocalDate LD_2016_01_10 = new LocalDate("2016-01-10");
    private static final LocalDate LD_2016_01_11 = new LocalDate("2016-01-11");
    private static final LocalDate LD_2016_01_15 = new LocalDate("2016-01-15");
    private static final LocalDate LD_2016_01_17 = new LocalDate("2016-01-17");
    private static final LocalDate LD_2016_01_31 = new LocalDate("2016-01-31");

    @Test
    public void testSingleRange() throws Exception {
        RangeSet<LocalDate> rangeSet = RangeFactory.singleRange(LD_2016_01_01, LD_2016_01_15);
        Set<Range<LocalDate>> ranges = rangeSet.asRanges();
        Assert.assertEquals(1, ranges.size());
        Assert.assertEquals(Range.closed(LD_2016_01_01, LD_2016_01_15), ranges.iterator().next());
    }

    @Test
    public void testSingleRangeOneDay() throws Exception {
        RangeSet<LocalDate> rangeSet = RangeFactory.singleRange(LD_2016_01_01, LD_2016_01_01);
        Set<Range<LocalDate>> ranges = rangeSet.asRanges();
        Assert.assertEquals(1, ranges.size());
        Assert.assertEquals(Range.closed(LD_2016_01_01, LD_2016_01_01), ranges.iterator().next());
    }

    @Test
    public void testSingleRangeReverse() throws Exception {
        RangeSet<LocalDate> rangeSet = RangeFactory.singleRange(LD_2016_01_15, LD_2016_01_01);
        Set<Range<LocalDate>> ranges = rangeSet.asRanges();
        Assert.assertEquals(1, ranges.size());
        Assert.assertEquals(Range.closed(LD_2016_01_01, LD_2016_01_15), ranges.iterator().next());
    }

    @Test
    public void testDoubleRange() throws Exception {
        RangeSet<LocalDate> rangeSet = RangeFactory.doubleRange(LD_2016_01_01, LD_2016_01_15);
        Set<Range<LocalDate>> ranges = rangeSet.asRanges();
        Assert.assertEquals(2, ranges.size());
        Assert.assertTrue(ranges.contains(Range.closed(LD_2015_12_17, LD_2015_12_31)));
        Assert.assertTrue(ranges.contains(Range.closed(LD_2016_01_01, LD_2016_01_15)));
    }

    @Test
    public void testDoubleRangeOneDay() throws Exception {
        RangeSet<LocalDate> rangeSet = RangeFactory.doubleRange(LD_2016_01_01, LD_2016_01_01);
        Set<Range<LocalDate>> ranges = rangeSet.asRanges();
        Assert.assertEquals(2, ranges.size());
        Assert.assertTrue(ranges.contains(Range.closed(LD_2015_12_31, LD_2015_12_31)));
        Assert.assertTrue(ranges.contains(Range.closed(LD_2016_01_01, LD_2016_01_01)));
    }

    @Test
    public void testDayPeriod() throws Exception {
        Set<Range<LocalDate>> ranges = RangeFactory
                .createRanges(LD_2016_01_01, LD_2016_01_15, AggregatePeriod.DAY, false, false)
                .asRanges();
        Set<Range<LocalDate>> rangesSnap = RangeFactory
                .createRanges(LD_2016_01_01, LD_2016_01_15, AggregatePeriod.DAY, true, true)
                .asRanges();
        Assert.assertEquals(15, ranges.size());
        Assert.assertTrue(ranges.equals(rangesSnap));

        for(int i = 1; i <= 15; i++) {
            LocalDate d = new LocalDate(2016, 1, i);
            Assert.assertTrue(ranges.contains(Range.closed(d, d)));
        }
    }

    @Test
    public void testWeekNoSnap() throws Exception {
        Set<Range<LocalDate>> ranges = RangeFactory
                .createRanges(LD_2016_01_01, LD_2016_01_15, AggregatePeriod.WEEK, false, false)
                .asRanges();
        Assert.assertEquals(3, ranges.size());

        Assert.assertTrue(ranges.contains(Range.closed(LD_2016_01_01, LD_2016_01_03)));
        Assert.assertTrue(ranges.contains(Range.closed(LD_2016_01_04, LD_2016_01_10)));
        Assert.assertTrue(ranges.contains(Range.closed(LD_2016_01_11, LD_2016_01_15)));
    }

    @Test
    public void testWeekLeftSnap() throws Exception {
        Set<Range<LocalDate>> ranges = RangeFactory
                .createRanges(LD_2016_01_01, LD_2016_01_15, AggregatePeriod.WEEK, true, false)
                .asRanges();
        Assert.assertEquals(3, ranges.size());

        Assert.assertTrue(ranges.contains(Range.closed(LD_2015_12_28, LD_2016_01_03)));
        Assert.assertTrue(ranges.contains(Range.closed(LD_2016_01_04, LD_2016_01_10)));
        Assert.assertTrue(ranges.contains(Range.closed(LD_2016_01_11, LD_2016_01_15)));
    }

    @Test
    public void testWeekRightSnap() throws Exception {
        Set<Range<LocalDate>> ranges = RangeFactory
                .createRanges(LD_2016_01_01, LD_2016_01_15, AggregatePeriod.WEEK, false, true)
                .asRanges();
        Assert.assertEquals(3, ranges.size());

        Assert.assertTrue(ranges.contains(Range.closed(LD_2016_01_01, LD_2016_01_03)));
        Assert.assertTrue(ranges.contains(Range.closed(LD_2016_01_04, LD_2016_01_10)));
        Assert.assertTrue(ranges.contains(Range.closed(LD_2016_01_11, LD_2016_01_17)));
    }

    @Test
    public void testWeekFullSnap() throws Exception {
        Set<Range<LocalDate>> ranges = RangeFactory
                .createRanges(LD_2016_01_01, LD_2016_01_15, AggregatePeriod.WEEK, true, true)
                .asRanges();
        Assert.assertEquals(3, ranges.size());

        Assert.assertTrue(ranges.contains(Range.closed(LD_2015_12_28, LD_2016_01_03)));
        Assert.assertTrue(ranges.contains(Range.closed(LD_2016_01_04, LD_2016_01_10)));
        Assert.assertTrue(ranges.contains(Range.closed(LD_2016_01_11, LD_2016_01_17)));
    }

    @Test
    public void testMonthNoSnap() throws Exception {
        Set<Range<LocalDate>> ranges = RangeFactory
                .createRanges(LD_2016_01_02, LD_2016_01_15, AggregatePeriod.MONTH, false, false)
                .asRanges();
        Assert.assertEquals(1, ranges.size());
        Assert.assertTrue(ranges.contains(Range.closed(LD_2016_01_02, LD_2016_01_15)));
    }

    @Test
    public void testMonthNoSnap2() throws Exception {
        Set<Range<LocalDate>> ranges = RangeFactory
                .createRanges(LD_2015_12_28, LD_2016_01_15, AggregatePeriod.MONTH, false, false)
                .asRanges();
        Assert.assertEquals(2, ranges.size());
        Assert.assertTrue(ranges.contains(Range.closed(LD_2015_12_28, LD_2015_12_31)));
        Assert.assertTrue(ranges.contains(Range.closed(LD_2016_01_01, LD_2016_01_15)));
    }

    @Test
    public void testMontLeftSnap() throws Exception {
        Set<Range<LocalDate>> ranges = RangeFactory
                .createRanges(LD_2016_01_02, LD_2016_01_15, AggregatePeriod.MONTH, true, false)
                .asRanges();
        Assert.assertEquals(1, ranges.size());
        Assert.assertTrue(ranges.contains(Range.closed(LD_2016_01_01, LD_2016_01_15)));
    }

    @Test
    public void testMontLeftSnap2() throws Exception {
        Set<Range<LocalDate>> ranges = RangeFactory
                .createRanges(LD_2015_12_28, LD_2016_01_15, AggregatePeriod.MONTH, true, false)
                .asRanges();
        Assert.assertEquals(2, ranges.size());
        Assert.assertTrue(ranges.contains(Range.closed(LD_2015_12_01, LD_2015_12_31)));
        Assert.assertTrue(ranges.contains(Range.closed(LD_2016_01_01, LD_2016_01_15)));
    }

    @Test
    public void testMontRightSnap() throws Exception {
        Set<Range<LocalDate>> ranges = RangeFactory
                .createRanges(LD_2016_01_02, LD_2016_01_15, AggregatePeriod.MONTH, false, true)
                .asRanges();
        Assert.assertEquals(1, ranges.size());
        Assert.assertTrue(ranges.contains(Range.closed(LD_2016_01_02, LD_2016_01_31)));
    }

    @Test
    public void testMontRightSnap2() throws Exception {
        Set<Range<LocalDate>> ranges = RangeFactory
                .createRanges(LD_2015_12_28, LD_2016_01_15, AggregatePeriod.MONTH, false, true)
                .asRanges();
        Assert.assertEquals(2, ranges.size());
        Assert.assertTrue(ranges.contains(Range.closed(LD_2015_12_28, LD_2015_12_31)));
        Assert.assertTrue(ranges.contains(Range.closed(LD_2016_01_01, LD_2016_01_31)));
    }

    @Test
    public void testMontFullSnap() throws Exception {
        Set<Range<LocalDate>> ranges = RangeFactory
                .createRanges(LD_2016_01_02, LD_2016_01_15, AggregatePeriod.MONTH, true, true)
                .asRanges();
        Assert.assertEquals(1, ranges.size());
        Assert.assertTrue(ranges.contains(Range.closed(LD_2016_01_01, LD_2016_01_31)));
    }

    @Test
    public void testMontFullSnap2() throws Exception {
        Set<Range<LocalDate>> ranges = RangeFactory
                .createRanges(LD_2015_12_28, LD_2016_01_15, AggregatePeriod.MONTH, true, true)
                .asRanges();
        Assert.assertEquals(2, ranges.size());
        Assert.assertTrue(ranges.contains(Range.closed(LD_2015_12_01, LD_2015_12_31)));
        Assert.assertTrue(ranges.contains(Range.closed(LD_2016_01_01, LD_2016_01_31)));
    }

    @Test
    public void testDayNPeriods() throws Exception {
        Set<Range<LocalDate>> ranges = RangeFactory
                .createRanges(LD_2016_01_15, AggregatePeriod.DAY, 5, false)
                .asRanges();
        Set<Range<LocalDate>> rangesSnap = RangeFactory
                .createRanges(LD_2016_01_15, AggregatePeriod.DAY, 5, true)
                .asRanges();
        Assert.assertEquals(5, ranges.size());
        Assert.assertTrue(ranges.equals(rangesSnap));

        for (int i = 0; i < 5 ; i++) {
            LocalDate d = new LocalDate(2016, 1, 15 - i);
            Assert.assertTrue(ranges.contains(Range.closed(d, d)));
        }
    }

    @Test
    public void testWeekNPeriodNoSnap() throws Exception {
        Set<Range<LocalDate>> ranges = RangeFactory
                .createRanges(LD_2016_01_15, AggregatePeriod.WEEK, 5, false)
                .asRanges();
        Assert.assertEquals(5, ranges.size());
        Assert.assertTrue(ranges.contains(Range.closed(LD_2015_12_14, LD_2015_12_20)));
        Assert.assertTrue(ranges.contains(Range.closed(LD_2015_12_21, LD_2015_12_27)));
        Assert.assertTrue(ranges.contains(Range.closed(LD_2015_12_28, LD_2016_01_03)));
        Assert.assertTrue(ranges.contains(Range.closed(LD_2016_01_04, LD_2016_01_10)));
        Assert.assertTrue(ranges.contains(Range.closed(LD_2016_01_11, LD_2016_01_15)));
    }

    @Test
    public void testWeekNPeriodSnap() throws Exception {
        Set<Range<LocalDate>> ranges = RangeFactory
                .createRanges(LD_2016_01_15, AggregatePeriod.WEEK, 5, true)
                .asRanges();
        Assert.assertEquals(5, ranges.size());
        Assert.assertTrue(ranges.contains(Range.closed(LD_2015_12_14, LD_2015_12_20)));
        Assert.assertTrue(ranges.contains(Range.closed(LD_2015_12_21, LD_2015_12_27)));
        Assert.assertTrue(ranges.contains(Range.closed(LD_2015_12_28, LD_2016_01_03)));
        Assert.assertTrue(ranges.contains(Range.closed(LD_2016_01_04, LD_2016_01_10)));
        Assert.assertTrue(ranges.contains(Range.closed(LD_2016_01_11, LD_2016_01_17)));
    }

    @Test
    public void testMonthNPeriodNoSnap() throws Exception {
        Set<Range<LocalDate>> ranges = RangeFactory
                .createRanges(LD_2016_01_15, AggregatePeriod.MONTH, 3, false)
                .asRanges();
        Assert.assertEquals(3, ranges.size());
        Assert.assertTrue(ranges.contains(Range.closed(LD_2015_11_01, LD_2015_11_30)));
        Assert.assertTrue(ranges.contains(Range.closed(LD_2015_12_01, LD_2015_12_31)));
        Assert.assertTrue(ranges.contains(Range.closed(LD_2016_01_01, LD_2016_01_15)));
    }

    @Test
    public void testMonthNPeriodSnap() throws Exception {
        Set<Range<LocalDate>> ranges = RangeFactory
                .createRanges(LD_2016_01_15, AggregatePeriod.MONTH, 3, true)
                .asRanges();
        Assert.assertEquals(3, ranges.size());
        Assert.assertTrue(ranges.contains(Range.closed(LD_2015_11_01, LD_2015_11_30)));
        Assert.assertTrue(ranges.contains(Range.closed(LD_2015_12_01, LD_2015_12_31)));
        Assert.assertTrue(ranges.contains(Range.closed(LD_2016_01_01, LD_2016_01_31)));
    }
}
