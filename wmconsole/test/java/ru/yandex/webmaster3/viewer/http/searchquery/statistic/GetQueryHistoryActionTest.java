package ru.yandex.webmaster3.viewer.http.searchquery.statistic;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.easymock.EasyMock;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.webmaster3.core.searchquery.QueryId;
import ru.yandex.webmaster3.core.searchquery.QueryIndicator;
import ru.yandex.webmaster3.core.searchquery.SpecialGroup;
import ru.yandex.webmaster3.storage.searchquery.AggregatePeriod;
import ru.yandex.webmaster3.storage.searchquery.DeviceType;
import ru.yandex.webmaster3.storage.searchquery.QueryStatisticsService2;

/**
 * @author aherman
 */
public class GetQueryHistoryActionTest implements TestData {
    @SuppressWarnings("Duplicates")
    @Test
    public void testDay() throws Exception {
        GetQueryHistoryAction action = new GetQueryHistoryAction();
        QueryStatisticsService2 queryStatisticsService2 = EasyMock.createMock(QueryStatisticsService2.class);

        action.setQueryStatisticsService2(queryStatisticsService2);

        QueryId queryId = new QueryId(1);

        Map<QueryId, String> texts = new HashMap<>();
        texts.put(queryId, "1");

        EasyMock.expect(queryStatisticsService2.getQueryTexts(
                EasyMock.eq(hostId),
                EasyMock.eq(SpecialGroup.TOP_3000_QUERIES),
                EasyMock.eq(Collections.singletonList(queryId))
        )).andReturn(texts);

        EasyMock.expect(queryStatisticsService2.getStatistics(
                EasyMock.eq(SpecialGroup.TOP_3000_QUERIES),
                EasyMock.eq(hostId),
                EasyMock.anyObject(),
                EasyMock.eq(Collections.singletonList(queryId)),
                EasyMock.anyObject(),
                EasyMock.anyObject(),
                EasyMock.eq(LD_2016_01_01),
                EasyMock.eq(LD_2016_01_15),
                EasyMock.eq(DeviceType.ALL_DEVICES)
        )).andReturn(fillQueryStat(queryId, LD_2016_01_01, LD_2016_01_15));

        GetQueryHistoryRequest request = new GetQueryHistoryRequest();
        request.setHostId(hostId.toStringId());
        request.setConvertedHostId(hostId);
        request.setDateFrom(LD_2016_01_01.toDateTimeAtStartOfDay());
        request.setDateTo(LD_2016_01_15.toDateTimeAtStartOfDay());
        request.setIndicator(new QueryIndicator[]{
                QueryIndicator.TOTAL_SHOWS_COUNT,
                QueryIndicator.TOTAL_CTR,
                QueryIndicator.AVERAGE_SHOW_POSITION
        });
        request.setPeriod(AggregatePeriod.DAY);
        request.setQueryId(new String[]{queryId.toStringId()});
        request.setSpecialGroup(SpecialGroup.TOP_3000_QUERIES);

        EasyMock.replay(queryStatisticsService2);
        QueryStatisticsResponse response = action.process(request);
        EasyMock.verify(queryStatisticsService2);

        Assert.assertTrue(response instanceof QueryStatisticsResponse.NormalResponse);

        QueryStatisticsResponse.NormalResponse normalResponse = (QueryStatisticsResponse.NormalResponse) response;

        List<QueryStatisticsResponse.QueryStat> statistics = normalResponse.getStatistics();
        Assert.assertEquals(3, statistics.size());
        {
            QueryStatisticsResponse.QueryStat stat = statistics.get(0);
            Assert.assertEquals(QueryIndicator.TOTAL_SHOWS_COUNT, stat.getIndicator().getName());
            Assert.assertEquals(15, stat.getIndicator().getRanges().size());

            LocalDate d = LD_2016_01_01;
            int i = 0;
            while (d.isBefore(LD_2016_01_15) || d.equals(LD_2016_01_15)) {
                GroupsStatisticResponse.RangeStat rangeStat = stat.getIndicator().getRanges().get(i);
                Assert.assertEquals(d, rangeStat.getDateFrom());
                Assert.assertEquals(d, rangeStat.getDateTo());
                Assert.assertEquals(d.getDayOfMonth() * 10, rangeStat.getValue(), 0.01);
                i++;
                d = d.plusDays(1);
            }
        }
        {
            QueryStatisticsResponse.QueryStat stat = statistics.get(1);
            Assert.assertEquals(QueryIndicator.TOTAL_CTR, stat.getIndicator().getName());
            Assert.assertEquals(15, stat.getIndicator().getRanges().size());

            LocalDate d = LD_2016_01_01;
            int i = 0;
            while (d.isBefore(LD_2016_01_15) || d.equals(LD_2016_01_15)) {
                GroupsStatisticResponse.RangeStat rangeStat = stat.getIndicator().getRanges().get(i);
                Assert.assertEquals(d, rangeStat.getDateFrom());
                Assert.assertEquals(d, rangeStat.getDateTo());
                Assert.assertEquals(0.5, rangeStat.getValue(), 0.01);
                i++;
                d = d.plusDays(1);
            }
        }
        {
            QueryStatisticsResponse.QueryStat stat = statistics.get(2);
            Assert.assertEquals(QueryIndicator.AVERAGE_SHOW_POSITION, stat.getIndicator().getName());
            Assert.assertEquals(15, stat.getIndicator().getRanges().size());

            LocalDate d = LD_2016_01_01;
            int i = 0;
            while (d.isBefore(LD_2016_01_15) || d.equals(LD_2016_01_15)) {
                GroupsStatisticResponse.RangeStat rangeStat = stat.getIndicator().getRanges().get(i);
                Assert.assertEquals(d, rangeStat.getDateFrom());
                Assert.assertEquals(d, rangeStat.getDateTo());
                Assert.assertEquals(1, rangeStat.getValue(), 0.01);
                i++;
                d = d.plusDays(1);
            }
        }
    }
}
