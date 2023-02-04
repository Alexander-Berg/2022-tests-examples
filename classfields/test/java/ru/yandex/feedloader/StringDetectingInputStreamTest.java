package ru.yandex.feedloader;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ru.yandex.feedloader.util.StringDetectingInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * User: Dmitrii Tolmachev (sunlight@yandex-team.ru)
 * Date: 19.09.13
 * Time: 18:54
 */
@RunWith(JUnit4.class)
public class StringDetectingInputStreamTest extends AbstractJUnit4SpringContextTests {

    @Test(expected=IllegalArgumentException.class)
    public void test1() throws IOException {
        test("<!dOcTyPe", "<!doctype");
    }

    @Test
    public void test2() throws IOException {
        test("<doctype", "<!doctype");
    }

    @Test(expected=IllegalArgumentException.class)
    public void test3() throws IOException {
        test("xx <!doctype", "<!doctype");
    }

    @Test(expected=IllegalArgumentException.class)
    public void test4() throws IOException {
        test("<!DoCtYpE yy", "<!doctype");
    }

    @Test(expected=IllegalArgumentException.class)
    public void test5() throws IOException {
        test("xx <!DoCtYpE yy", "<!doctype");
    }

    private  void test(final String source, final String pattern) throws IOException{
        final InputStream stream = new ByteArrayInputStream(source.getBytes("UTF-8"));
        final StringDetectingInputStream sdis = new StringDetectingInputStream(stream, pattern);
        try {
            while (sdis.read() != -1) {
            }
        } finally {
            sdis.close();
        }
    }

}
