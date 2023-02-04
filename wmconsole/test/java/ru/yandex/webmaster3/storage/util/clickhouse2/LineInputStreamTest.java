package ru.yandex.webmaster3.storage.util.clickhouse2;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * Created by ifilippov5 on 01.04.18.
 */
public class LineInputStreamTest {

    @Test
    public void test() {
        InputStreamImpl content = new InputStreamImpl();
        LineInputStream lineInputStream = new LineInputStream(content, 10);
        Iterator<LineInputStream.Line> lineIterator = lineInputStream.iterator();
        String answer = "";
        while (lineIterator.hasNext()) {
            LineInputStream.Line line = lineIterator.next();
            if (!answer.isEmpty()) {
                answer += '\n';
            }
            answer += line.getString(0, StandardCharsets.UTF_8);
        }
        Assert.assertEquals(content.str, answer);
    }

    public static class InputStreamImpl extends InputStream {
        private String str;
        private byte[] data;
        private int pos;

        public InputStreamImpl() {
            str = "";
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 10000; j++) {
                    str += (char)('a' + i);
                }
                if (i + 1 < 5) {
                    str += '\n';
                }
            }
            data = str.getBytes();
            pos = 0;
        }

        @Override
        public int read() throws IOException {
            if (pos == data.length) {
                return -1;
            }
            return data[pos++];
        }
    }
}
