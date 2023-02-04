package ru.yandex.webmaster3.storage.iks;

import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author akhazhoyan 01/2019
 */
public class IksHistoryServiceTest {
    @Test
    public void testGenerateHistoryForEachDay() {
        List<Pair<LocalDate, Long>> iks = new ArrayList<>();
        iks.add(Pair.of(new LocalDate("2018-04-10"), 23L));
        iks.add(Pair.of(new LocalDate("2018-04-08"), 22L));
        iks.add(Pair.of(new LocalDate("2018-04-11"), 24L));
        iks.add(Pair.of(new LocalDate("2018-04-13"), 25L));
        LocalDate dateFrom = new LocalDate("2018-04-06");
        LocalDate dateTo = new LocalDate("2018-04-17");

        Map<LocalDate, Long> expected = new HashMap<>();
        expected.put(new LocalDate("2018-04-08"), 22L);
        expected.put(new LocalDate("2018-04-09"), 22L);
        expected.put(new LocalDate("2018-04-10"), 23L);
        expected.put(new LocalDate("2018-04-11"), 24L);
        expected.put(new LocalDate("2018-04-12"), 24L);
        expected.put(new LocalDate("2018-04-13"), 25L);
        expected.put(new LocalDate("2018-04-14"), 25L);
        expected.put(new LocalDate("2018-04-15"), 25L);
        expected.put(new LocalDate("2018-04-16"), 25L);
        expected.put(new LocalDate("2018-04-17"), 25L);

        List<Pair<LocalDate, Long>> answer = IksHistoryService.generateHistoryForEachDay(
                iks, dateFrom, dateTo, Collections.emptyList()
        );
        Map<LocalDate, Long> actual = answer.stream()
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGenerateHistoryForEachDayWithZeroes() {
        List<Pair<LocalDate, Long>> iks = new ArrayList<>();
        iks.add(Pair.of(new LocalDate("2018-04-08"), 22L));
        iks.add(Pair.of(new LocalDate("2018-04-10"), 23L));
        iks.add(Pair.of(new LocalDate("2018-04-11"), 24L));
        iks.add(Pair.of(new LocalDate("2018-04-13"), 25L));
        LocalDate dateFrom = new LocalDate("2018-04-06");
        LocalDate dateTo = new LocalDate("2018-04-17");

        Map<LocalDate, Long> expected = new HashMap<>();
        expected.put(new LocalDate("2018-04-07"), 0L);
        expected.put(new LocalDate("2018-04-08"), 22L);
        expected.put(new LocalDate("2018-04-09"), 0L);
        expected.put(new LocalDate("2018-04-10"), 23L);
        expected.put(new LocalDate("2018-04-11"), 24L);
        expected.put(new LocalDate("2018-04-12"), 24L);
        expected.put(new LocalDate("2018-04-13"), 25L);
        expected.put(new LocalDate("2018-04-14"), 25L);
        expected.put(new LocalDate("2018-04-15"), 25L);
        expected.put(new LocalDate("2018-04-16"), 0L);
        expected.put(new LocalDate("2018-04-17"), 0L);

        List<LocalDate> iksUpdateTimes = Arrays.asList(
                LocalDate.parse("2018-04-07"), LocalDate.parse("2018-04-09"), LocalDate.parse("2018-04-16")
        );
        List<Pair<LocalDate, Long>> answer = IksHistoryService.generateHistoryForEachDay(
                iks, dateFrom, dateTo, iksUpdateTimes
        );
        Map<LocalDate, Long> actual = answer.stream()
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

        Assert.assertEquals(expected, actual);
    }
}