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
public class NotRegexSelectorTest {

    @Test
    public void notMatchesOneOf() {
        Selector selector = Selector.notRegex("host", "solomon-pre-(man|sas)-.*");
        assertFalse(selector.match("solomon-pre-man-01"));
        assertFalse(selector.match("solomon-pre-sas-01"));
        assertFalse(selector.match("solomon-pre-man-"));
        assertTrue(selector.match("solomon-pre-myt-01"));
    }

    @Test
    public void notMatchesAll() {
        Selector selector = Selector.notRegex("host", ".*");
        assertFalse(selector.match("cluster"));
        assertFalse(selector.match("Man"));
    }

    @Test
    public void forEachMatchedKey() {
        Selector selector = Selector.notRegex("host", "solomon-fetcher-(man|sas)-.*");

        ImmutableMap<String, Integer> map = ImmutableMap.of(
            "cluster", 1,
            "solomon-fetcher-man-00", 2,
            "solomon-fetcher-sas-00", 3);

        Set<Integer> matched = new HashSet<>();
        selector.forEachMatchedKey(map, matched::add);
        assertEquals(ImmutableSet.of(1), matched);
    }

    @Test
    public void withKey() {
        Selector actual = Selector.notRegex("name", "value").withKey("sensor");
        Selector expected = Selector.notRegex("sensor", "value");
        assertEquals(expected, actual);
    }
}
