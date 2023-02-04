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
public class NotGlobSelectorTest {

    @Test
    public void notExactGlob() {
        Selector selector = Selector.notGlob("host", "solomon-front-man-04");
        assertTrue(selector.match("solomon-"));
        assertFalse(selector.match("solomon-front-man-04"));
        assertTrue(selector.match("solomon"));
    }

    @Test
    public void notSingleGlob() {
        Selector selector = Selector.notGlob("host", "solomon-*");
        assertFalse(selector.match("solomon-"));
        assertFalse(selector.match("solomon-front-man-01"));
        assertTrue(selector.match("solomon"));
    }

    @Test
    public void notMultiGlob() {
        Selector selector = Selector.notGlob("host", "solomon-pre-*|Man");
        assertFalse(selector.match("solomon-pre-man-08"));
        assertFalse(selector.match("Man"));
        assertTrue(selector.match("cluster"));
    }

    @Test
    public void forEachMatchedKey() {
        ImmutableMap<String, Integer> map = ImmutableMap.of(
            "cluster", 1,
            "solomon-fetcher-man-00", 2,
            "solomon-fetcher-sas-00", 3);

        {
            Selector selector = Selector.notGlob("host", "cluster");
            Set<Integer> matched = new HashSet<>();
            selector.forEachMatchedKey(map, matched::add);
            assertEquals(ImmutableSet.of(2, 3), matched);
        }
        {
            Selector selector = Selector.notGlob("host", "solomon-*");
            Set<Integer> matched = new HashSet<>();
            selector.forEachMatchedKey(map, matched::add);
            assertEquals(ImmutableSet.of(1), matched);
        }
        {
            Selector selector = Selector.notGlob("host", "*-man-*|*-sas-*");
            Set<Integer> matched = new HashSet<>();
            selector.forEachMatchedKey(map, matched::add);
            assertEquals(ImmutableSet.of(1), matched);
        }
    }

    @Test
    public void withKey() {
        Selector actual = Selector.notGlob("name", "value").withKey("sensor");
        Selector expected = Selector.notGlob("sensor", "value");
        assertEquals(expected, actual);
    }
}
