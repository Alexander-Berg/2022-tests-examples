package ru.yandex.webmaster3.viewer.searchquery;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.webmaster3.core.searchquery.QueryId;
import ru.yandex.webmaster3.storage.searchquery.DayStat;
import ru.yandex.webmaster3.storage.searchquery.QueryStat;
import ru.yandex.webmaster3.storage.searchquery.util.CtrAccumulator;

/**
 * @author aherman
 */
public class CtrAccumulatorTest {
    @Test
    public void testCtr() throws Exception {
        CtrAccumulator accumulator = new CtrAccumulator(DayStat::getTotalClicksCount, DayStat::getTotalShowsCount);
        QueryId queryId = new QueryId(1);
        accumulator.apply(new QueryStat(new LocalDate("2016-01-01"), queryId, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        accumulator.apply(new QueryStat(new LocalDate("2016-01-02"), queryId, 2, 2, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        accumulator.apply(new QueryStat(new LocalDate("2016-01-03"), queryId, 3, 3, 3, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));

        Assert.assertEquals(1, accumulator.getValue(), 0.01);
    }
}
