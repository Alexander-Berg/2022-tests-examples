package ru.yandex.disk.remote.webdav;

import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;
import ru.yandex.disk.test.TestCase2;

import java.io.Reader;
import java.io.StringReader;

public class SaxLikeParserTest extends TestCase2 {

    private static class TestObject {
        final String a;
        final String b;
        final String e;

        public TestObject(String a, String b, String e) {
            this.a = a;
            this.b = b;
            this.e = e;
        }

    }

    private static class TestParser extends ru.yandex.disk.remote.webdav.SaxLikeParser<TestObject> {

        private String a;
        private String b;
        private String e;

        public TestParser(Reader reader) throws XmlPullParserException {
            super(reader);
        }

        @Override
        public void tagStart(String path) {
        }

        @Override
        public void tagEnd(String path, String text) {
            if ("/root/a".equals(path)) {
                a = text;
            }

            if ("/root/b".equals(path)) {
                b = text;
            }

            if ("/root/c/d/e".equals(path)) {
                e = text;
            }
        }

        @Override
        protected TestObject buildResult() {
            return new TestObject(a, b, e);
        }

    }

    @Test
    public void testParse() throws Exception {
        TestParser parser = new TestParser(new StringReader("<root>\n" +
                "\n" +
                "    <a>text a</a>\n" +
                "    <b>text b</b>\n" +
                "\n" +
                "    <c>\n" +
                "        <d>text d</d>\n" +
                "\n" +
                "        <d>\n" +
                "            <e>text e</e>\n" +
                "        </d>\n" +
                "    </c>\n" +
                "\n" +
                "</root>"));

        TestObject testObject = parser.parseAndBuildResult();

        assertEquals("text a", testObject.a);
        assertEquals("text b", testObject.b);
        assertEquals("text e", testObject.e);
    }

    @Test
    public void testTagWithBody() throws Exception {
        TestParser parser = new TestParser(new StringReader("<root>\n" +
                "\n" +
                "    <a>text a</a>\n" +
                "    <b/>\n" +
                "</root>"));

        TestObject testObject = parser.parseAndBuildResult();

        assertEquals("text a", testObject.a);
        assertEquals(null, testObject.b);
    }
}