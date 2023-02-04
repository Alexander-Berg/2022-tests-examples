package ru.yandex.realty.util;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Anton Irinev (airinev@yandex-team.ru)
 */
public class TopCollectorTest {

    private static final Resource[] RESOURCES = {
            new Resource(1, 0.4),
            new Resource(2, 0.3),
            new Resource(3, 0.2),
            new Resource(4, 0.1)
    };

    @Test
    public void testNonUnique() throws Exception {
        TopCollector<Integer> top = new TopCollector<>(3);
        top.submit(1);
        top.submit(1);
        top.submit(3);
        top.submit(4);
        top.submit(5);
        top.submit(5);
        assertEquals(Arrays.asList(5, 5, 4), top.getResult());

        top = new TopCollector<>(3, Collections.<Integer>reverseOrder());
        top.submit(1);
        top.submit(1);
        top.submit(3);
        top.submit(4);
        top.submit(5);
        top.submit(5);
        assertEquals(Arrays.asList(1, 1, 3), top.getResult());
    }

    @Test
    public void testSimpleMin1() {
        assertEquals("4 3 2", getSortedResult(RESOURCES, 3, false, true));
        assertEquals("4 3 2", getSortedResult(RESOURCES, 3, false, false));
    }

    @Test
    public void testSimpleMin2() {
        assertEquals("4 3 2 1", getSortedResult(RESOURCES, 4, false, true));
        assertEquals("4 3 2 1", getSortedResult(RESOURCES, 4, false, false));
    }

    @Test
    public void testSimpleMin3() {
        assertEquals("4 3 2 1", getSortedResult(RESOURCES, 5, false, true));
        assertEquals("4 3 2 1", getSortedResult(RESOURCES, 5, false, false));
    }

    @Test
    public void testSimpleMax1() {
        assertEquals("1 2 3", getSortedResult(RESOURCES, 3, true, true));
        assertEquals("1 2 3", getSortedResult(RESOURCES, 3, true, false));
    }

    @Test
    public void testSimpleMax2() {
        assertEquals("1 2 3 4", getSortedResult(RESOURCES, 4, true, true));
        assertEquals("1 2 3 4", getSortedResult(RESOURCES, 4, true, false));
    }

    @Test
    public void testSimpleMax3() {
        assertEquals("1 2 3 4", getSortedResult(RESOURCES, 5, true, true));
        assertEquals("1 2 3 4", getSortedResult(RESOURCES, 5, true, false));
    }

    private static final class Resource implements Comparable<Resource> {
        final int resourceId;
        final double compareValue;

        Resource(int resourceId, double compareValue) {
            this.resourceId = resourceId;
            this.compareValue = compareValue;
        }

        @Override
        public int compareTo(Resource o) {
            return Double.compare(compareValue, o.compareValue);
        }
    }

    // helper methods

    private static String getSortedResult(Resource[] testSet, int topCount, boolean collectMax, boolean iterateForward) {
        TopCollector<Resource> sorter = new TopCollector<Resource>(topCount, collectMax ? null : Collections.<Resource>reverseOrder());
        for (int i = 0; i < testSet.length; i++) {
            sorter.submit(testSet[iterateForward ? i : testSet.length - i - 1]);
        }
        return toString(sorter.getResult());
    }

    private static String toString(List<Resource> pairs) {
        String result = "";
        for (Resource pair : pairs) {
            result += pair.resourceId + " ";
        }
        int endIndex = result.length() > 0 ? result.length() - 1 : 0;
        return result.substring(0, endIndex);
    }
}