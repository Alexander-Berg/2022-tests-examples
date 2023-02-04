package ru.yandex.webmaster3.viewer.http.searchquery.statistic;

import java.util.List;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.webmaster3.storage.searchquery.MissingDataPeriod;

/**
 * @author aherman
 */
public class GetMissingPeriodsActionTest {
    private static final MissingDataPeriod P_2016_03_28__2016_03_30 =
            new MissingDataPeriod(new DateTime("2016-03-28"), new DateTime("2016-03-30"), "");
    private static final MissingDataPeriod P_2016_03_31__2016_03_31 =
            new MissingDataPeriod(new DateTime("2016-03-31"), new DateTime("2016-03-31"), "");
    private static final MissingDataPeriod P_2016_04_01__2016_04_07 =
            new MissingDataPeriod(new DateTime("2016-04-01"), new DateTime("2016-04-07"), "");
    private static final MissingDataPeriod P_2016_04_08__2016_04_08 =
            new MissingDataPeriod(new DateTime("2016-04-08"), new DateTime("2016-04-08"), "");
    private static final MissingDataPeriod P_2016_04_09__2016_04_10 =
            new MissingDataPeriod(new DateTime("2016-04-09"), new DateTime("2016-04-10"), "");

    @Test
    public void testNoFilter() throws Exception {
        List<MissingDataPeriod> periods = createPeriods();

        GetMissingPeriodsAction.Request request = new GetMissingPeriodsAction.Request();
        List<MissingDataPeriod> actualPeriods = GetMissingPeriodsAction.filterPeriods(request, periods);

        Assert.assertEquals(periods, actualPeriods);
    }

    @Test
    public void testLeftBorder1() throws Exception {
        List<MissingDataPeriod> expected = Lists.newArrayList(
                P_2016_04_01__2016_04_07,
                P_2016_04_08__2016_04_08,
                P_2016_04_09__2016_04_10
        );
        GetMissingPeriodsAction.Request request = new GetMissingPeriodsAction.Request();
        request.setDateFrom(new DateTime("2016-04-01"));
        List<MissingDataPeriod> actualPeriods = GetMissingPeriodsAction.filterPeriods(request, createPeriods());

        Assert.assertEquals(expected, actualPeriods);
    }

    @Test
    public void testLeftBorder2() throws Exception {
        List<MissingDataPeriod> expected = Lists.newArrayList(
                P_2016_04_01__2016_04_07,
                P_2016_04_08__2016_04_08,
                P_2016_04_09__2016_04_10
        );
        GetMissingPeriodsAction.Request request = new GetMissingPeriodsAction.Request();
        request.setDateFrom(new DateTime("2016-04-02"));
        List<MissingDataPeriod> actualPeriods = GetMissingPeriodsAction.filterPeriods(request, createPeriods());

        Assert.assertEquals(expected, actualPeriods);
    }

    @Test
    public void testLeftBorder3() throws Exception {
        List<MissingDataPeriod> expected = Lists.newArrayList(
                P_2016_03_31__2016_03_31,
                P_2016_04_01__2016_04_07,
                P_2016_04_08__2016_04_08,
                P_2016_04_09__2016_04_10
        );
        GetMissingPeriodsAction.Request request = new GetMissingPeriodsAction.Request();
        request.setDateFrom(new DateTime("2016-03-31"));
        List<MissingDataPeriod> actualPeriods = GetMissingPeriodsAction.filterPeriods(request, createPeriods());

        Assert.assertEquals(expected, actualPeriods);
    }

    @Test
    public void testRightBorder1() throws Exception {
        List<MissingDataPeriod> expected = Lists.newArrayList(
                P_2016_03_28__2016_03_30,
                P_2016_03_31__2016_03_31,
                P_2016_04_01__2016_04_07
        );
        GetMissingPeriodsAction.Request request = new GetMissingPeriodsAction.Request();
        request.setDateTo(new DateTime("2016-04-01"));
        List<MissingDataPeriod> actualPeriods = GetMissingPeriodsAction.filterPeriods(request, createPeriods());

        Assert.assertEquals(expected, actualPeriods);
    }

    @Test
    public void testRightBorder2() throws Exception {
        List<MissingDataPeriod> expected = Lists.newArrayList(
                P_2016_03_28__2016_03_30,
                P_2016_03_31__2016_03_31,
                P_2016_04_01__2016_04_07
        );
        GetMissingPeriodsAction.Request request = new GetMissingPeriodsAction.Request();
        request.setDateTo(new DateTime("2016-04-02"));
        List<MissingDataPeriod> actualPeriods = GetMissingPeriodsAction.filterPeriods(request, createPeriods());

        Assert.assertEquals(expected, actualPeriods);
    }

    @Test
    public void testRightBorder3() throws Exception {
        List<MissingDataPeriod> expected = Lists.newArrayList(
                P_2016_03_28__2016_03_30,
                P_2016_03_31__2016_03_31,
                P_2016_04_01__2016_04_07
        );
        GetMissingPeriodsAction.Request request = new GetMissingPeriodsAction.Request();
        request.setDateTo(new DateTime("2016-04-07"));
        List<MissingDataPeriod> actualPeriods = GetMissingPeriodsAction.filterPeriods(request, createPeriods());

        Assert.assertEquals(expected, actualPeriods);
    }

    @Test
    public void testRightBorder4() throws Exception {
        List<MissingDataPeriod> expected = Lists.newArrayList(
                P_2016_03_28__2016_03_30,
                P_2016_03_31__2016_03_31,
                P_2016_04_01__2016_04_07,
                P_2016_04_08__2016_04_08
        );
        GetMissingPeriodsAction.Request request = new GetMissingPeriodsAction.Request();
        request.setDateTo(new DateTime("2016-04-08"));
        List<MissingDataPeriod> actualPeriods = GetMissingPeriodsAction.filterPeriods(request, createPeriods());

        Assert.assertEquals(expected, actualPeriods);
    }

    @Test
    public void testBothBorder1() throws Exception {
        List<MissingDataPeriod> expected = Lists.newArrayList(
                P_2016_04_01__2016_04_07
        );
        GetMissingPeriodsAction.Request request = new GetMissingPeriodsAction.Request();
        request.setDateFrom(new DateTime("2016-04-02"));
        request.setDateTo(new DateTime("2016-04-04"));
        List<MissingDataPeriod> actualPeriods = GetMissingPeriodsAction.filterPeriods(request, createPeriods());

        Assert.assertEquals(expected, actualPeriods);
    }

    @Test
    public void testBothBorder2() throws Exception {
        List<MissingDataPeriod> expected = Lists.newArrayList(
                P_2016_03_31__2016_03_31,
                P_2016_04_01__2016_04_07,
                P_2016_04_08__2016_04_08
        );
        GetMissingPeriodsAction.Request request = new GetMissingPeriodsAction.Request();
        request.setDateFrom(new DateTime("2016-03-31"));
        request.setDateTo(new DateTime("2016-04-08"));
        List<MissingDataPeriod> actualPeriods = GetMissingPeriodsAction.filterPeriods(request, createPeriods());

        Assert.assertEquals(expected, actualPeriods);
    }

    @NotNull
    private List<MissingDataPeriod> createPeriods() {
        return Lists.newArrayList(
                P_2016_03_28__2016_03_30,
                P_2016_03_31__2016_03_31,
                P_2016_04_01__2016_04_07,
                P_2016_04_08__2016_04_08,
                P_2016_04_09__2016_04_10
            );
    }
}