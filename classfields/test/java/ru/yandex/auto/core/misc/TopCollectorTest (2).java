package ru.yandex.auto.core.misc;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static ru.yandex.common.util.collections.CollectionFactory.list;
import static ru.yandex.common.util.collections.CollectionFactory.newHashMap;

/**
 * @author ssimonchik
 */
public class TopCollectorTest {

    private static final List<Integer> INPUTS = list(4, 8, 3, 6, 10, -1, 8);

    private static final Map<Integer, List<Integer>> GREATEST_ANSWERS = newHashMap();
    static {
        GREATEST_ANSWERS.put(0, Collections.<Integer>emptyList());
        GREATEST_ANSWERS.put(1, list(10));
        GREATEST_ANSWERS.put(2, list(10, 8));
        GREATEST_ANSWERS.put(3, list(10, 8, 8));
        GREATEST_ANSWERS.put(4, list(10, 8, 8, 6));
        GREATEST_ANSWERS.put(5, list(10, 8, 8, 6, 4));
        GREATEST_ANSWERS.put(6, list(10, 8, 8, 6, 4, 3));
        GREATEST_ANSWERS.put(7, list(10, 8, 8, 6, 4, 3, -1));
        GREATEST_ANSWERS.put(8, list(10, 8, 8, 6, 4, 3, -1));
    }

    private static final Map<Integer, List<Integer>> LEAST_ANSWERS = newHashMap();
    static {
        LEAST_ANSWERS.put(0, Collections.<Integer>emptyList());
        LEAST_ANSWERS.put(1, list(-1));
        LEAST_ANSWERS.put(2, list(-1, 3));
        LEAST_ANSWERS.put(3, list(-1, 3, 4));
        LEAST_ANSWERS.put(4, list(-1, 3, 4, 6));
        LEAST_ANSWERS.put(5, list(-1, 3, 4, 6, 8));
        LEAST_ANSWERS.put(6, list(-1, 3, 4, 6, 8, 8));
        LEAST_ANSWERS.put(7, list(-1, 3, 4, 6, 8, 8, 10));
        LEAST_ANSWERS.put(8, list(-1, 3, 4, 6, 8, 8, 10));
    }

    @Test
    public void testGreatest0() {
        testGreatest(0);
    }

    @Test
    public void testGreatest1() {
        testGreatest(1);
    }

    @Test
    public void testGreatest2() {
        testGreatest(2);
    }

    @Test
    public void testGreatestAll() {
        for (int topCount : GREATEST_ANSWERS.keySet()) {
            testGreatest(topCount);
        }
    }

    private void testGreatest(int topGreatest) {
        TopCollector<Integer> topCollector = TopCollector.newTopGreatestCollector(topGreatest);
        topCollector.submitAll(INPUTS);
        List<Integer> tops = topCollector.tops();
        Assert.assertEquals(GREATEST_ANSWERS.get(topGreatest), tops);
    }

    @Test
    public void testLeast0() {
        testLeast(0);
    }

    @Test
    public void testLeast1() {
        testLeast(1);
    }

    @Test
    public void testLeast2() {
        testLeast(2);
    }

    @Test
    public void testLeastAll() {
        for (int topCount : GREATEST_ANSWERS.keySet()) {
            testLeast(topCount);
        }
    }

    private void testLeast(int topLeast) {
        TopCollector<Integer> topCollector = TopCollector.newTopLeastCollector(topLeast);
        topCollector.submitAll(INPUTS);
        List<Integer> tops = topCollector.tops();
        Assert.assertEquals(LEAST_ANSWERS.get(topLeast), tops);
    }

    @Test
    public void testUpdateFirst1() {
        TopCollector<Integer> topCollector = TopCollector.newTopGreatestCollector(3);
        topCollector.submit(5);
        topCollector.submit(4);
        topCollector.submit(3);
        topCollector.submit(2);
        topCollector.submit(1);
        topCollector.updateFirst(4, -1);
        topCollector.updateFirst(6, -1);
        Assert.assertEquals(list(5, 3, -1), topCollector.tops());
    }

    @Test
    public void testUpdateFirst2() {
        TopCollector<Integer> topCollector = TopCollector.newTopGreatestCollector(3);
        topCollector.submit(5);
        topCollector.submit(4);
        topCollector.submit(3);
        topCollector.submit(2);
        topCollector.submit(1);
        topCollector.updateFirst(4, 6);
        Assert.assertEquals(list(6, 5, 3), topCollector.tops());
    }
}
