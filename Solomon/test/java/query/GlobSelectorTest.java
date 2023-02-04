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
public class GlobSelectorTest {

    @Test
    public void exactGlob() {
        Selector selector = Selector.glob("host", "solomon-sas-03*");
        assertFalse(selector.match("solomon-"));
        assertTrue(selector.match("solomon-sas-03"));
        assertFalse(selector.match("solomon"));
    }

    @Test
    public void singleGlob() {
        Selector selector = Selector.glob("host", "solomon-?*");
        assertFalse(selector.match("solomon-"));
        assertTrue(selector.match("solomon-front-sas-01"));
        assertFalse(selector.match("solomon"));
    }

    @Test
    public void multiGlob() {
        Selector selector = Selector.glob("host", "solomon-pre-*|Man");
        assertTrue(selector.match("solomon-pre-man-08"));
        assertTrue(selector.match("Man"));
        assertFalse(selector.match("cluster"));
    }

    @Test
    public void forEachMatchedKey() {
        ImmutableMap<String, Integer> map = ImmutableMap.of(
            "cluster", 1,
            "solomon-fetcher-man-00", 2,
            "solomon-fetcher-sas-00", 3);

        {
            Selector selector = Selector.glob("host", "cluster");
            Set<Integer> matched = new HashSet<>();
            selector.forEachMatchedKey(map, matched::add);
            assertEquals(ImmutableSet.of(1), matched);
        }
        {
            Selector selector = Selector.glob("host", "solomon-*");
            Set<Integer> matched = new HashSet<>();
            selector.forEachMatchedKey(map, matched::add);
            assertEquals(ImmutableSet.of(2, 3), matched);
        }
        {
            Selector selector = Selector.glob("host", "*-man-*|*-sas-*");
            Set<Integer> matched = new HashSet<>();
            selector.forEachMatchedKey(map, matched::add);
            assertEquals(ImmutableSet.of(2, 3), matched);
        }
    }

    @Test
    public void withKey() {
        Selector actual = Selector.glob("name", "value").withKey("sensor");
        Selector expected = Selector.glob("sensor", "value");
        assertEquals(expected, actual);
    }
}
