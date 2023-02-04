package ru.yandex.solomon.model.point.column;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vladimir Gordiychuk
 */
public class CountColumnTest {

    @Test
    public void mergeCount() {
        assertEquals(0, CountColumn.merge(0, 0));
        assertEquals(4, CountColumn.merge(2, 2));
        assertEquals(Integer.MAX_VALUE + 2L, CountColumn.merge(Integer.MAX_VALUE, 2));
        assertEquals((long) Integer.MAX_VALUE + (long) Integer.MAX_VALUE, CountColumn.merge(Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, CountColumn.merge(Long.MAX_VALUE, 1));
        assertEquals(Long.MAX_VALUE, CountColumn.merge(Long.MAX_VALUE, Long.MAX_VALUE));
    }
}
