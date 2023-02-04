package ru.yandex.webmaster3.viewer.http.searchquery.statistic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.webmaster3.core.searchquery.OrderDirection;
import ru.yandex.webmaster3.core.searchquery.QueryId;
import ru.yandex.webmaster3.core.searchquery.QueryIndicator;
import ru.yandex.webmaster3.core.searchquery.SpecialGroup;
import ru.yandex.webmaster3.storage.searchquery.DeviceType;
import ru.yandex.webmaster3.storage.searchquery.QueryStat;
import ru.yandex.webmaster3.storage.searchquery.QueryStatisticsService2;

/**
 * @author aherman
 */
public class GetQueryListStatisticsActionTest implements TestData {
    private static final int PAGE_SIZE = 25;

    @SuppressWarnings("Duplicates")
    @Test
    public void testProcess() throws Exception {
        GetQueryListStatisticsAction action = new GetQueryListStatisticsAction();
        QueryStatisticsService2 queryStatisticsService2 = EasyMock.createMock(QueryStatisticsService2.class);
        action.setQueryStatisticsService2(queryStatisticsService2);

        EasyMock.expect(queryStatisticsService2.countQueries(
                EasyMock.eq(hostId),
                EasyMock.eq(SpecialGroup.TOP_3000_QUERIES),
                EasyMock.anyObject(),
                EasyMock.anyObject(),
                EasyMock.eq(LD_2016_01_01),
                EasyMock.eq(LD_2016_01_15),
                EasyMock.eq(DeviceType.ALL_DEVICES)
        )).andReturn(3);

        QueryId queryId1 = new QueryId(1);
        QueryId queryId2 = new QueryId(2);
        QueryId queryId3 = new QueryId(3);

        EasyMock.expect(queryStatisticsService2.getQueryIds(
                EasyMock.eq(hostId),
                EasyMock.eq(SpecialGroup.TOP_3000_QUERIES),
                EasyMock.anyObject(),
                EasyMock.anyObject(),
                EasyMock.eq(LD_2016_01_01),
                EasyMock.eq(LD_2016_01_15),
                EasyMock.eq(Collections.emptyList()),
                EasyMock.eq(DeviceType.ALL_DEVICES),
                EasyMock.eq(QueryIndicator.TOTAL_SHOWS_COUNT),
                EasyMock.eq(OrderDirection.DESC),
                EasyMock.eq(0),
                EasyMock.eq(PAGE_SIZE)
        )).andReturn(Lists.newArrayList(queryId1, queryId2, queryId3));

        List<QueryStat> queryStats = new ArrayList<>();
        queryStats.addAll(fillQueryStat(queryId1, LD_2015_12_17, LD_2016_01_15, 1));
        queryStats.addAll(fillQueryStat(queryId2, LD_2015_12_17, LD_2016_01_15, 2));
        queryStats.addAll(fillQueryStat(queryId3, LD_2015_12_17, LD_2016_01_15, 3));

        Map<QueryId, String> texts = new HashMap<>();
        texts.put(queryId1, "1");
        texts.put(queryId2, "2");
        texts.put(queryId3, "3");

        Pair<Map<QueryId, String>, List<QueryStat>> pair = Pair.of(texts, queryStats);
        EasyMock.expect(queryStatisticsService2.getQueryStatistics(
                EasyMock.eq(hostId),
                EasyMock.eq(SpecialGroup.TOP_3000_QUERIES),
                EasyMock.anyObject(),
                EasyMock.anyObject(),
                EasyMock.anyObject(),
                EasyMock.eq(Lists.newArrayList(queryId1, queryId2, queryId3)),
                EasyMock.eq(LD_2015_12_17),
                EasyMock.eq(LD_2016_01_15),
                EasyMock.eq(DeviceType.ALL_DEVICES)
        )).andReturn(pair);

        GetQueryListStatisticsRequest request = new GetQueryListStatisticsRequest();
        request.setDateFrom(LD_2016_01_01.toDateTimeAtStartOfDay());
        request.setDateTo(LD_2016_01_15.toDateTimeAtStartOfDay());
        request.setIndicator(new QueryIndicator[]{
                QueryIndicator.TOTAL_SHOWS_COUNT,
                QueryIndicator.TOTAL_CTR,
                QueryIndicator.AVERAGE_SHOW_POSITION
        });
        request.setHostId(hostId.toStringId());
        request.setConvertedHostId(hostId);
        request.setOrderBy(QueryIndicator.TOTAL_SHOWS_COUNT);
        request.setOrderDirection(OrderDirection.DESC);
        request.setP(0);
        request.setPSize(PAGE_SIZE);
        request.setSpecialGroup(SpecialGroup.TOP_3000_QUERIES);

        EasyMock.replay(queryStatisticsService2);
        GetQueryListStatisticsResponse response = action.process(request);
        EasyMock.verify(queryStatisticsService2);

        Assert.assertTrue(response instanceof GetQueryListStatisticsResponse.NormalResponse);
        GetQueryListStatisticsResponse.NormalResponse normalResponse =
                (GetQueryListStatisticsResponse.NormalResponse) response;

        Assert.assertEquals(3, normalResponse.getQueriesCount());
        Assert.assertEquals(3, normalResponse.getStatistics().size());
        {
            GetQueryListStatisticsResponse.QueryStat stat = normalResponse.getStatistics().get(0);
            Assert.assertEquals(queryId1, stat.getQuery().getId());
            List<AbstractQueryStatisticsResponse.IndicatorStats> indicatorStats = stat.getIndicators();
            Assert.assertEquals(3, indicatorStats.size());
            {
                AbstractQueryStatisticsResponse.IndicatorStats indicatorStat = indicatorStats.get(0);
                Assert.assertEquals(QueryIndicator.TOTAL_SHOWS_COUNT, indicatorStat.getName());
                List<GroupsStatisticResponse.RangeStat> rangesStat = indicatorStat.getRanges();
                Assert.assertEquals(1,  rangesStat.size());
                Assert.assertEquals(LD_2016_01_01, rangesStat.get(0).getDateFrom());
                Assert.assertEquals(LD_2016_01_15, rangesStat.get(0).getDateTo());
                double previous = getExpectedValue(LD_2015_12_17, LD_2015_12_31, QueryIndicator.TOTAL_SHOWS_COUNT, 1);
                double expected = getExpectedValue(LD_2016_01_01, LD_2016_01_15, QueryIndicator.TOTAL_SHOWS_COUNT, 1);
                Assert.assertEquals(expected, rangesStat.get(0).getValue(), 0.01);
                Assert.assertEquals(expected - previous, rangesStat.get(0).getDifference(), 0.01);
            }
            {
                AbstractQueryStatisticsResponse.IndicatorStats indicatorStat = indicatorStats.get(1);
                Assert.assertEquals(QueryIndicator.TOTAL_CTR, indicatorStat.getName());
                List<GroupsStatisticResponse.RangeStat> rangesStat = indicatorStat.getRanges();
                Assert.assertEquals(1,  rangesStat.size());
                Assert.assertEquals(LD_2016_01_01, rangesStat.get(0).getDateFrom());
                Assert.assertEquals(LD_2016_01_15, rangesStat.get(0).getDateTo());
                Assert.assertEquals(0.5, rangesStat.get(0).getValue(), 0.01);
                Assert.assertEquals(0.0, rangesStat.get(0).getDifference(), 0.01);
            }
            {
                AbstractQueryStatisticsResponse.IndicatorStats indicatorStat = indicatorStats.get(2);
                Assert.assertEquals(QueryIndicator.AVERAGE_SHOW_POSITION, indicatorStat.getName());
                List<GroupsStatisticResponse.RangeStat> rangesStat = indicatorStat.getRanges();
                Assert.assertEquals(1,  rangesStat.size());
                Assert.assertEquals(LD_2016_01_01, rangesStat.get(0).getDateFrom());
                Assert.assertEquals(LD_2016_01_15, rangesStat.get(0).getDateTo());
                Assert.assertEquals(1, rangesStat.get(0).getValue(), 0.01);
                Assert.assertEquals(0.0, rangesStat.get(0).getDifference(), 0.01);
            }
        }
        {
            GetQueryListStatisticsResponse.QueryStat stat = normalResponse.getStatistics().get(1);
            Assert.assertEquals(queryId2, stat.getQuery().getId());
            List<AbstractQueryStatisticsResponse.IndicatorStats> indicatorStats = stat.getIndicators();
            Assert.assertEquals(3, indicatorStats.size());
            {
                AbstractQueryStatisticsResponse.IndicatorStats indicatorStat = indicatorStats.get(0);
                Assert.assertEquals(QueryIndicator.TOTAL_SHOWS_COUNT, indicatorStat.getName());
                List<GroupsStatisticResponse.RangeStat> rangesStat = indicatorStat.getRanges();
                Assert.assertEquals(1,  rangesStat.size());
                Assert.assertEquals(LD_2016_01_01, rangesStat.get(0).getDateFrom());
                Assert.assertEquals(LD_2016_01_15, rangesStat.get(0).getDateTo());
                double previous = getExpectedValue(LD_2015_12_17, LD_2015_12_31, QueryIndicator.TOTAL_SHOWS_COUNT, 2);
                double expected = getExpectedValue(LD_2016_01_01, LD_2016_01_15, QueryIndicator.TOTAL_SHOWS_COUNT, 2);
                Assert.assertEquals(expected, rangesStat.get(0).getValue(), 0.01);
                Assert.assertEquals(expected - previous, rangesStat.get(0).getDifference(), 0.01);
            }
            {
                AbstractQueryStatisticsResponse.IndicatorStats indicatorStat = indicatorStats.get(1);
                Assert.assertEquals(QueryIndicator.TOTAL_CTR, indicatorStat.getName());
                List<GroupsStatisticResponse.RangeStat> rangesStat = indicatorStat.getRanges();
                Assert.assertEquals(1,  rangesStat.size());
                Assert.assertEquals(LD_2016_01_01, rangesStat.get(0).getDateFrom());
                Assert.assertEquals(LD_2016_01_15, rangesStat.get(0).getDateTo());
                Assert.assertEquals(0.5, rangesStat.get(0).getValue(), 0.01);
                Assert.assertEquals(0.0, rangesStat.get(0).getDifference(), 0.01);
            }
            {
                AbstractQueryStatisticsResponse.IndicatorStats indicatorStat = indicatorStats.get(2);
                Assert.assertEquals(QueryIndicator.AVERAGE_SHOW_POSITION, indicatorStat.getName());
                List<GroupsStatisticResponse.RangeStat> rangesStat = indicatorStat.getRanges();
                Assert.assertEquals(1,  rangesStat.size());
                Assert.assertEquals(LD_2016_01_01, rangesStat.get(0).getDateFrom());
                Assert.assertEquals(LD_2016_01_15, rangesStat.get(0).getDateTo());
                Assert.assertEquals(1, rangesStat.get(0).getValue(), 0.01);
                Assert.assertEquals(0.0, rangesStat.get(0).getDifference(), 0.01);
            }
        }
        {
            GetQueryListStatisticsResponse.QueryStat stat = normalResponse.getStatistics().get(2);
            Assert.assertEquals(queryId3, stat.getQuery().getId());
            List<AbstractQueryStatisticsResponse.IndicatorStats> indicatorStats = stat.getIndicators();
            Assert.assertEquals(3, indicatorStats.size());
            {
                AbstractQueryStatisticsResponse.IndicatorStats indicatorStat = indicatorStats.get(0);
                Assert.assertEquals(QueryIndicator.TOTAL_SHOWS_COUNT, indicatorStat.getName());
                List<GroupsStatisticResponse.RangeStat> rangesStat = indicatorStat.getRanges();
                Assert.assertEquals(1,  rangesStat.size());
                Assert.assertEquals(LD_2016_01_01, rangesStat.get(0).getDateFrom());
                Assert.assertEquals(LD_2016_01_15, rangesStat.get(0).getDateTo());
                double previous = getExpectedValue(LD_2015_12_17, LD_2015_12_31, QueryIndicator.TOTAL_SHOWS_COUNT, 3);
                double expected = getExpectedValue(LD_2016_01_01, LD_2016_01_15, QueryIndicator.TOTAL_SHOWS_COUNT, 3);
                Assert.assertEquals(expected, rangesStat.get(0).getValue(), 0.01);
                Assert.assertEquals(expected - previous, rangesStat.get(0).getDifference(), 0.01);
            }
            {
                AbstractQueryStatisticsResponse.IndicatorStats indicatorStat = indicatorStats.get(1);
                Assert.assertEquals(QueryIndicator.TOTAL_CTR, indicatorStat.getName());
                List<GroupsStatisticResponse.RangeStat> rangesStat = indicatorStat.getRanges();
                Assert.assertEquals(1,  rangesStat.size());
                Assert.assertEquals(LD_2016_01_01, rangesStat.get(0).getDateFrom());
                Assert.assertEquals(LD_2016_01_15, rangesStat.get(0).getDateTo());
                Assert.assertEquals(0.5, rangesStat.get(0).getValue(), 0.01);
                Assert.assertEquals(0.0, rangesStat.get(0).getDifference(), 0.01);
            }
            {
                AbstractQueryStatisticsResponse.IndicatorStats indicatorStat = indicatorStats.get(2);
                Assert.assertEquals(QueryIndicator.AVERAGE_SHOW_POSITION, indicatorStat.getName());
                List<GroupsStatisticResponse.RangeStat> rangesStat = indicatorStat.getRanges();
                Assert.assertEquals(1,  rangesStat.size());
                Assert.assertEquals(LD_2016_01_01, rangesStat.get(0).getDateFrom());
                Assert.assertEquals(LD_2016_01_15, rangesStat.get(0).getDateTo());
                Assert.assertEquals(1, rangesStat.get(0).getValue(), 0.01);
                Assert.assertEquals(0.0, rangesStat.get(0).getDifference(), 0.01);
            }
        }
    }
}
