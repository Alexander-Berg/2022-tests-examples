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
import ru.yandex.webmaster3.storage.searchquery.DeviceType;
import ru.yandex.webmaster3.storage.searchquery.QueryStat;
import ru.yandex.webmaster3.storage.searchquery.QueryStatisticsService2;

/**
 * @author aherman
 */
public class GetQueryPieChartActionTest implements TestData {
    @SuppressWarnings("Duplicates")
    @Test
    public void testProcess() throws Exception {
        WebmasterHostId hostId = IdUtils.urlToHostId("http://lenta.ru");

        QueryStatisticsService2 queryStatisticsService2 = EasyMock.createMock(QueryStatisticsService2.class);

        GetQueryPieChartAction action = new GetQueryPieChartAction();
        action.setQueryStatisticsService2(queryStatisticsService2);

        QueryId qid = new QueryId(1);
        QueryId qid1 = new QueryId(2);

        Map<QueryId, String> qTexts = new HashMap<>();
        qTexts.put(qid, "Test");
        qTexts.put(qid1, "Test1");
        EasyMock.expect(queryStatisticsService2.getQueryTexts(
                EasyMock.eq(hostId),
                EasyMock.eq(SpecialGroup.TOP_3000_QUERIES),
                EasyMock.eq(Lists.newArrayList(qid, qid1)))
        ).andReturn(qTexts);

        List<QueryStat> stat = new ArrayList<>();
        {
            {
                LocalDate d = LD_2016_01_01;
                int i = 4;
                while (d.isBefore(LD_2016_01_15) || d.equals(LD_2016_01_15)) {
                    stat.add(new QueryStat(d, qid, i * 10, 0, i * 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
                    i ++;
                    d = d.plusDays(1);
                }
            }
            {
                LocalDate d = LD_2016_01_01;
                int i = 3;
                while (d.isBefore(LD_2016_01_15) || d.equals(LD_2016_01_15)) {
                    stat.add(new QueryStat(d, qid1, i * 10, 0, i * 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
                    i++;
                    d = d.plusDays(1);
                }
            }
        }
        EasyMock.expect(queryStatisticsService2.getStatistics(
                EasyMock.eq(SpecialGroup.TOP_3000_QUERIES),
                EasyMock.eq(hostId),
                EasyMock.anyObject(),
                EasyMock.eq(Lists.newArrayList(qid, qid1)),
                EasyMock.anyObject(),
                EasyMock.anyObject(),
                EasyMock.eq(LD_2016_01_01),
                EasyMock.eq(LD_2016_01_15),
                EasyMock.eq(DeviceType.ALL_DEVICES)
        )).andReturn(stat);

        GetQueryPieChartRequest request = new GetQueryPieChartRequest();
        request.setHostId(hostId.toStringId());
        request.setConvertedHostId(hostId);
        request.setDateFrom(LD_2016_01_01.toDateTimeAtStartOfDay());
        request.setDateTo(LD_2016_01_15.toDateTimeAtStartOfDay());
        request.setQueryId(new String[]{qid.toString(), qid1.toString()});
        request.setIndicator(QueryIndicator.TOTAL_SHOWS_COUNT);
        request.setSpecialGroup(SpecialGroup.TOP_3000_QUERIES);

        EasyMock.replay(queryStatisticsService2);
        QueryStatisticsResponse response = action.process(request);
        EasyMock.verify(queryStatisticsService2);

        Assert.assertTrue(response instanceof QueryStatisticsResponse.NormalResponse);
        QueryStatisticsResponse.NormalResponse normalResponse = (QueryStatisticsResponse.NormalResponse) response;

        List<QueryStatisticsResponse.QueryStat> queryStats = normalResponse.getStatistics();
        Assert.assertEquals(2, queryStats.size());
        {
            QueryStatisticsResponse.QueryStat qs = queryStats.get(0);
            Assert.assertEquals(qid, qs.getQuery().getId());
            Assert.assertNotNull(qs.getIndicator());
            Assert.assertEquals(1, qs.getIndicator().getRanges().size());
            GroupsStatisticResponse.RangeStat rangeStat = qs.getIndicator().getRanges().get(0);
            Assert.assertEquals(LD_2016_01_01, rangeStat.getDateFrom());
            Assert.assertEquals(LD_2016_01_15, rangeStat.getDateTo());
            Assert.assertEquals(10 * 19 * 18 / 2 - 10 * 4 * 3 / 2, rangeStat.getValue(), 0.01);
        }
        {
            QueryStatisticsResponse.QueryStat qs = queryStats.get(1);
            Assert.assertEquals(qid1, qs.getQuery().getId());
            Assert.assertNotNull(qs.getIndicator());
            Assert.assertEquals(1, qs.getIndicator().getRanges().size());
            GroupsStatisticResponse.RangeStat rangeStat = qs.getIndicator().getRanges().get(0);
            Assert.assertEquals(LD_2016_01_01, rangeStat.getDateFrom());
            Assert.assertEquals(LD_2016_01_15, rangeStat.getDateTo());
            Assert.assertEquals(10 * 18 * 17 / 2 - 10 * 3 * 2 / 2, rangeStat.getValue(), 0.01);
        }
    }
}
