package ru.yandex.webmaster3.viewer.http.searchquery.statistic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.datastax.driver.core.utils.UUIDs;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;
import org.easymock.EasyMock;
import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.webmaster3.core.searchquery.QueryGroup;
import ru.yandex.webmaster3.core.searchquery.QueryGroupId;
import ru.yandex.webmaster3.core.searchquery.QueryIndicator;
import ru.yandex.webmaster3.storage.searchquery.DeviceType;
import ru.yandex.webmaster3.storage.searchquery.GroupStat;
import ru.yandex.webmaster3.storage.searchquery.GroupStatisticsService2;
import ru.yandex.webmaster3.storage.searchquery.QueryGroupService;

/**
 * @author aherman
 */
public class GetGroupsPieChartActionTest implements TestData {
    @SuppressWarnings("Duplicates")
    @Test
    public void testProcess() throws Exception {
        QueryGroupService queryGroupService = EasyMock.createMock(QueryGroupService.class);
        GroupStatisticsService2 groupStatisticsService2 = EasyMock.createMock(GroupStatisticsService2.class);

        GetGroupsPieChartAction action = new GetGroupsPieChartAction(queryGroupService,groupStatisticsService2);

        QueryGroupId gid = new QueryGroupId(hostId, UUIDs.startOf(1000));
        QueryGroupId gid1 = new QueryGroupId(hostId, UUIDs.startOf(1001));

        EasyMock.expect(queryGroupService.getGroup(gid)).andReturn(
                new QueryGroup(gid, "Test", false, null, Instant.now(), Instant.now())
        );
        EasyMock.expect(queryGroupService.getGroup(gid1)).andReturn(
                new QueryGroup(gid1, "Test1", false, null, Instant.now(), Instant.now())
        );

        Map<QueryGroupId, List<GroupStat>> stat = new HashMap<>();
        stat.put(gid, fillGroupStat(gid, LD_2016_01_01, LD_2016_01_15, 4));
        stat.put(gid1, fillGroupStat(gid1, LD_2016_01_01, LD_2016_01_15, 3));

        EasyMock.expect(groupStatisticsService2.getStatistics(
                EasyMock.eq(hostId),
                EasyMock.eq(Pair.of(LD_2016_01_01, LD_2016_01_15)),
                EasyMock.anyObject(),
                EasyMock.anyObject(),
                EasyMock.eq(Sets.newHashSet(gid, gid1)),
                EasyMock.anyObject(),
                EasyMock.eq(DeviceType.ALL_DEVICES)
        )).andReturn(stat);

        GetGroupsPieChartRequest request = new GetGroupsPieChartRequest();
        request.setHostId(hostId.toStringId());
        request.setConvertedHostId(hostId);
        request.setDateFrom(LD_2016_01_01.toDateTimeAtStartOfDay());
        request.setDateTo(LD_2016_01_15.toDateTimeAtStartOfDay());
        request.setGroupId(new String[]{gid.getGroupId().toString(), gid1.getGroupId().toString()});
        request.setIndicator(QueryIndicator.TOTAL_SHOWS_COUNT);

        EasyMock.replay(queryGroupService, groupStatisticsService2);
        GroupsStatisticResponse response = action.process(request);
        EasyMock.verify(queryGroupService, groupStatisticsService2);

        Assert.assertTrue(response instanceof GroupsStatisticResponse.NormalResponse);
        GroupsStatisticResponse.NormalResponse normalResponse = (GroupsStatisticResponse.NormalResponse) response;
        List<GroupsStatisticResponse.GroupStat> gs = normalResponse.getStatistics();
        Assert.assertEquals(2, gs.size());
        {
            GroupsStatisticResponse.GroupStat groupStat = gs.get(0);
            Assert.assertEquals(gid.getGroupId().toString(), groupStat.getGroup().getGroupId());
            Assert.assertNotNull(groupStat.getIndicator());
            Assert.assertNull(groupStat.getIndicators());
            Assert.assertEquals(1, groupStat.getIndicator().getRanges().size());
            GroupsStatisticResponse.RangeStat rangeStat = groupStat.getIndicator().getRanges().get(0);
            Assert.assertEquals(LD_2016_01_01, rangeStat.getDateFrom());
            Assert.assertEquals(LD_2016_01_15, rangeStat.getDateTo());
            Assert.assertEquals(
                    getExpectedValue(LD_2016_01_01, LD_2016_01_15, QueryIndicator.TOTAL_SHOWS_COUNT, 4),
                    rangeStat.getValue(),
                    0.01
            );
        }
        {
            GroupsStatisticResponse.GroupStat groupStat = gs.get(1);
            Assert.assertEquals(gid1.getGroupId().toString(), groupStat.getGroup().getGroupId());
            Assert.assertNotNull(groupStat.getIndicator());
            Assert.assertNull(groupStat.getIndicators());
            Assert.assertEquals(1, groupStat.getIndicator().getRanges().size());
            GroupsStatisticResponse.RangeStat rangeStat = groupStat.getIndicator().getRanges().get(0);
            Assert.assertEquals(LD_2016_01_01, rangeStat.getDateFrom());
            Assert.assertEquals(LD_2016_01_15, rangeStat.getDateTo());
            Assert.assertEquals(
                    getExpectedValue(LD_2016_01_01, LD_2016_01_15, QueryIndicator.TOTAL_SHOWS_COUNT, 3),
                    rangeStat.getValue(),
                    0.01
            );
        }
    }
}
