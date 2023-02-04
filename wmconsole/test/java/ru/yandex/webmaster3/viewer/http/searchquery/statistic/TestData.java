package ru.yandex.webmaster3.viewer.http.searchquery.statistic;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;

import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.searchquery.QueryGroupId;
import ru.yandex.webmaster3.core.searchquery.QueryId;
import ru.yandex.webmaster3.core.searchquery.QueryIndicator;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.storage.searchquery.GroupStat;
import ru.yandex.webmaster3.storage.searchquery.QueryStat;

/**
 * @author aherman
 */
interface TestData {
    WebmasterHostId hostId = IdUtils.urlToHostId("http://lenta.ru");

    LocalDate LD_2015_12_07 = new LocalDate("2015-12-07");
    LocalDate LD_2015_12_14 = new LocalDate("2015-12-14");
    LocalDate LD_2015_12_17 = new LocalDate("2015-12-17");
    LocalDate LD_2015_12_20 = new LocalDate("2015-12-20");
    LocalDate LD_2015_12_21 = new LocalDate("2015-12-21");
    LocalDate LD_2015_12_27 = new LocalDate("2015-12-27");
    LocalDate LD_2015_12_28 = new LocalDate("2015-12-28");
    LocalDate LD_2015_12_31 = new LocalDate("2015-12-31");
    LocalDate LD_2016_01_01 = new LocalDate("2016-01-01");
    LocalDate LD_2016_01_03 = new LocalDate("2016-01-03");
    LocalDate LD_2016_01_04 = new LocalDate("2016-01-04");
    LocalDate LD_2016_01_08 = new LocalDate("2016-01-08");
    LocalDate LD_2016_01_09 = new LocalDate("2016-01-09");
    LocalDate LD_2016_01_10 = new LocalDate("2016-01-10");
    LocalDate LD_2016_01_11 = new LocalDate("2016-01-11");
    LocalDate LD_2016_01_12 = new LocalDate("2016-01-12");
    LocalDate LD_2016_01_13 = new LocalDate("2016-01-13");
    LocalDate LD_2016_01_14 = new LocalDate("2016-01-14");
    LocalDate LD_2016_01_15 = new LocalDate("2016-01-15");

    public default int getExpectedValue(LocalDate startDate, LocalDate endDate, QueryIndicator queryIndicator) {
        return getExpectedValue(startDate, endDate, queryIndicator, 0);
    }

    public default int getExpectedValue(LocalDate startDate, LocalDate endDate, QueryIndicator queryIndicator, int offset) {
        int sum = 0;
        LocalDate d = startDate;
        while (d.isBefore(endDate) || d.equals(endDate)) {
            sum += d.getDayOfMonth();
            d = d.plusDays(1);
        }
        switch (queryIndicator) {
            case TOTAL_SHOWS_COUNT: return sum * 10 * (offset + 1);
            case TOTAL_CLICKS_COUNT: return sum * 5 * (offset + 1);
            case SHOWS_COUNT_1: return sum * 10 * (offset + 1);
            case CLICKS_COUNT_1: return sum * 5 * (offset + 1);
            default: return 0;
        }
    }

    public default List<GroupStat> fillGroupStat(QueryGroupId groupId, LocalDate startDate, LocalDate endDate) {
        return fillGroupStat(groupId, startDate, endDate, 0);
    }

    public default List<GroupStat> fillGroupStat(QueryGroupId groupId, LocalDate startDate, LocalDate endDate, int offset) {
        List<GroupStat> result = new ArrayList<>();
        LocalDate d = startDate;
        while (d.isBefore(endDate) || d.equals(endDate)) {
            int i = d.getDayOfMonth();
            int shows = i * 10 * (offset + 1);
            int clicks = i * 5 * (offset + 1);
            result.add(new GroupStat(d, groupId, shows, clicks, shows, clicks, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
            d = d.plusDays(1);
        }

        return result;
    }

    public default List<QueryStat> fillQueryStat(QueryId queryId, LocalDate startDate, LocalDate endDate) {
        return fillQueryStat(queryId, startDate, endDate, 0);
    }

    public default List<QueryStat> fillQueryStat(QueryId queryId, LocalDate startDate, LocalDate endDate, int offset) {
        List<QueryStat> stat = new ArrayList<>();
        LocalDate d = startDate;
        while (d.isBefore(endDate) || d.equals(endDate)) {
            int i = d.getDayOfMonth();
            int shows = i * 10 * (offset + 1);
            int clicks = i * 5 * (offset + 1);
            stat.add(new QueryStat(d, queryId, shows, clicks, shows, clicks, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
            d = d.plusDays(1);
        }
        return stat;
    }
}
