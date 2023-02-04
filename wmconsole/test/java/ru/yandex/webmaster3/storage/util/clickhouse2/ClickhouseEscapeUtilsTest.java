package ru.yandex.webmaster3.storage.util.clickhouse2;

import java.io.ByteArrayOutputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author aherman
 */
public class ClickhouseEscapeUtilsTest {
    @Test
    public void testUnescape() throws Exception {
        String s = "\\b\\f\\r\\n\\t\\0\\'\\\\";
        BAInputStream is = new BAInputStream() {
            int position = 0;
            byte[] buffer = s.getBytes();

            @Override
            public int read() {
                if (position == buffer.length) {
                    return -1;
                }
                return buffer[position++];
            }

            @Override
            public void resetPosition() {
                position = 0;
            }
        };

        BAInputStream unescape = ClickhouseEscapeUtils.unescape(is);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int copy = IOUtils.copy(unescape, baos);
        Assert.assertEquals(8, copy);
        Assert.assertEquals("\b\f\r\n\t\0'\\", new String(baos.toByteArray()));
    }
}
