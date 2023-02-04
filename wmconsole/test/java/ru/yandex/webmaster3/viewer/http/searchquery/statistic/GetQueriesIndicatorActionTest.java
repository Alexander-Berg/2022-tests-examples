package ru.yandex.webmaster3.viewer.http.searchquery.statistic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.easymock.EasyMock;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.webmaster3.core.searchquery.OrderDirection;
import ru.yandex.webmaster3.core.searchquery.QueryId;
import ru.yandex.webmaster3.core.searchquery.QueryIndicator;
import ru.yandex.webmaster3.core.searchquery.SpecialGroup;
import ru.yandex.webmaster3.storage.searchquery.*;

/**
 * @author aherman
 */
public class GetQueriesIndicatorActionTest implements TestData {
    @Test
    public void testDayStatsSum() throws Exception {
        final QueryIndicator indicator = QueryIndicator.TOTAL_SHOWS_COUNT;

        GetQueriesIndicatorAction action = new GetQueriesIndicatorAction();
        QueryStatisticsService2 statisticsService2 = EasyMock.createMock(QueryStatisticsService2.class);
        action.setQueryStatisticsService2(statisticsService2);

        EasyMock.expect(statisticsService2.countQueries(
                EasyMock.eq(hostId),
                EasyMock.eq(SpecialGroup.TOP_3000_QUERIES),
                EasyMock.anyObject(),
                EasyMock.anyObject(),
                EasyMock.eq(LD_2016_01_01),
                EasyMock.eq(LD_2016_01_15),
                EasyMock.eq(DeviceType.ALL_DEVICES)
        )).andReturn(7);

        List<QueryId> queryIds = new ArrayList<>();
        Map<QueryId, String> queryTexts = new HashMap<>();
        for (int i = 0; i < 7; i++) {
            QueryId q = new QueryId(i + 1);
            queryIds.add(q);
            queryTexts.put(q, String.valueOf(i + 1));
        }

        EasyMock.expect(statisticsService2.getQueryIds(
                EasyMock.eq(hostId),
                EasyMock.eq(SpecialGroup.TOP_3000_QUERIES),
                EasyMock.<Set<Integer>>anyObject(),
                EasyMock.<RegionInclusion>anyObject(),
                EasyMock.eq(LD_2016_01_01),
                EasyMock.eq(LD_2016_01_15),
                EasyMock.<List<QueryId>>anyObject(),
                EasyMock.eq(DeviceType.ALL_DEVICES),
                EasyMock.eq(QueryIndicator.TOTAL_CLICKS_COUNT),
                EasyMock.eq(OrderDirection.DESC),
                EasyMock.eq(0),
                EasyMock.eq(25)
        )).andReturn(queryIds);

        EasyMock.expect(statisticsService2.getQueryTexts(
                EasyMock.eq(hostId),
                EasyMock.eq(SpecialGroup.TOP_3000_QUERIES),
                EasyMock.eq(queryIds)
        )).andReturn(queryTexts);

        List<QueryStat> stat = new ArrayList<>();
        {
            for (int i = 0; i < 7; i++) {
                QueryId queryId = new QueryId(i + 1);
                stat.addAll(fillQueryStat(queryId, LD_2015_12_17, LD_2016_01_15));
            }
        }
        EasyMock.expect(statisticsService2.getStatistics(
                EasyMock.eq(SpecialGroup.TOP_3000_QUERIES),
                EasyMock.eq(hostId),
                EasyMock.anyObject(),
                EasyMock.eq(queryIds),
                EasyMock.anyObject(),
                EasyMock.anyObject(),
                EasyMock.eq(LD_2015_12_17),
                EasyMock.eq(LD_2016_01_15),
                EasyMock.eq(DeviceType.ALL_DEVICES)
        )).andReturn(stat);
        EasyMock.replay(statisticsService2);

        GetQueriesIndicatorRequest request = new GetQueriesIndicatorRequest();
        request.setDateFrom(LD_2016_01_01.toDateTimeAtStartOfDay());
        request.setDateTo(LD_2016_01_15.toDateTimeAtStartOfDay());
        request.setPeriod(AggregatePeriod.DAY);
        request.setIndicator(indicator);
        request.setSpecialGroup(SpecialGroup.TOP_3000_QUERIES);
        request.setHostId(hostId.toStringId());
        request.setConvertedHostId(hostId);

        QueryStatisticsResponse response = action.process(request);
        EasyMock.verify(statisticsService2);
        Assert.assertTrue(response instanceof QueryStatisticsResponse.NormalResponse);
        QueryStatisticsResponse.NormalResponse normalResponse = (QueryStatisticsResponse.NormalResponse) response;
        Assert.assertEquals(7, normalResponse.getQueriesCount().intValue());
        {
            List<QueryStatisticsResponse.DateRange> ranges = normalResponse.getRanges();
            Assert.assertEquals(6, ranges.size());
            Assert.assertEquals(LD_2016_01_01, ranges.get(0).getDateFrom());
            Assert.assertEquals(LD_2016_01_15, ranges.get(0).getDateTo());
            Assert.assertTrue(ranges.get(0).isWholeUserRange());

            Assert.assertEquals(LD_2016_01_15, ranges.get(1).getDateFrom());
            Assert.assertEquals(LD_2016_01_15, ranges.get(1).getDateTo());
            Assert.assertEquals(LD_2016_01_14, ranges.get(2).getDateFrom());
            Assert.assertEquals(LD_2016_01_14, ranges.get(2).getDateTo());
            Assert.assertEquals(LD_2016_01_13, ranges.get(3).getDateFrom());
            Assert.assertEquals(LD_2016_01_13, ranges.get(3).getDateTo());
            Assert.assertEquals(LD_2016_01_12, ranges.get(4).getDateFrom());
            Assert.assertEquals(LD_2016_01_12, ranges.get(4).getDateTo());
            Assert.assertEquals(LD_2016_01_11, ranges.get(5).getDateFrom());
            Assert.assertEquals(LD_2016_01_11, ranges.get(5).getDateTo());
        }

        List<QueryStatisticsResponse.QueryStat> statistics = normalResponse.getStatistics();
        for (QueryStatisticsResponse.QueryStat s : statistics) {
            Assert.assertEquals(indicator, s.getIndicator().getName());
            List<GroupsStatisticResponse.RangeStat> ranges = s.getIndicator().getRanges();
            Assert.assertEquals(6, ranges.size());
            {
                GroupsStatisticResponse.RangeStat r = ranges.get(0);
                Assert.assertEquals(LD_2016_01_01, r.getDateFrom());
                Assert.assertEquals(LD_2016_01_15, r.getDateTo());
                Assert.assertEquals(
                        getExpectedValue(LD_2016_01_01, LD_2016_01_15, QueryIndicator.TOTAL_SHOWS_COUNT),
                        r.getValue().longValue()
                );
            }
            {
                for (int i = 0; i < 5; i++) {
                    LocalDate d = new LocalDate(2016, 1, 15 - i);
                    GroupsStatisticResponse.RangeStat r = ranges.get(1 + i);
                    Assert.assertEquals(d, r.getDateFrom());
                    Assert.assertEquals(d, r.getDateTo());
                    Assert.assertEquals(10.0, r.getDifference(), 0.01);
                    Assert.assertEquals(getExpectedValue(d, d, QueryIndicator.TOTAL_SHOWS_COUNT), r.getValue().longValue());
                }
            }
        }
    }
}
