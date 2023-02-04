package ru.yandex.webmaster3.storage.util.clickhouse2;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import com.google.common.math.IntMath;
import com.google.common.math.LongMath;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.webmaster3.core.util.IdUtils;

/**
 * @author aherman
 */
public class ByteConvertUtilsTest {
    @Test
    public void testParseUrlAsHostId() throws Exception {
        Assert.assertEquals(
                IdUtils.stringToHostId("http:lenta.ru:80"),
                ByteConvertUtils.parseUrlAsHostId(asStream("http://lenta.ru"))
        );
        Assert.assertEquals(
                IdUtils.stringToHostId("http:lenta.ru:8080"),
                ByteConvertUtils.parseUrlAsHostId(asStream("http://lenta.ru:8080"))
        );
        Assert.assertEquals(
                IdUtils.stringToHostId("https:lenta.ru:443"),
                ByteConvertUtils.parseUrlAsHostId(asStream("https://lenta.ru"))
        );
        Assert.assertEquals(
                IdUtils.stringToHostId("https:lenta.ru:8080"),
                ByteConvertUtils.parseUrlAsHostId(asStream("https://lenta.ru:8080"))
        );
        Assert.assertEquals(
                IdUtils.stringToHostId("http:lenta.ru:80"),
                ByteConvertUtils.parseUrlAsHostId(asStream("http://lenta.ru/"))
        );
        Assert.assertEquals(
                IdUtils.stringToHostId("http:lenta.ru:8080"),
                ByteConvertUtils.parseUrlAsHostId(asStream("http://lenta.ru:8080/"))
        );
        Assert.assertEquals(
                IdUtils.stringToHostId("https:lenta.ru:443"),
                ByteConvertUtils.parseUrlAsHostId(asStream("https://lenta.ru/"))
        );
        Assert.assertEquals(
                IdUtils.stringToHostId("https:lenta.ru:8080"),
                ByteConvertUtils.parseUrlAsHostId(asStream("https://lenta.ru:8080/"))
        );
    }

    private InputStream asStream(String s) {
        return new ByteArrayInputStream(s.getBytes());
    }

    @Test
    public void testWriteLong() throws Exception {
        assertLong(Long.MIN_VALUE);
        assertLong(Long.MIN_VALUE + 1);
        for (int j = 18; j > 0; j--) {
            long v = -1 * LongMath.checkedPow(10, j);
            assertLong(v - 1);
            assertLong(v);
            assertLong(v + 1);
        }
        for (int i = -1001; i < 1002; i++) {
            assertLong(i);
        }
        for (int j = 1; j < 19; j++) {
            long v = LongMath.checkedPow(10, j);
            assertLong(v - 1);
            assertLong(v);
            assertLong(v + 1);
        }
        assertLong(Long.MAX_VALUE - 1);
        assertLong(Long.MAX_VALUE);
    }

    @Test
    public void testWriteLong1() throws Exception {
        assertLong(-7312296559024148321L);
    }

    @Test
    public void testWriteInt() throws Exception {
        assertInt(Integer.MIN_VALUE);
        assertInt(Integer.MIN_VALUE + 1);
        for (int j = 9; j > 0; j--) {
            int v = -1 * IntMath.checkedPow(10, j);
            assertInt(v - 1);
            assertInt(v);
            assertInt(v + 1);
        }
        for (int i = -1001; i < 1002; i++) {
            assertInt(i);
        }
        for (int j = 1; j < 10; j++) {
            int v = IntMath.checkedPow(10, j);
            assertInt(v - 1);
            assertInt(v);
            assertInt(v + 1);
        }
        assertInt(Integer.MAX_VALUE - 1);
        assertInt(Integer.MAX_VALUE);
    }

    private void assertLong(long value) {
//        System.out.println(value);
        SimpleByteArrayOutputStream stream = new SimpleByteArrayOutputStream(16);
        ByteConvertUtils.writeLong(stream, value);

        Assert.assertEquals(Long.toString(value), new String(stream.toByteArray()));
    }

    private void assertInt(int value) {
//        System.out.println(value);
        SimpleByteArrayOutputStream stream = new SimpleByteArrayOutputStream(16);
        ByteConvertUtils.writeInt(stream, value);

        Assert.assertEquals(Integer.toString(value), new String(stream.toByteArray()));
    }
}
