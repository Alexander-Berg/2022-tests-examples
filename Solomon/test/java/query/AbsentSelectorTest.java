package ru.yandex.solomon.labels.query;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Oleg Baryshnikov
 */
public class AbsentSelectorTest {

    @Test
    public void matches() {
        Selector selector = Selector.absent("host");
        assertFalse(selector.match("cluster"));
    }

    @Test
    public void forEachMatchedKey() {
        Selector selector = Selector.absent("host");

        ImmutableMap<String, Integer> map = ImmutableMap.of(
            "cluster", 1,
            "solomon-fetcher-man-00", 2,
            "solomon-fetcher-sas-00", 3);

        Set<Integer> matched = new HashSet<>();
        selector.forEachMatchedKey(map, matched::add);
        assertEquals(ImmutableSet.of(), matched);
    }

    @Test
    public void withKey() {
        Selector actual = Selector.absent("name").withKey("sensor");
        Selector expected = Selector.absent("sensor");
        assertEquals(expected, actual);
    }
}
