package ru.yandex.wmconsole.service;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Test;

/**
 * User: azakharov
 * Date: 27.08.12
 * Time: 19:09
 */
public class IndexHistoryServiceTest {

    @Test
    public void testCalcTrend() throws ParseException {
        NavigableMap<Date, Long> m = new TreeMap<Date, Long>();
        DateFormat form = new SimpleDateFormat("yyyy-MM-dd");
        m.put(form.parse("2012-08-22"), 99631L);
        m.put(form.parse("2012-08-23"), 99631L);
        m.put(form.parse("2012-08-24"), 97618L);
        m.put(form.parse("2012-08-25"), 99619L);
        m.put(form.parse("2012-08-26"), 100000L);
        m.put(form.parse("2012-08-27"), 114847L);
        Long trend = IndexHistoryService.calculateTrend(m);
        Assert.assertEquals(Long.valueOf(114847L-97618L), trend);
    }
    @Test
    public void testCalcTrendForValueSmallerThanBorderValue1() throws ParseException {
        NavigableMap<Date, Long> m = new TreeMap<Date, Long>();
        DateFormat form = new SimpleDateFormat("yyyy-MM-dd");
        m.put(form.parse("2012-08-24"), 0L);
        m.put(form.parse("2012-08-27"), 99L);
        Long trend = IndexHistoryService.calculateTrend(m);
        Assert.assertEquals(null, trend);
    }

    @Test
    public void testCalcTrendForValueSmallerThanBorderValue2() throws ParseException {
        NavigableMap<Date, Long> m = new TreeMap<Date, Long>();
        DateFormat form = new SimpleDateFormat("yyyy-MM-dd");
        m.put(form.parse("2012-08-24"), 99L);
        m.put(form.parse("2012-08-27"), 0L);
        Long trend = IndexHistoryService.calculateTrend(m);
        Assert.assertEquals(null, trend);
    }

    @Test
    public void testCalcTrendForRelativeDifferenceLessThen15Percents() throws ParseException {
        NavigableMap<Date, Long> m = new TreeMap<Date, Long>();
        DateFormat form = new SimpleDateFormat("yyyy-MM-dd");
        m.put(form.parse("2012-08-24"), 150L);
        m.put(form.parse("2012-08-27"), 128L);
        Long trend = IndexHistoryService.calculateTrend(m);
        Assert.assertEquals(Long.valueOf(0L), trend);
    }
}
