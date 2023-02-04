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
public class RegexSelectorTest {

    @Test
    public void matchesOneOf() {
        Selector selector = Selector.regex("host", "solomon-pre-(man|sas)-.*");
        assertTrue(selector.match("solomon-pre-man-01"));
        assertTrue(selector.match("solomon-pre-sas-01"));
        assertTrue(selector.match("solomon-pre-man-"));
        assertFalse(selector.match("solomon-pre-myt-01"));
    }

    @Test
    public void matchesAll() {
        Selector selector = Selector.regex("host", ".*");
        assertTrue(selector.match("cluster"));
        assertTrue(selector.match("Myt"));
    }

    @Test
    public void forEachMatchedKey() {
        Selector selector = Selector.regex("host", "solomon-fetcher-(man|sas)-.*");

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
        Selector actual = Selector.regex("name", "value").withKey("sensor");
        Selector expected = Selector.regex("sensor", "value");
        assertEquals(expected, actual);
    }

    @Test
    public void namedCaptureGroup() {
        Selector selector = Selector.regex("host", "solomon-pre-(?P<DC>(man|sas))-.*");
        assertTrue(selector.match("solomon-pre-man-01"));
        assertTrue(selector.match("solomon-pre-sas-01"));
        assertTrue(selector.match("solomon-pre-man-"));
        assertFalse(selector.match("solomon-pre-myt-01"));
    }
}
