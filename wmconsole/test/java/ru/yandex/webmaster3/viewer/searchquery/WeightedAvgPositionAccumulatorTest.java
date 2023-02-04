package ru.yandex.webmaster3.viewer.searchquery;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.webmaster3.core.searchquery.QueryId;
import ru.yandex.webmaster3.storage.searchquery.DayStat;
import ru.yandex.webmaster3.storage.searchquery.QueryStat;
import ru.yandex.webmaster3.storage.searchquery.util.WeightedAvgPositionAccumulator;

/**
 * @author aherman
 */
public class WeightedAvgPositionAccumulatorTest {
    @Test
    public void testAveragePosition() throws Exception {
        WeightedAvgPositionAccumulator accumulator = new WeightedAvgPositionAccumulator(DayStat::getShowsCount_2_3, DayStat::getAggrShowsCount_2_3);
        QueryId queryId = new QueryId(1);
        accumulator.apply(new QueryStat(new LocalDate("2016-01-01"), queryId, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1 * 2, 0, 0, 0, 0, 0, 0));
        accumulator.apply(new QueryStat(new LocalDate("2016-01-02"), queryId, 2, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1 * 3, 0, 0, 0, 0, 0, 0));
        accumulator.apply(new QueryStat(new LocalDate("2016-01-03"), queryId, 3, 0, 0, 0, 3, 0, 0, 0, 0, 0, 2 * 2 + 1 * 3, 0, 0, 0, 0, 0, 0));

        Assert.assertEquals((2.0 * 3.0 + 3.0 * 2.0)/ (1.0 + 1.0 + 3.0) , accumulator.getValue(), 0.01);
    }
}
