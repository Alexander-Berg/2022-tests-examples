package ru.yandex.webmaster3.viewer.http.searchquery.statistic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import org.easymock.EasyMock;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.searchquery.QueryId;
import ru.yandex.webmaster3.core.searchquery.QueryIndicator;
import ru.yandex.webmaster3.core.searchquery.SpecialGroup;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.storage.searchquery.AggregatePeriod;
import ru.yandex.webmaster3.storage.searchquery.DeviceType;
import ru.yandex.webmaster3.storage.searchquery.QueryStat;
import ru.yandex.webmaster3.storage.searchquery.QueryStatisticsService2;

/**
 * @author aherman
 */
public class GetSelectedQueryActionTest implements TestData {
    @SuppressWarnings("Duplicates")
    @Test
    public void testDay() throws Exception {
        WebmasterHostId hostId = IdUtils.urlToHostId("http://lenta.ru");
        QueryId queryId = new QueryId(1);

        QueryStatisticsService2 queryStatisticsService2 = EasyMock.createMock(QueryStatisticsService2.class);

        GetSelectedQueryAction action = new GetSelectedQueryAction();
        action.setQueryStatisticsService2(queryStatisticsService2);

        Map<QueryId, String> texts = new HashMap<>();
        texts.put(queryId, "Test");
        EasyMock.expect(queryStatisticsService2.getQueryTexts(
                EasyMock.eq(hostId),
                EasyMock.eq(SpecialGroup.TOP_3000_QUERIES),
                EasyMock.eq(Lists.newArrayList(queryId)))
        ).andReturn(texts);

        List<QueryStat> stats = new ArrayList<>();
        stats.addAll(fillQueryStat(queryId, LD_2015_12_17, LD_2016_01_15));
        EasyMock.expect(queryStatisticsService2.getStatistics(
                EasyMock.eq(SpecialGroup.TOP_3000_QUERIES),
                EasyMock.eq(hostId),
                EasyMock.anyObject(),
                EasyMock.eq(Lists.newArrayList(queryId)),
                EasyMock.anyObject(),
                EasyMock.anyObject(),
                EasyMock.eq(LD_2015_12_17),
                EasyMock.eq(LD_2016_01_15),
                EasyMock.eq(DeviceType.ALL_DEVICES)
        )).andReturn(stats);

        GetSelectedQueryRequest request = new GetSelectedQueryRequest();
        request.setDateFrom(LD_2016_01_01.toDateTimeAtStartOfDay());
        request.setDateTo(LD_2016_01_15.toDateTimeAtStartOfDay());
        request.setHostId(hostId.toStringId());
        request.setConvertedHostId(hostId);
        request.setPeriod(AggregatePeriod.DAY);
        request.setSpecialGroup(SpecialGroup.TOP_3000_QUERIES);
        request.setQueryId(queryId.toStringId());
        EasyMock.replay(queryStatisticsService2);
        QueryStatisticsResponse response = action.process(request);
        EasyMock.verify(queryStatisticsService2);

        Assert.assertTrue(response instanceof QueryStatisticsResponse.NormalResponse);
        QueryStatisticsResponse.NormalResponse normalResponse = (QueryStatisticsResponse.NormalResponse) response;
        List<QueryStatisticsResponse.DateRange> ranges = normalResponse.getRanges();
        Assert.assertEquals(6, ranges.size());
        checkRange(ranges.get(0), LD_2016_01_01, LD_2016_01_15);
        checkRange(ranges.get(1), LD_2016_01_15, LD_2016_01_15);
        checkRange(ranges.get(2), LD_2016_01_14, LD_2016_01_14);
        checkRange(ranges.get(3), LD_2016_01_13, LD_2016_01_13);
        checkRange(ranges.get(4), LD_2016_01_12, LD_2016_01_12);
        checkRange(ranges.get(5), LD_2016_01_11, LD_2016_01_11);

        List<QueryStatisticsResponse.QueryStat> queryStats = normalResponse.getStatistics();
        Assert.assertEquals(GetSelectedQueryAction.DEFAULT_INDICATORS.size(), queryStats.size());
        for (QueryStatisticsResponse.QueryStat queryStat : queryStats) {
            List<GroupsStatisticResponse.RangeStat> rangeStats = queryStat.getIndicator().getRanges();

            if (queryStat.getIndicator().getName() == QueryIndicator.TOTAL_SHOWS_COUNT) {
                checkRange(rangeStats.get(0), LD_2016_01_01, LD_2016_01_15);
                checkValue(rangeStats.get(0), LD_2016_01_01, LD_2016_01_15, LD_2015_12_17, LD_2015_12_31, QueryIndicator.TOTAL_SHOWS_COUNT);

                checkRange(rangeStats.get(1), LD_2016_01_15, LD_2016_01_15);
                checkValue(rangeStats.get(1), LD_2016_01_15, LD_2016_01_15, LD_2016_01_14, LD_2016_01_14, QueryIndicator.TOTAL_SHOWS_COUNT);

                checkRange(rangeStats.get(2), LD_2016_01_14, LD_2016_01_14);
                checkValue(rangeStats.get(2), LD_2016_01_14, LD_2016_01_14, LD_2016_01_13, LD_2016_01_13, QueryIndicator.TOTAL_SHOWS_COUNT);

                checkRange(rangeStats.get(3), LD_2016_01_13, LD_2016_01_13);
                checkValue(rangeStats.get(3), LD_2016_01_13, LD_2016_01_13, LD_2016_01_12, LD_2016_01_12, QueryIndicator.TOTAL_SHOWS_COUNT);

                checkRange(rangeStats.get(4), LD_2016_01_12, LD_2016_01_12);
                checkValue(rangeStats.get(4), LD_2016_01_12, LD_2016_01_12, LD_2016_01_11, LD_2016_01_11, QueryIndicator.TOTAL_SHOWS_COUNT);

                checkRange(rangeStats.get(5), LD_2016_01_11, LD_2016_01_11);
                checkValue(rangeStats.get(5), LD_2016_01_11, LD_2016_01_11, LD_2016_01_10, LD_2016_01_10, QueryIndicator.TOTAL_SHOWS_COUNT);
            } else if (queryStat.getIndicator().getName() == QueryIndicator.TOTAL_CTR) {
                checkRange(rangeStats.get(0), LD_2016_01_01, LD_2016_01_15);
                checkCtrIs0_5(rangeStats.get(0));

                checkRange(rangeStats.get(1), LD_2016_01_15, LD_2016_01_15);
                checkCtrIs0_5(rangeStats.get(1));

                checkRange(rangeStats.get(2), LD_2016_01_14, LD_2016_01_14);
                checkCtrIs0_5(rangeStats.get(2));

                checkRange(rangeStats.get(3), LD_2016_01_13, LD_2016_01_13);
                checkCtrIs0_5(rangeStats.get(3));

                checkRange(rangeStats.get(4), LD_2016_01_12, LD_2016_01_12);
                checkCtrIs0_5(rangeStats.get(4));

                checkRange(rangeStats.get(5), LD_2016_01_11, LD_2016_01_11);
                checkCtrIs0_5(rangeStats.get(5));
            } else if (queryStat.getIndicator().getName() == QueryIndicator.AVERAGE_SHOW_POSITION) {
                checkRange(rangeStats.get(0), LD_2016_01_01, LD_2016_01_15);
                checkPosIs1(rangeStats.get(0));

                checkRange(rangeStats.get(1), LD_2016_01_15, LD_2016_01_15);
                checkPosIs1(rangeStats.get(1));

                checkRange(rangeStats.get(2), LD_2016_01_14, LD_2016_01_14);
                checkPosIs1(rangeStats.get(2));

                checkRange(rangeStats.get(3), LD_2016_01_13, LD_2016_01_13);
                checkPosIs1(rangeStats.get(3));

                checkRange(rangeStats.get(4), LD_2016_01_12, LD_2016_01_12);
                checkPosIs1(rangeStats.get(4));

                checkRange(rangeStats.get(5), LD_2016_01_11, LD_2016_01_11);
                checkPosIs1(rangeStats.get(5));
            }
        }
    }

    @SuppressWarnings("Duplicates")
    @Test
    public void testWeek() throws Exception {
        WebmasterHostId hostId = IdUtils.urlToHostId("http://lenta.ru");
        QueryId queryId = new QueryId(1);

        QueryStatisticsService2 queryStatisticsService2 = EasyMock.createMock(QueryStatisticsService2.class);

        GetSelectedQueryAction action = new GetSelectedQueryAction();
        action.setQueryStatisticsService2(queryStatisticsService2);

        Map<QueryId, String> texts = new HashMap<>();
        texts.put(queryId, "Test");
        EasyMock.expect(queryStatisticsService2.getQueryTexts(
                EasyMock.eq(hostId),
                EasyMock.eq(SpecialGroup.TOP_3000_QUERIES),
                EasyMock.eq(Lists.newArrayList(queryId))
        )).andReturn(texts);

        List<QueryStat> stats = new ArrayList<>();
        stats.addAll(fillQueryStat(queryId, LD_2016_01_01, LD_2016_01_15));
        EasyMock.expect(queryStatisticsService2.getStatistics(
                EasyMock.eq(SpecialGroup.TOP_3000_QUERIES),
                EasyMock.eq(hostId),
                EasyMock.anyObject(),
                EasyMock.eq(Lists.newArrayList(queryId)),
                EasyMock.anyObject(),
                EasyMock.anyObject(),
                EasyMock.eq(LD_2015_12_07),
                EasyMock.eq(LD_2016_01_15),
                EasyMock.eq(DeviceType.ALL_DEVICES)
        )).andReturn(stats);

        GetSelectedQueryRequest request = new GetSelectedQueryRequest();
        request.setDateFrom(LD_2016_01_01.toDateTimeAtStartOfDay());
        request.setDateTo(LD_2016_01_15.toDateTimeAtStartOfDay());
        request.setHostId(hostId.toStringId());
        request.setConvertedHostId(hostId);
        request.setPeriod(AggregatePeriod.WEEK);
        request.setSpecialGroup(SpecialGroup.TOP_3000_QUERIES);
        request.setQueryId(queryId.toStringId());

        EasyMock.replay(queryStatisticsService2);
        QueryStatisticsResponse response = action.process(request);
        EasyMock.verify(queryStatisticsService2);

        Assert.assertTrue(response instanceof QueryStatisticsResponse.NormalResponse);
        QueryStatisticsResponse.NormalResponse normalResponse = (QueryStatisticsResponse.NormalResponse) response;
        List<QueryStatisticsResponse.DateRange> ranges = normalResponse.getRanges();
        Assert.assertEquals(6, ranges.size());
        checkRange(ranges.get(0), LD_2016_01_01, LD_2016_01_15);
        checkRange(ranges.get(1), LD_2016_01_11, LD_2016_01_15);
        checkRange(ranges.get(2), LD_2016_01_04, LD_2016_01_10);
        checkRange(ranges.get(3), LD_2015_12_28, LD_2016_01_03);
        checkRange(ranges.get(4), LD_2015_12_21, LD_2015_12_27);
        checkRange(ranges.get(5), LD_2015_12_14, LD_2015_12_20);

        List<QueryStatisticsResponse.QueryStat> queryStats = normalResponse.getStatistics();
        Assert.assertEquals(GetSelectedQueryAction.DEFAULT_INDICATORS.size(), queryStats.size());
        for (QueryStatisticsResponse.QueryStat queryStat : queryStats) {
            List<GroupsStatisticResponse.RangeStat> rangeStats = queryStat.getIndicator().getRanges();
            if (queryStat.getIndicator().getName() == QueryIndicator.TOTAL_SHOWS_COUNT) {
                {
                    GroupsStatisticResponse.RangeStat rangeStat = rangeStats.get(0);
                    checkRange(rangeStat, LD_2016_01_01, LD_2016_01_15);
                    int currentValue = getExpectedValue(LD_2016_01_01, LD_2016_01_15, QueryIndicator.TOTAL_SHOWS_COUNT);
                    Assert.assertEquals(currentValue, rangeStat.getValue(), 0.01);
                    Assert.assertEquals(currentValue, rangeStat.getDifference(), 0.01);
                }
                {
                    GroupsStatisticResponse.RangeStat rangeStat = rangeStats.get(1);
                    checkRange(rangeStat, LD_2016_01_11, LD_2016_01_15);
                    int previousValue = getExpectedValue(LD_2016_01_04, LD_2016_01_10, QueryIndicator.TOTAL_SHOWS_COUNT);
                    int currentValue = getExpectedValue(LD_2016_01_11, LD_2016_01_15, QueryIndicator.TOTAL_SHOWS_COUNT);
                    Assert.assertEquals(currentValue, rangeStat.getValue(), 0.01);
                    Assert.assertEquals(currentValue - previousValue, rangeStat.getDifference(), 0.01);
                }
                {
                    GroupsStatisticResponse.RangeStat rangeStat = rangeStats.get(2);
                    checkRange(rangeStat, LD_2016_01_04, LD_2016_01_10);
                    int previousValue = getExpectedValue(LD_2016_01_01, LD_2016_01_03, QueryIndicator.TOTAL_SHOWS_COUNT);
                    int currentValue = getExpectedValue(LD_2016_01_04, LD_2016_01_10, QueryIndicator.TOTAL_SHOWS_COUNT);
                    Assert.assertEquals(currentValue, rangeStat.getValue(), 0.01);
                    Assert.assertEquals(currentValue - previousValue, rangeStat.getDifference(), 0.01);
                }
                {
                    GroupsStatisticResponse.RangeStat rangeStat = rangeStats.get(3);
                    checkRange(rangeStat, LD_2015_12_28, LD_2016_01_03);
                    int currentValue = getExpectedValue(LD_2016_01_01, LD_2016_01_03, QueryIndicator.TOTAL_SHOWS_COUNT);
                    Assert.assertEquals(currentValue, rangeStat.getValue(), 0.01);
                    Assert.assertEquals(currentValue, rangeStat.getDifference(), 0.01);
                }
                {
                    GroupsStatisticResponse.RangeStat rangeStat = rangeStats.get(4);
                    checkRange(rangeStat, LD_2015_12_21, LD_2015_12_27);
                    Assert.assertEquals(0.0, rangeStat.getValue(), 0.01);
                    Assert.assertEquals(0.0, rangeStat.getDifference(), 0.01);
                }
                {
                    GroupsStatisticResponse.RangeStat rangeStat = rangeStats.get(5);
                    checkRange(rangeStat, LD_2015_12_14, LD_2015_12_20);
                    Assert.assertEquals(0.0, rangeStat.getValue(), 0.01);
                    Assert.assertEquals(0.0, rangeStat.getDifference(), 0.01);
                }
            } else if (queryStat.getIndicator().getName() == QueryIndicator.TOTAL_CTR) {
                {
                    GroupsStatisticResponse.RangeStat rangeStat = rangeStats.get(0);
                    checkRange(rangeStat, LD_2016_01_01, LD_2016_01_15);
                    Assert.assertEquals(0.5, rangeStat.getValue(), 0.01);
                    Assert.assertNull(rangeStat.getDifference());
                }
                {
                    GroupsStatisticResponse.RangeStat rangeStat = rangeStats.get(1);
                    checkRange(rangeStat, LD_2016_01_11, LD_2016_01_15);
                    Assert.assertEquals(0.5, rangeStat.getValue(), 0.01);
                    Assert.assertEquals(0.0, rangeStat.getDifference(), 0.01);
                }
                {
                    GroupsStatisticResponse.RangeStat rangeStat = rangeStats.get(2);
                    checkRange(rangeStat, LD_2016_01_04, LD_2016_01_10);
                    Assert.assertEquals(0.5, rangeStat.getValue(), 0.01);
                    Assert.assertEquals(0.0, rangeStat.getDifference(), 0.01);
                }
                {
                    GroupsStatisticResponse.RangeStat rangeStat = rangeStats.get(3);
                    checkRange(rangeStat, LD_2015_12_28, LD_2016_01_03);
                    Assert.assertEquals(0.5, rangeStat.getValue(), 0.01);
                    Assert.assertNull(rangeStat.getDifference());
                }
                {
                    GroupsStatisticResponse.RangeStat rangeStat = rangeStats.get(4);
                    checkRange(rangeStat, LD_2015_12_21, LD_2015_12_27);
                    Assert.assertNull(rangeStat.getValue());
                    Assert.assertNull(rangeStat.getDifference());
                }
                {
                    GroupsStatisticResponse.RangeStat rangeStat = rangeStats.get(5);
                    checkRange(rangeStat, LD_2015_12_14, LD_2015_12_20);
                    Assert.assertNull(rangeStat.getValue());
                    Assert.assertNull(rangeStat.getDifference());
                }
            } else if (queryStat.getIndicator().getName() == QueryIndicator.AVERAGE_SHOW_POSITION) {
                {
                    GroupsStatisticResponse.RangeStat rangeStat = rangeStats.get(0);
                    checkRange(rangeStat, LD_2016_01_01, LD_2016_01_15);
                    Assert.assertEquals(1, rangeStat.getValue(), 0.01);
                    Assert.assertNull(rangeStat.getDifference());
                }
                {
                    GroupsStatisticResponse.RangeStat rangeStat = rangeStats.get(1);
                    checkRange(rangeStat, LD_2016_01_11, LD_2016_01_15);
                    Assert.assertEquals(1, rangeStat.getValue(), 0.01);
                    Assert.assertEquals(0, rangeStat.getDifference(), 0.01);
                }
                {
                    GroupsStatisticResponse.RangeStat rangeStat = rangeStats.get(2);
                    checkRange(rangeStat, LD_2016_01_04, LD_2016_01_10);
                    Assert.assertEquals(1, rangeStat.getValue(), 0.01);
                    Assert.assertEquals(0, rangeStat.getDifference(), 0.01);
                }
                {
                    GroupsStatisticResponse.RangeStat rangeStat = rangeStats.get(3);
                    checkRange(rangeStat, LD_2015_12_28, LD_2016_01_03);
                    Assert.assertEquals(1, rangeStat.getValue(), 0.01);
                    Assert.assertNull(rangeStat.getDifference());
                }
                {
                    GroupsStatisticResponse.RangeStat rangeStat = rangeStats.get(4);
                    checkRange(rangeStat, LD_2015_12_21, LD_2015_12_27);
                    Assert.assertNull(rangeStat.getValue());
                    Assert.assertNull(rangeStat.getDifference());
                }
                {
                    GroupsStatisticResponse.RangeStat rangeStat = rangeStats.get(5);
                    checkRange(rangeStat, LD_2015_12_14, LD_2015_12_20);
                    Assert.assertNull(rangeStat.getValue());
                    Assert.assertNull(rangeStat.getDifference());
                }
            }
        }
    }

    private void checkCtrIs0_5(GroupsStatisticResponse.RangeStat rangeStat) {
        Assert.assertEquals(0.5, rangeStat.getValue(), 0.01);
        Assert.assertEquals(0, rangeStat.getDifference(), 0.01);
    }

    private void checkPosIs1(GroupsStatisticResponse.RangeStat rangeStat) {
        Assert.assertEquals(1, rangeStat.getValue(), 0.01);
        Assert.assertEquals(0, rangeStat.getDifference(), 0.01);
    }

    private void checkValue(GroupsStatisticResponse.RangeStat rangeStat, LocalDate dateFrom, LocalDate dateTo,
            LocalDate previousDateFrom, LocalDate previousDateTo, QueryIndicator queryIndicator) {
        int currentValue = getExpectedValue(dateFrom, dateTo, queryIndicator);
        int previousValue = getExpectedValue(previousDateFrom, previousDateTo, queryIndicator);
        Assert.assertEquals(currentValue, rangeStat.getValue(), 0.01);
        Assert.assertEquals(currentValue - previousValue, rangeStat.getDifference(), 0.01);
    }

    private void checkRange(QueryStatisticsResponse.DateRange dateRange, LocalDate dateFrom, LocalDate dateTo) {
        Assert.assertEquals(dateFrom, dateRange.getDateFrom());
        Assert.assertEquals(dateTo, dateRange.getDateTo());
    }

    private void checkRange(GroupsStatisticResponse.RangeStat rangeStat, LocalDate dateFrom, LocalDate dateTo) {
        Assert.assertEquals(dateFrom, rangeStat.getDateFrom());
        Assert.assertEquals(dateTo, rangeStat.getDateTo());
    }
}
