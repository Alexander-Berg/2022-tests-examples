package ru.yandex.webmaster3.viewer.http.searchquery.statistic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.datastax.driver.core.utils.UUIDs;
import org.apache.commons.lang3.tuple.Pair;
import org.easymock.EasyMock;
import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.webmaster3.core.searchquery.OrderDirection;
import ru.yandex.webmaster3.core.searchquery.QueryGroup;
import ru.yandex.webmaster3.core.searchquery.QueryGroupId;
import ru.yandex.webmaster3.core.searchquery.QueryIndicator;
import ru.yandex.webmaster3.core.searchquery.SpecialGroup;
import ru.yandex.webmaster3.storage.searchquery.DeviceType;
import ru.yandex.webmaster3.storage.searchquery.GroupStat;
import ru.yandex.webmaster3.storage.searchquery.GroupStatisticsService2;
import ru.yandex.webmaster3.storage.searchquery.QueryGroupService;

/**
 * @author aherman
 */
public class GetGroupListStatisticsActionTest implements TestData {
    private static final int G_START = 4;
    private static final int ALL_START = 5;
    private static final int SELECTED_START = 2;
    private static final int G1_START = 1;

    @SuppressWarnings("Duplicates")
    @Test
    public void testProcess() throws Exception {
        QueryGroupService queryGroupService = EasyMock.createMock(QueryGroupService.class);
        GroupStatisticsService2 groupStatisticsService2 = EasyMock.createMock(GroupStatisticsService2.class);
        GetGroupListStatisticsAction action = new GetGroupListStatisticsAction(queryGroupService,groupStatisticsService2);

        QueryGroupId gId = new QueryGroupId(hostId, UUIDs.startOf(1000));
        QueryGroupId gId1 = new QueryGroupId(hostId, UUIDs.startOf(1001));
        QueryGroupId allGId = new QueryGroupId(hostId, SpecialGroup.ALL_QUERIES);
        QueryGroupId selectedGId = new QueryGroupId(hostId, SpecialGroup.SELECTED_QUERIES);

        List<QueryGroup> groups = new ArrayList<>();
        groups.add(new QueryGroup(gId, "Test", false, null, Instant.now(), Instant.now()));
        groups.add(new QueryGroup(gId1, "Test1", false, null, Instant.now(), Instant.now()));
        EasyMock.expect(queryGroupService.listGroups(EasyMock.eq(hostId))).andReturn(groups);

        Set<QueryGroupId> groupIds = new HashSet<>();
        groupIds.add(allGId);
        groupIds.add(selectedGId);
        groupIds.add(gId);
        groupIds.add(gId1);

        Map<QueryGroupId, List<GroupStat>> stats = new HashMap<>();
        stats.put(gId, fillGroupStat(gId, LD_2015_12_17, LD_2016_01_15, G_START));
        stats.put(allGId, fillGroupStat(allGId, LD_2015_12_17, LD_2016_01_15, ALL_START));
        stats.put(selectedGId, fillGroupStat(selectedGId, LD_2015_12_17, LD_2016_01_15, SELECTED_START));
        stats.put(gId1, fillGroupStat(gId1, LD_2015_12_17, LD_2016_01_15, G1_START));

        EasyMock.expect(groupStatisticsService2.getStatistics(
                EasyMock.eq(hostId),
                EasyMock.eq(Pair.of(LD_2015_12_17, LD_2016_01_15)),
                EasyMock.anyObject(),
                EasyMock.anyObject(),
                EasyMock.eq(groupIds),
                EasyMock.anyObject(),
                EasyMock.eq(DeviceType.ALL_DEVICES))
        ).andReturn(stats);

        GetGroupListStatisticsRequest request = new GetGroupListStatisticsRequest();
        request.setUserId(100L);
        request.setDateFrom(LD_2016_01_01.toDateTimeAtStartOfDay());
        request.setDateTo(LD_2016_01_15.toDateTimeAtStartOfDay());
        request.setIndicator(new QueryIndicator[]{
                QueryIndicator.TOTAL_SHOWS_COUNT,
                QueryIndicator.TOTAL_CTR,
                QueryIndicator.AVERAGE_CLICK_POSITION
        });
        request.setOrderBy(QueryIndicator.TOTAL_SHOWS_COUNT);
        request.setOrderDirection(OrderDirection.DESC);
        request.setHostId(hostId.toStringId());
        request.setConvertedHostId(hostId);

        EasyMock.replay(queryGroupService, groupStatisticsService2);
        GroupsStatisticResponse response = action.process(request);
        EasyMock.verify(queryGroupService, groupStatisticsService2);

        Assert.assertTrue(response instanceof GroupsStatisticResponse.NormalResponse);
        GroupsStatisticResponse.NormalResponse normalResponse =
                (GroupsStatisticResponse.NormalResponse) response;

        List<GroupsStatisticResponse.GroupStat> statistics = normalResponse.getStatistics();
        Assert.assertEquals(4, statistics.size());
        {
            GroupsStatisticResponse.GroupStat groupStat = statistics.get(0);
            Assert.assertEquals(allGId.getSpecialGroup().name(), groupStat.getGroup().getGroupId());
            List<GroupsStatisticResponse.IndicatorStats> indicators = groupStat.getIndicators();
            Assert.assertEquals(3, indicators.size());
            {
                GroupsStatisticResponse.IndicatorStats ind = indicators.get(0);
                Assert.assertEquals(QueryIndicator.TOTAL_SHOWS_COUNT, ind.getName());
                List<GroupsStatisticResponse.RangeStat> ranges = ind.getRanges();
                Assert.assertEquals(1, ranges.size());
                GroupsStatisticResponse.RangeStat rangeStat = ranges.get(0);
                Assert.assertEquals(LD_2016_01_01, rangeStat.getDateFrom());
                Assert.assertEquals(LD_2016_01_15, rangeStat.getDateTo());
                int expectedUserRangeValue = getExpectedValue(LD_2016_01_01, LD_2016_01_15, QueryIndicator.TOTAL_SHOWS_COUNT, ALL_START);
                int expectedPreviousRangeValue = getExpectedValue(LD_2015_12_17, LD_2015_12_31, QueryIndicator.TOTAL_SHOWS_COUNT, ALL_START);
                Assert.assertEquals(expectedUserRangeValue, rangeStat.getValue(), 0.001);
                Assert.assertEquals(expectedUserRangeValue - expectedPreviousRangeValue, rangeStat.getDifference(), 0.001);
            }
            {
                GroupsStatisticResponse.IndicatorStats ind = indicators.get(1);
                Assert.assertEquals(QueryIndicator.TOTAL_CTR, ind.getName());
                List<GroupsStatisticResponse.RangeStat> ranges = ind.getRanges();
                Assert.assertEquals(1, ranges.size());
                GroupsStatisticResponse.RangeStat rangeStat = ranges.get(0);
                Assert.assertEquals(LD_2016_01_01, rangeStat.getDateFrom());
                Assert.assertEquals(LD_2016_01_15, rangeStat.getDateTo());
                Assert.assertEquals(0.5, rangeStat.getValue(), 0.001);
                Assert.assertEquals(0.0, rangeStat.getDifference(), 0.001);
            }
            {
                GroupsStatisticResponse.IndicatorStats ind = indicators.get(2);
                Assert.assertEquals(QueryIndicator.AVERAGE_CLICK_POSITION, ind.getName());
                List<GroupsStatisticResponse.RangeStat> ranges = ind.getRanges();
                Assert.assertEquals(1, ranges.size());
                GroupsStatisticResponse.RangeStat rangeStat = ranges.get(0);
                Assert.assertEquals(LD_2016_01_01, rangeStat.getDateFrom());
                Assert.assertEquals(LD_2016_01_15, rangeStat.getDateTo());
                Assert.assertEquals(1, rangeStat.getValue(), 0.001);
                Assert.assertEquals(0.0, rangeStat.getDifference(), 0.001);
            }
        }
        {
            GroupsStatisticResponse.GroupStat groupStat = statistics.get(1);
            Assert.assertEquals(selectedGId.getSpecialGroup().name(), groupStat.getGroup().getGroupId());
            List<GroupsStatisticResponse.IndicatorStats> indicators = groupStat.getIndicators();
            Assert.assertEquals(3, indicators.size());
            {
                GroupsStatisticResponse.IndicatorStats ind = indicators.get(0);
                Assert.assertEquals(QueryIndicator.TOTAL_SHOWS_COUNT, ind.getName());
                List<GroupsStatisticResponse.RangeStat> ranges = ind.getRanges();
                Assert.assertEquals(1, ranges.size());
                GroupsStatisticResponse.RangeStat rangeStat = ranges.get(0);
                Assert.assertEquals(LD_2016_01_01, rangeStat.getDateFrom());
                Assert.assertEquals(LD_2016_01_15, rangeStat.getDateTo());
                int expectedUserRangeValue = getExpectedValue(LD_2016_01_01, LD_2016_01_15, QueryIndicator.TOTAL_SHOWS_COUNT, SELECTED_START);
                int expectedPreviousRangeValue = getExpectedValue(LD_2015_12_17, LD_2015_12_31, QueryIndicator.TOTAL_SHOWS_COUNT, SELECTED_START);
                Assert.assertEquals(expectedUserRangeValue, rangeStat.getValue(), 0.001);
                Assert.assertEquals(expectedUserRangeValue - expectedPreviousRangeValue, rangeStat.getDifference(), 0.001);
            }
            {
                GroupsStatisticResponse.IndicatorStats ind = indicators.get(1);
                Assert.assertEquals(QueryIndicator.TOTAL_CTR, ind.getName());
                List<GroupsStatisticResponse.RangeStat> ranges = ind.getRanges();
                Assert.assertEquals(1, ranges.size());
                GroupsStatisticResponse.RangeStat rangeStat = ranges.get(0);
                Assert.assertEquals(LD_2016_01_01, rangeStat.getDateFrom());
                Assert.assertEquals(LD_2016_01_15, rangeStat.getDateTo());
                Assert.assertEquals(0.5, rangeStat.getValue(), 0.001);
                Assert.assertEquals(0.0, rangeStat.getDifference(), 0.001);
            }
            {
                GroupsStatisticResponse.IndicatorStats ind = indicators.get(2);
                Assert.assertEquals(QueryIndicator.AVERAGE_CLICK_POSITION, ind.getName());
                List<GroupsStatisticResponse.RangeStat> ranges = ind.getRanges();
                Assert.assertEquals(1, ranges.size());
                GroupsStatisticResponse.RangeStat rangeStat = ranges.get(0);
                Assert.assertEquals(LD_2016_01_01, rangeStat.getDateFrom());
                Assert.assertEquals(LD_2016_01_15, rangeStat.getDateTo());
                Assert.assertEquals(1, rangeStat.getValue(), 0.001);
                Assert.assertEquals(0.0, rangeStat.getDifference(), 0.001);
            }
        }
        {
            GroupsStatisticResponse.GroupStat groupStat = statistics.get(2);
            Assert.assertEquals(gId.getGroupId().toString(), groupStat.getGroup().getGroupId());
            List<GroupsStatisticResponse.IndicatorStats> indicators = groupStat.getIndicators();
            Assert.assertEquals(3, indicators.size());
            {
                GroupsStatisticResponse.IndicatorStats ind = indicators.get(0);
                Assert.assertEquals(QueryIndicator.TOTAL_SHOWS_COUNT, ind.getName());
                List<GroupsStatisticResponse.RangeStat> ranges = ind.getRanges();
                Assert.assertEquals(1, ranges.size());
                GroupsStatisticResponse.RangeStat rangeStat = ranges.get(0);
                Assert.assertEquals(LD_2016_01_01, rangeStat.getDateFrom());
                Assert.assertEquals(LD_2016_01_15, rangeStat.getDateTo());
                int expectedUserRangeValue = getExpectedValue(LD_2016_01_01, LD_2016_01_15, QueryIndicator.TOTAL_SHOWS_COUNT, G_START);
                int expectedPreviousRangeValue = getExpectedValue(LD_2015_12_17, LD_2015_12_31, QueryIndicator.TOTAL_SHOWS_COUNT, G_START);
                Assert.assertEquals(expectedUserRangeValue, rangeStat.getValue(), 0.001);
                Assert.assertEquals(expectedUserRangeValue - expectedPreviousRangeValue, rangeStat.getDifference(), 0.001);
            }
            {
                GroupsStatisticResponse.IndicatorStats ind = indicators.get(1);
                Assert.assertEquals(QueryIndicator.TOTAL_CTR, ind.getName());
                List<GroupsStatisticResponse.RangeStat> ranges = ind.getRanges();
                Assert.assertEquals(1, ranges.size());
                GroupsStatisticResponse.RangeStat rangeStat = ranges.get(0);
                Assert.assertEquals(LD_2016_01_01, rangeStat.getDateFrom());
                Assert.assertEquals(LD_2016_01_15, rangeStat.getDateTo());
                Assert.assertEquals(0.5, rangeStat.getValue(), 0.001);
                Assert.assertEquals(0.0, rangeStat.getDifference(), 0.001);
            }
            {
                GroupsStatisticResponse.IndicatorStats ind = indicators.get(2);
                Assert.assertEquals(QueryIndicator.AVERAGE_CLICK_POSITION, ind.getName());
                List<GroupsStatisticResponse.RangeStat> ranges = ind.getRanges();
                Assert.assertEquals(1, ranges.size());
                GroupsStatisticResponse.RangeStat rangeStat = ranges.get(0);
                Assert.assertEquals(LD_2016_01_01, rangeStat.getDateFrom());
                Assert.assertEquals(LD_2016_01_15, rangeStat.getDateTo());
                Assert.assertEquals(1, rangeStat.getValue(), 0.001);
                Assert.assertEquals(0.0, rangeStat.getDifference(), 0.001);
            }
        }
        {
            GroupsStatisticResponse.GroupStat groupStat = statistics.get(3);
            Assert.assertEquals(gId1.getGroupId().toString(), groupStat.getGroup().getGroupId());
            List<GroupsStatisticResponse.IndicatorStats> indicators = groupStat.getIndicators();
            Assert.assertEquals(3, indicators.size());
            {
                GroupsStatisticResponse.IndicatorStats ind = indicators.get(0);
                Assert.assertEquals(QueryIndicator.TOTAL_SHOWS_COUNT, ind.getName());
                List<GroupsStatisticResponse.RangeStat> ranges = ind.getRanges();
                Assert.assertEquals(1, ranges.size());
                GroupsStatisticResponse.RangeStat rangeStat = ranges.get(0);
                Assert.assertEquals(LD_2016_01_01, rangeStat.getDateFrom());
                Assert.assertEquals(LD_2016_01_15, rangeStat.getDateTo());
                int expectedUserRangeValue = getExpectedValue(LD_2016_01_01, LD_2016_01_15, QueryIndicator.TOTAL_SHOWS_COUNT, G1_START);
                int expectedPreviousRangeValue = getExpectedValue(LD_2015_12_17, LD_2015_12_31, QueryIndicator.TOTAL_SHOWS_COUNT, G1_START);
                Assert.assertEquals(expectedUserRangeValue, rangeStat.getValue(), 0.001);
                Assert.assertEquals(expectedUserRangeValue - expectedPreviousRangeValue, rangeStat.getDifference(), 0.001);
            }
            {
                GroupsStatisticResponse.IndicatorStats ind = indicators.get(1);
                Assert.assertEquals(QueryIndicator.TOTAL_CTR, ind.getName());
                List<GroupsStatisticResponse.RangeStat> ranges = ind.getRanges();
                Assert.assertEquals(1, ranges.size());
                GroupsStatisticResponse.RangeStat rangeStat = ranges.get(0);
                Assert.assertEquals(LD_2016_01_01, rangeStat.getDateFrom());
                Assert.assertEquals(LD_2016_01_15, rangeStat.getDateTo());
                Assert.assertEquals(0.5, rangeStat.getValue(), 0.001);
                Assert.assertEquals(0.0, rangeStat.getDifference(), 0.001);
            }
            {
                GroupsStatisticResponse.IndicatorStats ind = indicators.get(2);
                Assert.assertEquals(QueryIndicator.AVERAGE_CLICK_POSITION, ind.getName());
                List<GroupsStatisticResponse.RangeStat> ranges = ind.getRanges();
                Assert.assertEquals(1, ranges.size());
                GroupsStatisticResponse.RangeStat rangeStat = ranges.get(0);
                Assert.assertEquals(LD_2016_01_01, rangeStat.getDateFrom());
                Assert.assertEquals(LD_2016_01_15, rangeStat.getDateTo());
                Assert.assertEquals(1, rangeStat.getValue(), 0.001);
                Assert.assertEquals(0.0, rangeStat.getDifference(), 0.001);
            }
        }
    }
}
