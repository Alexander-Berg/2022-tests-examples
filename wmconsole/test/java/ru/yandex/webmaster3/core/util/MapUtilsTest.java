package ru.yandex.webmaster3.core.util;

import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author aherman
 */
public class MapUtilsTest {
    private TreeMap<Integer, Integer> map;

    @Before
    public void setUp() throws Exception {
        map = new TreeMap<>();
        map.put(1, 1);
        map.put(3, 3);
        map.put(5, 5);
    }

    @Test
    public void headMap() throws Exception {
        SortedMap<Integer, Integer> m = MapUtils.tailMap(map,0);
        Assert.assertEquals(0, MapUtils.headMap(m,-1).size());
        Assert.assertEquals(0, MapUtils.headMap(m,0).size());
        Assert.assertEquals(0, MapUtils.headMap(m,1).size());
        Assert.assertEquals(1, MapUtils.headMap(m,2).size());
        Assert.assertEquals(1, MapUtils.headMap(m,3).size());
        Assert.assertEquals(2, MapUtils.headMap(m,4).size());
        Assert.assertEquals(2, MapUtils.headMap(m,5).size());
        Assert.assertEquals(3, MapUtils.headMap(m,6).size());
        Assert.assertEquals(3, MapUtils.headMap(m,7).size());
    }

    @Test
    public void headMapInclusive() throws Exception {
        NavigableMap<Integer, Integer> m = MapUtils.tailMap(map,0, true);
        Assert.assertEquals(0, MapUtils.headMap(m,-1, true).size());
        Assert.assertEquals(0, MapUtils.headMap(m,0, true).size());
        Assert.assertEquals(1, MapUtils.headMap(m,1, true).size());
        Assert.assertEquals(1, MapUtils.headMap(m,2, true).size());
        Assert.assertEquals(2, MapUtils.headMap(m,3, true).size());
        Assert.assertEquals(2, MapUtils.headMap(m,4, true).size());
        Assert.assertEquals(3, MapUtils.headMap(m,5, true).size());
        Assert.assertEquals(3, MapUtils.headMap(m,6, true).size());
        Assert.assertEquals(3, MapUtils.headMap(m,7, true).size());
    }

    @Test
    public void tailMap() throws Exception {
        SortedMap<Integer, Integer> m = MapUtils.headMap(map,6);
        Assert.assertEquals(3, MapUtils.tailMap(m,-1).size());
        Assert.assertEquals(3, MapUtils.tailMap(m,0).size());
        Assert.assertEquals(3, MapUtils.tailMap(m,1).size());
        Assert.assertEquals(2, MapUtils.tailMap(m,2).size());
        Assert.assertEquals(2, MapUtils.tailMap(m,3).size());
        Assert.assertEquals(1, MapUtils.tailMap(m,4).size());
        Assert.assertEquals(1, MapUtils.tailMap(m,5).size());
        Assert.assertEquals(0, MapUtils.tailMap(m,6).size());
        Assert.assertEquals(0, MapUtils.tailMap(m,7).size());
    }

    @Test
    public void tailMapExclusive() throws Exception {
        NavigableMap<Integer, Integer> m = MapUtils.headMap(map,6, false);
        Assert.assertEquals(3, MapUtils.tailMap(m,-1, false).size());
        Assert.assertEquals(3, MapUtils.tailMap(m,0, false).size());
        Assert.assertEquals(2, MapUtils.tailMap(m,1, false).size());
        Assert.assertEquals(2, MapUtils.tailMap(m,2, false).size());
        Assert.assertEquals(1, MapUtils.tailMap(m,3, false).size());
        Assert.assertEquals(1, MapUtils.tailMap(m,4, false).size());
        Assert.assertEquals(0, MapUtils.tailMap(m,5, false).size());
        Assert.assertEquals(0, MapUtils.tailMap(m,6, false).size());
        Assert.assertEquals(0, MapUtils.tailMap(m,7, false).size());

    }
}