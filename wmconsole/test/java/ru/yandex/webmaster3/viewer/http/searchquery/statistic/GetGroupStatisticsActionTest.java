package ru.yandex.webmaster3.viewer.http.searchquery.statistic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.datastax.driver.core.utils.UUIDs;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;
import org.easymock.EasyMock;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.webmaster3.core.searchquery.QueryGroup;
import ru.yandex.webmaster3.core.searchquery.QueryGroupId;
import ru.yandex.webmaster3.core.searchquery.QueryIndicator;
import ru.yandex.webmaster3.core.searchquery.SpecialGroup;
import ru.yandex.webmaster3.storage.searchquery.AggregatePeriod;
import ru.yandex.webmaster3.storage.searchquery.DeviceType;
import ru.yandex.webmaster3.storage.searchquery.GroupStat;
import ru.yandex.webmaster3.storage.searchquery.GroupStatisticsService2;
import ru.yandex.webmaster3.storage.searchquery.QueryGroupService;

/**
 * @author aherman
 */
public class GetGroupStatisticsActionTest implements TestData {
    public static final int G1_OFFSET = 1;
    public static final int G2_OFFSET = 2;
    public static final int G3_OFFSET = 3;
    public static final int G_ALL_OFFSET = 4;
    public static final int G_SELECTED_OFFSET = 5;

    @Test
    public void testAllGroups() throws Exception {
        GroupStatisticsService2 groupStatisticsService2 = EasyMock.createMock(GroupStatisticsService2.class);
        QueryGroupService queryGroupService = EasyMock.createMock(QueryGroupService.class);
        GetGroupStatisticsAction action = new GetGroupStatisticsAction(queryGroupService, groupStatisticsService2);
        action.setMode(GetGroupStatisticsAction.Mode.ALL_GROUPS__ONE_INDICATOR__TIME_RANGES);
        QueryGroupId gid1 = new QueryGroupId(hostId, UUIDs.startOf(1001));
        QueryGroupId gid2 = new QueryGroupId(hostId, UUIDs.startOf(1002));
        QueryGroupId gid3 = new QueryGroupId(hostId, UUIDs.startOf(1003));
        QueryGroupId allgid = new QueryGroupId(hostId, SpecialGroup.ALL_QUERIES);
        QueryGroupId selectecgid = new QueryGroupId(hostId, SpecialGroup.SELECTED_QUERIES);

        EasyMock.expect(queryGroupService.listGroups(
                EasyMock.eq(hostId)
        )).andReturn(
                Lists.newArrayList(
                        new QueryGroup(gid1, "1", false, null, Instant.now(), Instant.now()),
                        new QueryGroup(gid2, "2", false, null, Instant.now(), Instant.now()),
                        new QueryGroup(gid3, "3", false, null, Instant.now(), Instant.now())
                )
        );

        Map<QueryGroupId, List<GroupStat>> groupStatsMap = new HashMap<>();
        groupStatsMap.put(gid1, fillGroupStat(gid1, LD_2016_01_08, LD_2016_01_15, G1_OFFSET));
        groupStatsMap.put(gid2, fillGroupStat(gid1, LD_2016_01_08, LD_2016_01_15, G2_OFFSET));
        groupStatsMap.put(gid3, fillGroupStat(gid1, LD_2016_01_08, LD_2016_01_15, G3_OFFSET));
        groupStatsMap.put(allgid, fillGroupStat(allgid, LD_2016_01_08, LD_2016_01_15, G_ALL_OFFSET));
        groupStatsMap.put(selectecgid, fillGroupStat(selectecgid, LD_2016_01_08, LD_2016_01_15, G_SELECTED_OFFSET));

        EasyMock.expect(groupStatisticsService2.getStatistics(
                EasyMock.eq(hostId),
                EasyMock.eq(Pair.of(LD_2016_01_08, LD_2016_01_15)),
                EasyMock.anyObject(),
                EasyMock.anyObject(),
                EasyMock.eq(Sets.newHashSet(gid1, gid2, gid3, allgid, selectecgid)),
                EasyMock.anyObject(),
                EasyMock.eq(DeviceType.ALL_DEVICES)
        )).andReturn(groupStatsMap);

        GetGroupStatisticsRequest request = new GetGroupStatisticsRequest();
        request.setUserId(100L);
        request.setDateFrom(LD_2016_01_12.toDateTimeAtStartOfDay());
        request.setDateTo(LD_2016_01_15.toDateTimeAtStartOfDay());
        request.setPeriod(AggregatePeriod.DAY);
        request.setIndicator(new QueryIndicator[]{QueryIndicator.TOTAL_SHOWS_COUNT});
        request.setConvertedHostId(hostId);
        request.setHostId(hostId.toStringId());

        EasyMock.replay(groupStatisticsService2, queryGroupService);
        GroupsStatisticResponse response = action.process(request);
        EasyMock.verify(groupStatisticsService2, queryGroupService);

        Assert.assertTrue(response instanceof GroupsStatisticResponse.NormalResponse);

        GroupsStatisticResponse.NormalResponse normalResponse = (GroupsStatisticResponse.NormalResponse) response;
        List<GroupsStatisticResponse.DateRange> ranges = normalResponse.getRanges();
        Assert.assertEquals(6, ranges.size());
        Assert.assertEquals(LD_2016_01_12, ranges.get(0).getDateFrom());
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

        List<GroupsStatisticResponse.GroupStat> stats = normalResponse.getStatistics();
        Assert.assertEquals(5, stats.size());
        {
            GroupsStatisticResponse.GroupStat groupStat = stats.get(0);
            Assert.assertEquals(SpecialGroup.ALL_QUERIES.name(), groupStat.getGroup().getGroupId());
            Assert.assertEquals(QueryIndicator.TOTAL_SHOWS_COUNT, groupStat.getIndicator().getName());
            List<GroupsStatisticResponse.RangeStat> rangesStat = groupStat.getIndicator().getRanges();
            Assert.assertEquals(6, rangesStat.size());

            checkTotalShowsInRange(rangesStat.get(0), LD_2016_01_12, LD_2016_01_15, LD_2016_01_08, LD_2016_01_11, G_ALL_OFFSET);
            checkTotalShowsInRange(rangesStat.get(1), LD_2016_01_15, LD_2016_01_15, LD_2016_01_14, LD_2016_01_14, G_ALL_OFFSET);
            checkTotalShowsInRange(rangesStat.get(2), LD_2016_01_14, LD_2016_01_14, LD_2016_01_13, LD_2016_01_13, G_ALL_OFFSET);
            checkTotalShowsInRange(rangesStat.get(3), LD_2016_01_13, LD_2016_01_13, LD_2016_01_12, LD_2016_01_12, G_ALL_OFFSET);
            checkTotalShowsInRange(rangesStat.get(4), LD_2016_01_12, LD_2016_01_12, LD_2016_01_11, LD_2016_01_11, G_ALL_OFFSET);
            checkTotalShowsInRange(rangesStat.get(5), LD_2016_01_11, LD_2016_01_11, LD_2016_01_10, LD_2016_01_10, G_ALL_OFFSET);
        }
        {
            GroupsStatisticResponse.GroupStat groupStat = stats.get(1);
            Assert.assertEquals(SpecialGroup.SELECTED_QUERIES.name(), groupStat.getGroup().getGroupId());
            Assert.assertEquals(QueryIndicator.TOTAL_SHOWS_COUNT, groupStat.getIndicator().getName());
            List<GroupsStatisticResponse.RangeStat> rangesStat = groupStat.getIndicator().getRanges();
            Assert.assertEquals(6, rangesStat.size());

            checkTotalShowsInRange(rangesStat.get(0), LD_2016_01_12, LD_2016_01_15, LD_2016_01_08, LD_2016_01_11, G_SELECTED_OFFSET);
            checkTotalShowsInRange(rangesStat.get(1), LD_2016_01_15, LD_2016_01_15, LD_2016_01_14, LD_2016_01_14, G_SELECTED_OFFSET);
            checkTotalShowsInRange(rangesStat.get(2), LD_2016_01_14, LD_2016_01_14, LD_2016_01_13, LD_2016_01_13, G_SELECTED_OFFSET);
            checkTotalShowsInRange(rangesStat.get(3), LD_2016_01_13, LD_2016_01_13, LD_2016_01_12, LD_2016_01_12, G_SELECTED_OFFSET);
            checkTotalShowsInRange(rangesStat.get(4), LD_2016_01_12, LD_2016_01_12, LD_2016_01_11, LD_2016_01_11, G_SELECTED_OFFSET);
            checkTotalShowsInRange(rangesStat.get(5), LD_2016_01_11, LD_2016_01_11, LD_2016_01_10, LD_2016_01_10, G_SELECTED_OFFSET);
        }
        {
            GroupsStatisticResponse.GroupStat groupStat = stats.get(2);
            Assert.assertEquals(gid3.getGroupId().toString(), groupStat.getGroup().getGroupId());
            Assert.assertEquals(QueryIndicator.TOTAL_SHOWS_COUNT, groupStat.getIndicator().getName());
            List<GroupsStatisticResponse.RangeStat> rangesStat = groupStat.getIndicator().getRanges();
            Assert.assertEquals(6, rangesStat.size());

            checkTotalShowsInRange(rangesStat.get(0), LD_2016_01_12, LD_2016_01_15, LD_2016_01_08, LD_2016_01_11, G3_OFFSET);
            checkTotalShowsInRange(rangesStat.get(1), LD_2016_01_15, LD_2016_01_15, LD_2016_01_14, LD_2016_01_14, G3_OFFSET);
            checkTotalShowsInRange(rangesStat.get(2), LD_2016_01_14, LD_2016_01_14, LD_2016_01_13, LD_2016_01_13, G3_OFFSET);
            checkTotalShowsInRange(rangesStat.get(3), LD_2016_01_13, LD_2016_01_13, LD_2016_01_12, LD_2016_01_12, G3_OFFSET);
            checkTotalShowsInRange(rangesStat.get(4), LD_2016_01_12, LD_2016_01_12, LD_2016_01_11, LD_2016_01_11, G3_OFFSET);
            checkTotalShowsInRange(rangesStat.get(5), LD_2016_01_11, LD_2016_01_11, LD_2016_01_10, LD_2016_01_10, G3_OFFSET);
        }
        {
            GroupsStatisticResponse.GroupStat groupStat = stats.get(3);
            Assert.assertEquals(gid2.getGroupId().toString(), groupStat.getGroup().getGroupId());
            Assert.assertEquals(QueryIndicator.TOTAL_SHOWS_COUNT, groupStat.getIndicator().getName());
            List<GroupsStatisticResponse.RangeStat> rangesStat = groupStat.getIndicator().getRanges();
            Assert.assertEquals(6, rangesStat.size());

            checkTotalShowsInRange(rangesStat.get(0), LD_2016_01_12, LD_2016_01_15, LD_2016_01_08, LD_2016_01_11, G2_OFFSET);
            checkTotalShowsInRange(rangesStat.get(1), LD_2016_01_15, LD_2016_01_15, LD_2016_01_14, LD_2016_01_14, G2_OFFSET);
            checkTotalShowsInRange(rangesStat.get(2), LD_2016_01_14, LD_2016_01_14, LD_2016_01_13, LD_2016_01_13, G2_OFFSET);
            checkTotalShowsInRange(rangesStat.get(3), LD_2016_01_13, LD_2016_01_13, LD_2016_01_12, LD_2016_01_12, G2_OFFSET);
            checkTotalShowsInRange(rangesStat.get(4), LD_2016_01_12, LD_2016_01_12, LD_2016_01_11, LD_2016_01_11, G2_OFFSET);
            checkTotalShowsInRange(rangesStat.get(5), LD_2016_01_11, LD_2016_01_11, LD_2016_01_10, LD_2016_01_10, G2_OFFSET);
        }
        {
            GroupsStatisticResponse.GroupStat groupStat = stats.get(4);
            Assert.assertEquals(gid1.getGroupId().toString(), groupStat.getGroup().getGroupId());
            Assert.assertEquals(QueryIndicator.TOTAL_SHOWS_COUNT, groupStat.getIndicator().getName());
            List<GroupsStatisticResponse.RangeStat> rangesStat = groupStat.getIndicator().getRanges();
            Assert.assertEquals(6, rangesStat.size());

            checkTotalShowsInRange(rangesStat.get(0), LD_2016_01_12, LD_2016_01_15, LD_2016_01_08, LD_2016_01_11, G1_OFFSET);
            checkTotalShowsInRange(rangesStat.get(1), LD_2016_01_15, LD_2016_01_15, LD_2016_01_14, LD_2016_01_14, G1_OFFSET);
            checkTotalShowsInRange(rangesStat.get(2), LD_2016_01_14, LD_2016_01_14, LD_2016_01_13, LD_2016_01_13, G1_OFFSET);
            checkTotalShowsInRange(rangesStat.get(3), LD_2016_01_13, LD_2016_01_13, LD_2016_01_12, LD_2016_01_12, G1_OFFSET);
            checkTotalShowsInRange(rangesStat.get(4), LD_2016_01_12, LD_2016_01_12, LD_2016_01_11, LD_2016_01_11, G1_OFFSET);
            checkTotalShowsInRange(rangesStat.get(5), LD_2016_01_11, LD_2016_01_11, LD_2016_01_10, LD_2016_01_10, G1_OFFSET);
        }
    }

    private void checkTotalShowsInRange(GroupsStatisticResponse.RangeStat rangeStat, LocalDate dateFrom,
                                        LocalDate dateTo, LocalDate previousDateFrom,
                                        LocalDate previousDateTo, int offset) {
        double previous;
        double current;
        Assert.assertEquals(dateFrom, rangeStat.getDateFrom());
        Assert.assertEquals(dateTo, rangeStat.getDateTo());
        previous = getExpectedValue(previousDateFrom, previousDateTo, QueryIndicator.TOTAL_SHOWS_COUNT, offset);
        current = getExpectedValue(dateFrom, dateTo, QueryIndicator.TOTAL_SHOWS_COUNT, offset);
        Assert.assertEquals(current, rangeStat.getValue(), 0.01);
        Assert.assertEquals(current - previous, rangeStat.getDifference(), 0.01);
    }

    private void checkTotalCtrInRange(GroupsStatisticResponse.RangeStat rangeStat, LocalDate dateFrom, LocalDate dateTo) {
        Assert.assertEquals(dateFrom, rangeStat.getDateFrom());
        Assert.assertEquals(dateTo, rangeStat.getDateTo());
        Assert.assertEquals(0.5, rangeStat.getValue(), 0.01);
        Assert.assertEquals(0, rangeStat.getDifference(), 0.01);
    }

    private void checkAvgShowPosInRange(GroupsStatisticResponse.RangeStat rangeStat, LocalDate dateFrom, LocalDate dateTo) {
        Assert.assertEquals(dateFrom, rangeStat.getDateFrom());
        Assert.assertEquals(dateTo, rangeStat.getDateTo());
        Assert.assertEquals(1, rangeStat.getValue(), 0.01);
        Assert.assertEquals(0, rangeStat.getDifference(), 0.01);
    }

    @Test
    public void testOneGroup() throws Exception {
        GroupStatisticsService2 groupStatisticsService2 = EasyMock.createMock(GroupStatisticsService2.class);
        QueryGroupService queryGroupService = EasyMock.createMock(QueryGroupService.class);
        GetGroupStatisticsAction action = new GetGroupStatisticsAction(queryGroupService,groupStatisticsService2);
        action.setMode(GetGroupStatisticsAction.Mode.SELECTED_GROUP__ALL_INDICATORS__TIME_RANGES);
        QueryGroupId gid1 = new QueryGroupId(hostId, UUIDs.startOf(1001));

        EasyMock.expect(queryGroupService.getGroup(
                EasyMock.eq(gid1)
        )).andReturn(
                new QueryGroup(gid1, "1", false, null, Instant.now(), Instant.now())
        );

        Map<QueryGroupId, List<GroupStat>> groupStatsMap = new HashMap<>();
        groupStatsMap.put(gid1, fillGroupStat(gid1, LD_2016_01_08, LD_2016_01_15, G1_OFFSET));

        EasyMock.expect(groupStatisticsService2.getStatistics(
                EasyMock.eq(hostId),
                EasyMock.eq(Pair.of(LD_2016_01_08, LD_2016_01_15)),
                EasyMock.anyObject(),
                EasyMock.anyObject(),
                EasyMock.eq(Sets.newHashSet(gid1)),
                EasyMock.anyObject(),
                EasyMock.eq(DeviceType.ALL_DEVICES)
        )).andReturn(groupStatsMap);

        GetGroupStatisticsRequest request = new GetGroupStatisticsRequest();
        request.setUserId(100L);
        request.setDateFrom(LD_2016_01_12.toDateTimeAtStartOfDay());
        request.setDateTo(LD_2016_01_15.toDateTimeAtStartOfDay());
        request.setPeriod(AggregatePeriod.DAY);
        request.setGroupId(gid1.getGroupId().toString());
        request.setConvertedHostId(hostId);
        request.setHostId(hostId.toStringId());

        EasyMock.replay(groupStatisticsService2, queryGroupService);
        GroupsStatisticResponse response = action.process(request);
        EasyMock.verify(groupStatisticsService2, queryGroupService);

        Assert.assertTrue(response instanceof GroupsStatisticResponse.NormalResponse);

        GroupsStatisticResponse.NormalResponse normalResponse = (GroupsStatisticResponse.NormalResponse) response;
        List<GroupsStatisticResponse.DateRange> ranges = normalResponse.getRanges();
        Assert.assertEquals(6, ranges.size());
        Assert.assertEquals(LD_2016_01_12, ranges.get(0).getDateFrom());
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

        List<GroupsStatisticResponse.GroupStat> stats = normalResponse.getStatistics();
        Assert.assertEquals(GetGroupStatisticsAction.DEFAULT_INDICATORS.size(), stats.size());
        for (int i = 0; i < stats.size(); i++) {
            GroupsStatisticResponse.GroupStat groupStat = stats.get(i);
            Assert.assertEquals(gid1.getGroupId().toString(), groupStat.getGroup().getGroupId());
            if (groupStat.getIndicator().getName() == QueryIndicator.TOTAL_SHOWS_COUNT) {
                List<GroupsStatisticResponse.RangeStat> rangesStat = groupStat.getIndicator().getRanges();
                Assert.assertEquals(6, rangesStat.size());

                checkTotalShowsInRange(rangesStat.get(0), LD_2016_01_12, LD_2016_01_15, LD_2016_01_08, LD_2016_01_11, G1_OFFSET);
                checkTotalShowsInRange(rangesStat.get(1), LD_2016_01_15, LD_2016_01_15, LD_2016_01_14, LD_2016_01_14, G1_OFFSET);
                checkTotalShowsInRange(rangesStat.get(2), LD_2016_01_14, LD_2016_01_14, LD_2016_01_13, LD_2016_01_13, G1_OFFSET);
                checkTotalShowsInRange(rangesStat.get(3), LD_2016_01_13, LD_2016_01_13, LD_2016_01_12, LD_2016_01_12, G1_OFFSET);
                checkTotalShowsInRange(rangesStat.get(4), LD_2016_01_12, LD_2016_01_12, LD_2016_01_11, LD_2016_01_11, G1_OFFSET);
                checkTotalShowsInRange(rangesStat.get(5), LD_2016_01_11, LD_2016_01_11, LD_2016_01_10, LD_2016_01_10, G1_OFFSET);
            } else if (groupStat.getIndicator().getName() == QueryIndicator.TOTAL_CTR) {
                List<GroupsStatisticResponse.RangeStat> rangesStat = groupStat.getIndicator().getRanges();
                Assert.assertEquals(6, rangesStat.size());

                checkTotalCtrInRange(rangesStat.get(0), LD_2016_01_12, LD_2016_01_15);
                checkTotalCtrInRange(rangesStat.get(1), LD_2016_01_15, LD_2016_01_15);
                checkTotalCtrInRange(rangesStat.get(2), LD_2016_01_14, LD_2016_01_14);
                checkTotalCtrInRange(rangesStat.get(3), LD_2016_01_13, LD_2016_01_13);
                checkTotalCtrInRange(rangesStat.get(4), LD_2016_01_12, LD_2016_01_12);
                checkTotalCtrInRange(rangesStat.get(5), LD_2016_01_11, LD_2016_01_11);
            } else if (groupStat.getIndicator().getName() == QueryIndicator.AVERAGE_SHOW_POSITION) {
                List<GroupsStatisticResponse.RangeStat> rangesStat = groupStat.getIndicator().getRanges();
                Assert.assertEquals(6, rangesStat.size());

                checkAvgShowPosInRange(rangesStat.get(0), LD_2016_01_12, LD_2016_01_15);
                checkAvgShowPosInRange(rangesStat.get(1), LD_2016_01_15, LD_2016_01_15);
                checkAvgShowPosInRange(rangesStat.get(2), LD_2016_01_14, LD_2016_01_14);
                checkAvgShowPosInRange(rangesStat.get(3), LD_2016_01_13, LD_2016_01_13);
                checkAvgShowPosInRange(rangesStat.get(4), LD_2016_01_12, LD_2016_01_12);
                checkAvgShowPosInRange(rangesStat.get(5), LD_2016_01_11, LD_2016_01_11);
            }
        }
    }
}
