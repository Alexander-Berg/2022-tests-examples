package ru.yandex.solomon.labels.query;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Oleg Baryshnikov
 */
public class NotExactSelectorTest {

    @Test
    public void notMatches() {
        Selector selector = Selector.notExact("host", "cluster");
        assertFalse(selector.match("cluster"));
        assertTrue(selector.match("Man"));
    }

    @Test
    public void forEachMatchedKey() {
        Selector selector = Selector.notExact("host", "cluster");

        ImmutableMap<String, Integer> map = ImmutableMap.of(
            "cluster", 1,
            "solomon-fetcher-man-00", 2,
            "solomon-fetcher-sas-00", 3);

        Set<Integer> matched = new HashSet<>();
        selector.forEachMatchedKey(map, matched::add);
        assertEquals(ImmutableSet.of(2, 3), matched);
    }

    @Test
    public void withKey() {
        Selector actual = Selector.notExact("name", "value").withKey("sensor");
        Selector expected = Selector.notExact("sensor", "value");
        assertEquals(expected, actual);
    }
}
