package ru.yandex.auto.core.misc;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * @author Anton Irinev (airinev@yandex-team.ru)
 */
public class TopSorterTest {

    private static final Resource[] RESOURCES = {
            new Resource(1, 0.1),
            new Resource(2, 0.2),
            new Resource(3, 0.3),
            new Resource(4, 0.4)
    };

    @Test
    public void testSimple1() {
        Assert.assertEquals("4 3 2", getSortedResult(RESOURCES, 3, true));
        Assert.assertEquals("4 3 2", getSortedResult(RESOURCES, 3, false));
    }

    @Test
    public void testSimple2() {
        Assert.assertEquals("4 3 2 1", getSortedResult(RESOURCES, 4, true));
        Assert.assertEquals("4 3 2 1", getSortedResult(RESOURCES, 4, false));
    }

    @Test
    public void testSimple3() {
        Assert.assertEquals("4 3 2 1", getSortedResult(RESOURCES, 5, true));
        Assert.assertEquals("4 3 2 1", getSortedResult(RESOURCES, 5, false));
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

    private static String getSortedResult(Resource[] testSet, int topCount, boolean iterateForward) {
        TopSorter<Resource> sorter = new TopSorter<Resource>(topCount);
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
