package ru.yandex.webmaster.common.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author aherman
 */
public class TopKCounterTest {

    @Test
    public void testTestCounter() throws Exception {
        TopKCounter<Integer> topK = new TopKCounter<>(4);

        TopKCounter.Measure m = topK.newEntry(0);
        Assert.assertEquals(1, m.getCount());
        Assert.assertEquals(0, m.getError());

        m = topK.newEntry(1);
        Assert.assertEquals(1, m.getCount());
        Assert.assertEquals(0, m.getError());

        m = topK.newEntry(2);
        Assert.assertEquals(1, m.getCount());
        Assert.assertEquals(0, m.getError());

        m = topK.newEntry(10);
        Assert.assertEquals(1, m.getCount());
        Assert.assertEquals(0, m.getError());

        m = topK.newEntry(0);
        Assert.assertEquals(2, m.getCount());
        Assert.assertEquals(0, m.getError());

        m = topK.newEntry(1);
        Assert.assertEquals(2, m.getCount());
        Assert.assertEquals(0, m.getError());

        m = topK.newEntry(10);
        Assert.assertEquals(2, m.getCount());
        Assert.assertEquals(0, m.getError());

        m = topK.newEntry(0);
        Assert.assertEquals(3, m.getCount());
        Assert.assertEquals(0, m.getError());

        m = topK.newEntry(3);
        Assert.assertEquals(2, m.getCount());
        Assert.assertEquals(1, m.getError());

        m = topK.newEntry(3);
        Assert.assertEquals(3, m.getCount());
        Assert.assertEquals(1, m.getError());

        m = topK.newEntry(4);
        Assert.assertEquals(3, m.getCount());
        Assert.assertEquals(2, m.getError());

        m = topK.newEntry(0);
        Assert.assertEquals(4, m.getCount());
        Assert.assertEquals(0, m.getError());
    }
}
