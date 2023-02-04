package ru.yandex.webmaster3.viewer.http.searchquery.statistic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.datastax.driver.core.utils.UUIDs;
import org.apache.commons.lang3.tuple.Pair;
import org.easymock.EasyMock;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.webmaster3.core.http.ActionStatus;
import ru.yandex.webmaster3.core.searchquery.QueryGroup;
import ru.yandex.webmaster3.core.searchquery.QueryGroupId;
import ru.yandex.webmaster3.core.searchquery.QueryIndicator;
import ru.yandex.webmaster3.storage.searchquery.AggregatePeriod;
import ru.yandex.webmaster3.storage.searchquery.DeviceType;
import ru.yandex.webmaster3.storage.searchquery.GroupStat;
import ru.yandex.webmaster3.storage.searchquery.GroupStatisticsService2;
import ru.yandex.webmaster3.storage.searchquery.QueryGroupService;
import ru.yandex.webmaster3.storage.searchquery.RegionInclusion;

/**
 * @author aherman
 */
public class GetGroupHistoryActionTest implements TestData {
    private UUID groupUUID;
    private GetGroupHistoryAction action;
    private GroupStatisticsService2 groupStatisticsService2;
    private QueryGroupService queryGroupService;

    @Before
    public void setUp() throws Exception {


        groupUUID = UUIDs.startOf(1000);
        QueryGroupId groupId = QueryGroupId.byGroupIdStr(hostId, groupUUID);
        queryGroupService = EasyMock.createMock(QueryGroupService.class);
        EasyMock.expect(queryGroupService.getGroup(groupId))
                .andReturn(new QueryGroup(groupId, "Test", false, null, null, null));

        groupStatisticsService2 = EasyMock.createMock(GroupStatisticsService2.class);
        action = new GetGroupHistoryAction(queryGroupService, groupStatisticsService2);

        Map<QueryGroupId, List<GroupStat>> stats = new HashMap<>();
        stats.put(groupId, fillGroupStat(groupId, LD_2016_01_01, LD_2016_01_15));

        EasyMock.expect(groupStatisticsService2.getStatistics(
                EasyMock.eq(hostId),
                EasyMock.eq(Pair.of(LD_2016_01_01, LD_2016_01_15)),
                EasyMock.eq(RegionInclusion.INCLUDE_ALL),
                EasyMock.anyObject(),
                EasyMock.anyObject(),
                EasyMock.anyObject(),
                EasyMock.eq(DeviceType.ALL_DEVICES)
        )).andReturn(stats);

        EasyMock.replay(queryGroupService, groupStatisticsService2);
    }

    @SuppressWarnings("Duplicates")
    @Test
    public void testDayHistory() throws Exception {
        GetGroupHistoryRequest request = new GetGroupHistoryRequest();
        request.setUserId(100L);
        request.setDateFrom(LD_2016_01_01.toDateTimeAtStartOfDay());
        request.setDateTo(LD_2016_01_15.toDateTimeAtStartOfDay());

        request.setGroupId(new String[]{groupUUID.toString()});
        request.setHostId(hostId.toStringId());
        request.setConvertedHostId(hostId);
        request.setIndicator(new QueryIndicator[]{
                QueryIndicator.TOTAL_SHOWS_COUNT,
                QueryIndicator.TOTAL_CTR,
                QueryIndicator.AVERAGE_SHOW_POSITION
        });
        request.setPeriod(AggregatePeriod.DAY);

        GroupsStatisticResponse response = action.process(request);
        EasyMock.verify(queryGroupService, groupStatisticsService2);

        Assert.assertNotNull(response);
        Assert.assertEquals(ActionStatus.SUCCESS, response.getRequestStatus());
        Assert.assertTrue(response instanceof GroupsStatisticResponse.NormalResponse);
        GroupsStatisticResponse.NormalResponse normalResponse = (GroupsStatisticResponse.NormalResponse) response;

        Assert.assertNull(normalResponse.getRanges());

        List<GroupsStatisticResponse.GroupStat> statistics = normalResponse.getStatistics();
        Assert.assertEquals(3, statistics.size());

        {
            Optional<GroupsStatisticResponse.GroupStat> tscO =
                    statistics.stream()
                            .filter(gs -> gs.getIndicator().getName() == QueryIndicator.TOTAL_SHOWS_COUNT)
                            .findFirst();
            Assert.assertTrue(tscO.isPresent());
            GroupsStatisticResponse.GroupStat tsc = tscO.get();
            List<GroupsStatisticResponse.RangeStat> tscRanges = tsc.getIndicator().getRanges();
            Assert.assertEquals(15, tscRanges.size());
            int i = 1;
            LocalDate d = LD_2016_01_01;
            while (d.isBefore(LD_2016_01_15) || d.equals(LD_2016_01_15)) {
                GroupsStatisticResponse.RangeStat rs = tscRanges.get(i - 1);
                Assert.assertEquals(d, rs.getDateFrom());
                Assert.assertEquals(d, rs.getDateTo());
                Assert.assertEquals(i * 10.0, rs.getValue(), 0.001);
                i++;
                d = d.plusDays(1);
            }
        }

        {
            Optional<GroupsStatisticResponse.GroupStat> tscO =
                    statistics.stream()
                            .filter(gs -> gs.getIndicator().getName() == QueryIndicator.TOTAL_CTR)
                            .findFirst();
            Assert.assertTrue(tscO.isPresent());
            GroupsStatisticResponse.GroupStat tsc = tscO.get();
            List<GroupsStatisticResponse.RangeStat> tscRanges = tsc.getIndicator().getRanges();
            Assert.assertEquals(15, tscRanges.size());
            LocalDate d = LD_2016_01_01;
            int i = 1;
            while (d.isBefore(LD_2016_01_15) || d.equals(LD_2016_01_15)) {
                GroupsStatisticResponse.RangeStat rs = tscRanges.get(i - 1);
                Assert.assertEquals(d, rs.getDateFrom());
                Assert.assertEquals(d, rs.getDateTo());
                Assert.assertEquals(0.5, rs.getValue(), 0.001);
                i++;
                d = d.plusDays(1);
            }
        }

        {
            Optional<GroupsStatisticResponse.GroupStat> tscO =
                    statistics.stream()
                            .filter(gs -> gs.getIndicator().getName() == QueryIndicator.AVERAGE_SHOW_POSITION)
                            .findFirst();
            Assert.assertTrue(tscO.isPresent());
            GroupsStatisticResponse.GroupStat tsc = tscO.get();
            List<GroupsStatisticResponse.RangeStat> tscRanges = tsc.getIndicator().getRanges();
            Assert.assertEquals(15, tscRanges.size());
            LocalDate d = LD_2016_01_01;
            int i = 1;
            while (d.isBefore(LD_2016_01_15) || d.equals(LD_2016_01_15)) {
                GroupsStatisticResponse.RangeStat rs = tscRanges.get(i - 1);
                Assert.assertEquals(d, rs.getDateFrom());
                Assert.assertEquals(d, rs.getDateTo());
                Assert.assertEquals(1.0, rs.getValue(), 0.001);
                i++;
                d = d.plusDays(1);
            }
        }

        {
            Optional<GroupsStatisticResponse.GroupStat> tscO =
                    statistics.stream()
                            .filter(gs -> gs.getIndicator().getName() == QueryIndicator.TOTAL_CLICKS_COUNT)
                            .findFirst();
            Assert.assertFalse(tscO.isPresent());
        }
    }

    @SuppressWarnings("Duplicates")
    @Test
    public void testWeekHistory() throws Exception {
        GetGroupHistoryRequest request = new GetGroupHistoryRequest();
        request.setUserId(100L);
        request.setDateFrom(LD_2016_01_01.toDateTimeAtStartOfDay());
        request.setDateTo(LD_2016_01_15.toDateTimeAtStartOfDay());

        request.setGroupId(new String[]{groupUUID.toString()});
        request.setHostId(hostId.toStringId());
        request.setConvertedHostId(hostId);
        request.setIndicator(new QueryIndicator[]{
                QueryIndicator.TOTAL_SHOWS_COUNT,
                QueryIndicator.TOTAL_CTR,
                QueryIndicator.AVERAGE_SHOW_POSITION
        });
        request.setPeriod(AggregatePeriod.WEEK);

        GroupsStatisticResponse response = action.process(request);
        EasyMock.verify(queryGroupService, groupStatisticsService2);

        Assert.assertNotNull(response);
        Assert.assertEquals(ActionStatus.SUCCESS, response.getRequestStatus());
        Assert.assertTrue(response instanceof GroupsStatisticResponse.NormalResponse);
        GroupsStatisticResponse.NormalResponse normalResponse = (GroupsStatisticResponse.NormalResponse) response;

        Assert.assertNull(normalResponse.getRanges());

        List<GroupsStatisticResponse.GroupStat> statistics = normalResponse.getStatistics();
        Assert.assertEquals(3, statistics.size());

        {
            Optional<GroupsStatisticResponse.GroupStat> tscO =
                    statistics.stream()
                            .filter(gs -> gs.getIndicator().getName() == QueryIndicator.TOTAL_SHOWS_COUNT)
                            .findFirst();
            Assert.assertTrue(tscO.isPresent());
            GroupsStatisticResponse.GroupStat tsc = tscO.get();
            List<GroupsStatisticResponse.RangeStat> tscRanges = tsc.getIndicator().getRanges();
            Assert.assertEquals(3, tscRanges.size());

            GroupsStatisticResponse.RangeStat rs = tscRanges.get(0);
            Assert.assertEquals(LD_2016_01_01, rs.getDateFrom());
            Assert.assertEquals(LD_2016_01_03, rs.getDateTo());
            Assert.assertEquals(getExpectedValue(LD_2016_01_01, LD_2016_01_03, QueryIndicator.TOTAL_SHOWS_COUNT), rs.getValue(), 0.01);

            rs = tscRanges.get(1);
            Assert.assertEquals(LD_2016_01_04, rs.getDateFrom());
            Assert.assertEquals(LD_2016_01_10, rs.getDateTo());
            Assert.assertEquals(getExpectedValue(LD_2016_01_04, LD_2016_01_10, QueryIndicator.TOTAL_SHOWS_COUNT), rs.getValue(), 0.01);

            rs = tscRanges.get(2);
            Assert.assertEquals(LD_2016_01_11, rs.getDateFrom());
            Assert.assertEquals(LD_2016_01_15, rs.getDateTo());
            Assert.assertEquals(getExpectedValue(LD_2016_01_11, LD_2016_01_15, QueryIndicator.TOTAL_SHOWS_COUNT), rs.getValue(), 0.01);
        }

        {
            Optional<GroupsStatisticResponse.GroupStat> tscO =
                    statistics.stream()
                            .filter(gs -> gs.getIndicator().getName() == QueryIndicator.TOTAL_CTR)
                            .findFirst();
            Assert.assertTrue(tscO.isPresent());
            GroupsStatisticResponse.GroupStat tsc = tscO.get();
            List<GroupsStatisticResponse.RangeStat> tscRanges = tsc.getIndicator().getRanges();
            Assert.assertEquals(3, tscRanges.size());

            GroupsStatisticResponse.RangeStat rs = tscRanges.get(0);
            Assert.assertEquals(LD_2016_01_01, rs.getDateFrom());
            Assert.assertEquals(LD_2016_01_03, rs.getDateTo());
            Assert.assertEquals(0.5, rs.getValue(), 0.01);

            rs = tscRanges.get(1);
            Assert.assertEquals(LD_2016_01_04, rs.getDateFrom());
            Assert.assertEquals(LD_2016_01_10, rs.getDateTo());
            Assert.assertEquals(0.5, rs.getValue(), 0.01);

            rs = tscRanges.get(2);
            Assert.assertEquals(LD_2016_01_11, rs.getDateFrom());
            Assert.assertEquals(LD_2016_01_15, rs.getDateTo());
            Assert.assertEquals(0.5, rs.getValue(), 0.01);
        }

        {
            Optional<GroupsStatisticResponse.GroupStat> tscO =
                    statistics.stream()
                            .filter(gs -> gs.getIndicator().getName() == QueryIndicator.AVERAGE_SHOW_POSITION)
                            .findFirst();
            Assert.assertTrue(tscO.isPresent());
            GroupsStatisticResponse.GroupStat tsc = tscO.get();
            List<GroupsStatisticResponse.RangeStat> tscRanges = tsc.getIndicator().getRanges();
            Assert.assertEquals(3, tscRanges.size());

            GroupsStatisticResponse.RangeStat rs = tscRanges.get(0);
            Assert.assertEquals(LD_2016_01_01, rs.getDateFrom());
            Assert.assertEquals(LD_2016_01_03, rs.getDateTo());
            Assert.assertEquals(1.0, rs.getValue(), 0.01);

            rs = tscRanges.get(1);
            Assert.assertEquals(LD_2016_01_04, rs.getDateFrom());
            Assert.assertEquals(LD_2016_01_10, rs.getDateTo());
            Assert.assertEquals(1.0, rs.getValue(), 0.01);

            rs = tscRanges.get(2);
            Assert.assertEquals(LD_2016_01_11, rs.getDateFrom());
            Assert.assertEquals(LD_2016_01_15, rs.getDateTo());
            Assert.assertEquals(1.0, rs.getValue(), 0.01);
        }

        {
            Optional<GroupsStatisticResponse.GroupStat> tscO =
                    statistics.stream()
                            .filter(gs -> gs.getIndicator().getName() == QueryIndicator.TOTAL_CLICKS_COUNT)
                            .findFirst();
            Assert.assertFalse(tscO.isPresent());
        }
    }
}
