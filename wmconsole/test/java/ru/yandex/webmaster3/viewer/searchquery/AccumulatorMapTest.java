package ru.yandex.webmaster3.viewer.searchquery;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.webmaster3.core.searchquery.QueryId;
import ru.yandex.webmaster3.core.searchquery.QueryIndicator;
import ru.yandex.webmaster3.storage.searchquery.AccumulatorMap;
import ru.yandex.webmaster3.storage.searchquery.QueryStat;

/**
 * @author aherman
 */
public class AccumulatorMapTest {
    private static final LocalDate LD_2016_01_12 = new LocalDate("2016-01-12");
    private static final LocalDate LD_2016_01_13 = new LocalDate("2016-01-13");
    private static final LocalDate LD_2016_01_14 = new LocalDate("2016-01-14");
    private static final LocalDate LD_2016_01_15 = new LocalDate("2016-01-15");

    @Test
    public void testAccumulator() throws Exception {
        RangeSet<LocalDate> rangeSet = TreeRangeSet.create();

        rangeSet.add(Range.closed(LD_2016_01_12, LD_2016_01_12));
        rangeSet.add(Range.closed(LD_2016_01_13, LD_2016_01_13));
        rangeSet.add(Range.closed(LD_2016_01_14, LD_2016_01_14));
        rangeSet.add(Range.closed(LD_2016_01_15, LD_2016_01_15));

        AccumulatorMap accumulatorMap = AccumulatorMap.create(
                Lists.newArrayList(
                        QueryIndicator.TOTAL_SHOWS_COUNT,
                        QueryIndicator.TOTAL_CTR,
                        QueryIndicator.AVERAGE_SHOW_POSITION
                ),
                rangeSet
        );

        QueryId queryId = new QueryId(1);

        accumulatorMap.apply(new QueryStat(LD_2016_01_13, queryId, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        accumulatorMap.apply(new QueryStat(LD_2016_01_15, queryId, 3, 3, 3, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));

        {
            List<Pair<Range<LocalDate>, Double>> stat =
                    accumulatorMap.getIndicator(QueryIndicator.TOTAL_SHOWS_COUNT);
            Assert.assertEquals(4, stat.size());

            testRangeStat(stat.get(0), LD_2016_01_12, LD_2016_01_12);
            Assert.assertEquals(0, stat.get(0).getValue(), 0.01);

            testRangeStat(stat.get(1), LD_2016_01_13, LD_2016_01_13);
            Assert.assertEquals(1, stat.get(1).getValue(), 0.01);

            testRangeStat(stat.get(2), LD_2016_01_14, LD_2016_01_14);
            Assert.assertEquals(0, stat.get(2).getValue(), 0.01);

            testRangeStat(stat.get(3), LD_2016_01_15, LD_2016_01_15);
            Assert.assertEquals(3, stat.get(3).getValue(), 0.01);
        }
        {
            List<Pair<Range<LocalDate>, Double>> stat =
                    accumulatorMap.getIndicator(QueryIndicator.TOTAL_CTR);
            Assert.assertEquals(4, stat.size());

            testRangeStat(stat.get(0), LD_2016_01_12, LD_2016_01_12);
            Assert.assertNull(stat.get(0).getValue());

            testRangeStat(stat.get(1), LD_2016_01_13, LD_2016_01_13);
            Assert.assertEquals(1, stat.get(1).getValue(), 0.01);

            testRangeStat(stat.get(2), LD_2016_01_14, LD_2016_01_14);
            Assert.assertNull(stat.get(2).getValue());

            testRangeStat(stat.get(3), LD_2016_01_15, LD_2016_01_15);
            Assert.assertEquals(1, stat.get(3).getValue(), 0.01);
        }
        {
            List<Pair<Range<LocalDate>, Double>> stat =
                    accumulatorMap.getIndicator(QueryIndicator.AVERAGE_SHOW_POSITION);
            Assert.assertEquals(4, stat.size());

            testRangeStat(stat.get(0), LD_2016_01_12, LD_2016_01_12);
            Assert.assertNull(stat.get(0).getValue());

            testRangeStat(stat.get(1), LD_2016_01_13, LD_2016_01_13);
            Assert.assertEquals(1, stat.get(1).getValue(), 0.01);

            testRangeStat(stat.get(2), LD_2016_01_14, LD_2016_01_14);
            Assert.assertNull(stat.get(2).getValue());

            testRangeStat(stat.get(3), LD_2016_01_15, LD_2016_01_15);
            Assert.assertEquals(1, stat.get(3).getValue(), 0.01);
        }
    }

    private void testRangeStat(Pair<Range<LocalDate>, Double> data, LocalDate dateFrom, LocalDate dateTo) {
        Assert.assertEquals(dateFrom, data.getKey().lowerEndpoint());
        Assert.assertEquals(dateTo, data.getKey().upperEndpoint());
    }
}
