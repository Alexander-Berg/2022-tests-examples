package ru.yandex.webmaster3.storage.util.yt;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.io.CountingInputStream;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.webmaster3.storage.util.ProgressLogInputStream;

/**
 * @author aherman
 */
public class SchemafulDsvParserTest {
    private static final Logger log = LoggerFactory.getLogger(SchemafulDsvParserTest.class);

    private static final String COLUMN_URL = "url";
    private static final String COLUMN_VERDICT = "verdict";
    private static final String COLUMN_THREAT = "threat";
    private static final String COLUMN_SOURCE = "source";
    private static final String COLUMN_IS_MAIN_URL = "is_main_url";
    private static final String COLUMN_HOST_TYPE = "host_type";
    private static final String COLUMN_FIRST_CHECK = "first_check";
    private static final String COLUMN_DOWNLOAD_TIME = "download_time";
    private static final String COLUMN_CHAIN = "chain";


    @Test
    public void testParseSimple() throws Exception {
        Entry[] expected = new Entry[] {
                new Entry("1", "", "22"),
                new Entry("", "333", "4444"),
                new Entry("\t", "\n", "\0"),
        };

        byte[] data = (
                "1"     + "\t"  +  ""   + "\t"  + "22"      + "\n" +
                ""      + "\t"  + "333" + "\t"  + "4444"    + "\n" +
                "\\t"   + "\t"  + "\\n" + "\t"  + "\\0"     + "\n"
        ).getBytes();

        class Mapper implements YtRowMapper<Entry> {
            private Entry entry;

            @Override
            public void nextField(String name, InputStream data) {
                if (entry == null) {
                    entry = new Entry();
                }

                String value = null;
                try {
                    value = IOUtils.toString(data);
                } catch (IOException e) {
                    Assert.fail("Unable to read data: " + e.getMessage());
                }
                switch (name) {
                    case "1": entry.f1 = value; entry.has1 = true; break;
                    case "2": entry.f2 = value; entry.has2 = true; break;
                    case "3": entry.f3 = value; entry.has3 = true; break;
                    default:
                        Assert.fail("Unknown field");
                }
            }

            @Override
            public Entry rowEnd() {
                Entry result = this.entry;
                this.entry = null;
                return result;
            }

            @Override
            public List<String> getColumns() {
                return Lists.newArrayList("1", "2", "3");
            }
        }

        SchemafulDsvParser<Entry> parser = new SchemafulDsvParser<>(new ByteArrayInputStream(data), new Mapper());

        Entry entry;

        Assert.assertTrue(parser.hasNext());
        entry = parser.next();
        Assert.assertEquals(expected[0], entry);

        Assert.assertTrue(parser.hasNext());
        entry = parser.next();
        Assert.assertEquals(expected[1], entry);

        Assert.assertTrue(parser.hasNext());
        entry = parser.next();
        Assert.assertEquals(expected[2], entry);

        Assert.assertFalse(parser.hasNext());
    }

    @Test
    public void testBuffer() throws Exception {
        SchemafulDsvParser.Buffer b = new SchemafulDsvParser.Buffer(16);
        Assert.assertEquals(0, b.position);
        Assert.assertEquals(0, b.end);
        b.append((byte) '1');
        Assert.assertEquals(0, b.position);
        Assert.assertEquals(1, b.end);
        b.append((byte) '2');
        Assert.assertEquals(0, b.position);
        Assert.assertEquals(2, b.end);
        b.append((byte) '3');
        Assert.assertEquals(0, b.position);
        Assert.assertEquals(3, b.end);

        Assert.assertEquals("123", new String(b.buffer, 0, b.end));
    }

    @Test
    public void testBuffer1() throws Exception {
        InputStream is = IOUtils.toInputStream("123");

        SchemafulDsvParser.Buffer b = new SchemafulDsvParser.Buffer(16);
        Assert.assertEquals(0, b.position);
        Assert.assertEquals(0, b.end);
        b.fill(is, 1);
        Assert.assertEquals(0, b.position);
        Assert.assertEquals(1, b.end);
        b.fill(is, 1);
        Assert.assertEquals(0, b.position);
        Assert.assertEquals(2, b.end);
        b.fill(is, 1);
        Assert.assertEquals(0, b.position);
        Assert.assertEquals(3, b.end);

        Assert.assertEquals("123", new String(b.buffer, 0, b.end));
    }

    @Test
    public void testFieldIterator() throws Exception {
        byte[] data = (
                "11\t12\n" +
                "21\t22\n"
        ).getBytes();

        SchemafulDsvParser.FieldIterator fieldIterator = new SchemafulDsvParser.FieldIterator(new ByteArrayInputStream(data), 1024);
        InputStream fieldIS;

        Assert.assertTrue(fieldIterator.hasNext());
        fieldIS = fieldIterator.next();
        Assert.assertEquals("11", IOUtils.toString(fieldIS));
        Assert.assertEquals(SchemafulDsvParser.Delimiter.FIELD_END, fieldIterator.getDelimiter());

        Assert.assertTrue(fieldIterator.hasNext());
        fieldIS = fieldIterator.next();
        Assert.assertEquals("12", IOUtils.toString(fieldIS));
        Assert.assertEquals(SchemafulDsvParser.Delimiter.ROW_END, fieldIterator.getDelimiter());

        Assert.assertTrue(fieldIterator.hasNext());
        fieldIS = fieldIterator.next();
        Assert.assertEquals("21", IOUtils.toString(fieldIS));
        Assert.assertEquals(SchemafulDsvParser.Delimiter.FIELD_END, fieldIterator.getDelimiter());

        Assert.assertTrue(fieldIterator.hasNext());
        fieldIS = fieldIterator.next();
        Assert.assertEquals("22", IOUtils.toString(fieldIS));
        Assert.assertEquals(SchemafulDsvParser.Delimiter.ROW_END, fieldIterator.getDelimiter());

        Assert.assertFalse(fieldIterator.hasNext());
    }

    @Test
    public void testStreamCorrectEnd() throws Exception {
        byte[] data = "1\n2\n3\n4\n".getBytes();
        SchemafulDsvParser.FieldIterator fieldIterator = new SchemafulDsvParser.FieldIterator(new ByteArrayInputStream(data), 1024);
        InputStream fieldIS;

        fieldIS = fieldIterator.next();
        Assert.assertEquals("1", IOUtils.toString(fieldIS));
        Assert.assertEquals(SchemafulDsvParser.Delimiter.ROW_END, fieldIterator.getDelimiter());

        fieldIS = fieldIterator.next();
        Assert.assertEquals("2", IOUtils.toString(fieldIS));
        Assert.assertEquals(SchemafulDsvParser.Delimiter.ROW_END, fieldIterator.getDelimiter());

        fieldIS = fieldIterator.next();
        Assert.assertEquals("3", IOUtils.toString(fieldIS));
        Assert.assertEquals(SchemafulDsvParser.Delimiter.ROW_END, fieldIterator.getDelimiter());

        fieldIS = fieldIterator.next();
        Assert.assertEquals("4", IOUtils.toString(fieldIS));
        Assert.assertEquals(SchemafulDsvParser.Delimiter.ROW_END, fieldIterator.getDelimiter());
    }

    @Test
    public void testStreamCorrectEnd1() throws Exception {
        byte[] data = "1\n2\n3\n4".getBytes();
        SchemafulDsvParser.FieldIterator fieldIterator = new SchemafulDsvParser.FieldIterator(new ByteArrayInputStream(data), 1024);
        InputStream fieldIS;

        fieldIS = fieldIterator.next();
        Assert.assertEquals("1", IOUtils.toString(fieldIS));
        Assert.assertEquals(SchemafulDsvParser.Delimiter.ROW_END, fieldIterator.getDelimiter());

        fieldIS = fieldIterator.next();
        Assert.assertEquals("2", IOUtils.toString(fieldIS));
        Assert.assertEquals(SchemafulDsvParser.Delimiter.ROW_END, fieldIterator.getDelimiter());

        fieldIS = fieldIterator.next();
        Assert.assertEquals("3", IOUtils.toString(fieldIS));
        Assert.assertEquals(SchemafulDsvParser.Delimiter.ROW_END, fieldIterator.getDelimiter());

        fieldIS = fieldIterator.next();
        Assert.assertEquals("4", IOUtils.toString(fieldIS));
        Assert.assertEquals(SchemafulDsvParser.Delimiter.ROW_END, fieldIterator.getDelimiter());
    }

    @Test
    public void testLongField() throws Exception {
        String expected =
                "1234567890"
                + "1234567890"
                + "1234567890"
                + "1234567890"
                + "1234567890";
        SchemafulDsvParser.FieldIterator fieldIterator =
                new SchemafulDsvParser.FieldIterator(new ByteArrayInputStream(expected.getBytes()), 16);
        InputStream fieldIS;

        fieldIS = fieldIterator.next();
        Assert.assertEquals(expected, IOUtils.toString(fieldIS));
        Assert.assertEquals(SchemafulDsvParser.Delimiter.ROW_END, fieldIterator.getDelimiter());
    }

    @Test
    public void testLongStream() throws Exception {
        String expected = "1234567890";
        byte[] data = (
                expected + "\n"
                + expected + "\n"
                + expected + "\n"
                + expected + "\n"
                + expected + "\n"
        ).getBytes();
        SchemafulDsvParser.FieldIterator fieldIterator =
                new SchemafulDsvParser.FieldIterator(new ByteArrayInputStream(data), 16);
        InputStream fieldIS;

        fieldIS = fieldIterator.next();
        Assert.assertEquals(expected, IOUtils.toString(fieldIS));
        Assert.assertEquals(SchemafulDsvParser.Delimiter.ROW_END, fieldIterator.getDelimiter());

        fieldIS = fieldIterator.next();
        Assert.assertEquals(expected, IOUtils.toString(fieldIS));
        Assert.assertEquals(SchemafulDsvParser.Delimiter.ROW_END, fieldIterator.getDelimiter());

        fieldIS = fieldIterator.next();
        Assert.assertEquals(expected, IOUtils.toString(fieldIS));
        Assert.assertEquals(SchemafulDsvParser.Delimiter.ROW_END, fieldIterator.getDelimiter());

        fieldIS = fieldIterator.next();
        Assert.assertEquals(expected, IOUtils.toString(fieldIS));
        Assert.assertEquals(SchemafulDsvParser.Delimiter.ROW_END, fieldIterator.getDelimiter());

        fieldIS = fieldIterator.next();
        Assert.assertEquals(expected, IOUtils.toString(fieldIS));
        Assert.assertEquals(SchemafulDsvParser.Delimiter.ROW_END, fieldIterator.getDelimiter());
    }

    @Test
    public void testUnescapeField1() throws Exception {
        byte[] data = "1\t".getBytes();
        SchemafulDsvParser.FieldIterator fieldIterator = new SchemafulDsvParser.FieldIterator(new ByteArrayInputStream(data), 1024);
        InputStream fieldIS;

        Assert.assertTrue(fieldIterator.hasNext());
        fieldIS = fieldIterator.next();
        Assert.assertEquals("1", IOUtils.toString(fieldIS));
        Assert.assertEquals(SchemafulDsvParser.Delimiter.FIELD_END, fieldIterator.getDelimiter());

        Assert.assertFalse(fieldIterator.hasNext());
    }

    @Test
    public void testUnescapeField2() throws Exception {
        byte[] data = "1\n".getBytes();
        SchemafulDsvParser.FieldIterator fieldIterator = new SchemafulDsvParser.FieldIterator(new ByteArrayInputStream(data), 1024);
        InputStream fieldIS;

        Assert.assertTrue(fieldIterator.hasNext());
        fieldIS = fieldIterator.next();
        Assert.assertEquals("1", IOUtils.toString(fieldIS));
        Assert.assertEquals(SchemafulDsvParser.Delimiter.ROW_END, fieldIterator.getDelimiter());

        Assert.assertFalse(fieldIterator.hasNext());
    }

    @Test
    public void testUnescapeField3() throws Exception {
        byte[] data = ("1\\t\\n\\0\\\\" +"\t").getBytes();
        SchemafulDsvParser.FieldIterator fieldIterator = new SchemafulDsvParser.FieldIterator(new ByteArrayInputStream(data), 1024);
        InputStream fieldIS;

        Assert.assertTrue(fieldIterator.hasNext());
        fieldIS = fieldIterator.next();
        Assert.assertEquals("1\t\n\0\\", IOUtils.toString(fieldIS));
        Assert.assertEquals(SchemafulDsvParser.Delimiter.FIELD_END, fieldIterator.getDelimiter());

        Assert.assertFalse(fieldIterator.hasNext());
    }

    @Test
    public void testUnescapeField5() throws Exception {
        byte[] data = "1\\t1234567890\\nqwertyuiop\\0asdfghjkl".getBytes();
        SchemafulDsvParser.FieldIterator fieldIterator = new SchemafulDsvParser.FieldIterator(new ByteArrayInputStream(data), 1024);
        InputStream fieldIS;

        Assert.assertTrue(fieldIterator.hasNext());
        fieldIS = fieldIterator.next();
        Assert.assertEquals("1\t1234567890\nqwertyuiop\0asdfghjkl", IOUtils.toString(fieldIS));
        Assert.assertEquals(SchemafulDsvParser.Delimiter.ROW_END, fieldIterator.getDelimiter());

        Assert.assertFalse(fieldIterator.hasNext());
    }

    @Test
    public void testUnescapeField6() throws Exception {
        byte[] data = "\t".getBytes();
        SchemafulDsvParser.FieldIterator fieldIterator = new SchemafulDsvParser.FieldIterator(new ByteArrayInputStream(data), 1024);
        InputStream fieldIS;

        Assert.assertTrue(fieldIterator.hasNext());
        fieldIS = fieldIterator.next();
        Assert.assertEquals("", IOUtils.toString(fieldIS));
        Assert.assertEquals(SchemafulDsvParser.Delimiter.FIELD_END, fieldIterator.getDelimiter());

        Assert.assertFalse(fieldIterator.hasNext());
    }

    @Test
    public void testUnescapeField7() throws Exception {
        byte[] data = "\n".getBytes();
        SchemafulDsvParser.FieldIterator fieldIterator = new SchemafulDsvParser.FieldIterator(new ByteArrayInputStream(data), 1024);
        InputStream fieldIS;

        Assert.assertTrue(fieldIterator.hasNext());
        fieldIS = fieldIterator.next();
        Assert.assertEquals("", IOUtils.toString(fieldIS));
        Assert.assertEquals(SchemafulDsvParser.Delimiter.ROW_END, fieldIterator.getDelimiter());

        Assert.assertFalse(fieldIterator.hasNext());
    }

    @Test
    public void testUnescapeField8() throws Exception {
        byte[] data = "a\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\".getBytes();
        SchemafulDsvParser.FieldIterator fieldIterator = new SchemafulDsvParser.FieldIterator(new ByteArrayInputStream(data), 16);
        InputStream fieldIS;

        Assert.assertTrue(fieldIterator.hasNext());
        fieldIS = fieldIterator.next();
        Assert.assertEquals("a\\\\\\\\\\\\\\\\\\\\", IOUtils.toString(fieldIS));
        Assert.assertEquals(SchemafulDsvParser.Delimiter.ROW_END, fieldIterator.getDelimiter());

        Assert.assertFalse(fieldIterator.hasNext());
    }

    @Ignore
    @Test
    public void testReadStream() throws Exception {
        for (int i = 0; i < 10; i++) {
            InputStream is = this.getClass().getClassLoader().getResourceAsStream("infected_urls.data");
            InputStream is1 = this.getClass().getClassLoader().getResourceAsStream("infected_urls.data");

            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(is1));

                InfectedYtRowMapper mapper = mapper();
                ProgressLogInputStream pis = new ProgressLogInputStream(new CountingInputStream(is), "test");
                SchemafulDsvParser<Infected> parser = new SchemafulDsvParser<>(pis, mapper, (i + 1) * 1024);

                Infected infected = null;
                try {
                    while (parser.hasNext()) {
                        infected = parser.next();

                        String line = br.readLine();
                        String[] parts = line.split("\t");
                        Assert.assertEquals("Line: " + mapper.lines, parts[0], new String(infected.chain));
                        Assert.assertEquals("Line: " + mapper.lines, parts[1], new String(infected.downloadTime));
                        Assert.assertEquals("Line: " + mapper.lines, parts[2], new String(infected.firstCheck));
                        Assert.assertEquals("Line: " + mapper.lines, parts[3], new String(infected.hostType));
                        Assert.assertEquals("Line: " + mapper.lines, parts[4], new String(infected.isMainUrl));
                        Assert.assertEquals("Line: " + mapper.lines, parts[5], new String(infected.source));
                    }
                } catch (Exception e) {
                    log.error("Error", e);
                }
                if (infected != null) {
                    log.info("Lines: {}", mapper.lines);
                }
            } finally {
                IOUtils.closeQuietly(is);
                IOUtils.closeQuietly(is1);
            }
        }
    }

    private static InfectedYtRowMapper mapper() {
        return new InfectedYtRowMapper();
    }

    private static class Infected {
        public byte[] chain;
        public byte[] downloadTime;
        public byte[] firstCheck;
        public byte[] hostType;
        public byte[] isMainUrl;
        public byte[] source;
        public byte[] thread;
        public byte[] url;
        public byte[] verdict;
    }

    private static class Entry {
        String f1;
        String f2;
        String f3;

        boolean has1 = false;
        boolean has2 = false;
        boolean has3 = false;

        public Entry() {
        }

        public Entry(String f1, String f2, String f3) {
            this.f1 = f1;
            this.f2 = f2;
            this.f3 = f3;
            this.has1 = true;
            this.has2 = true;
            this.has3 = true;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Entry entry = (Entry) o;

            if (has1 != entry.has1) {
                return false;
            }
            if (has2 != entry.has2) {
                return false;
            }
            if (has3 != entry.has3) {
                return false;
            }
            if (f1 != null ? !f1.equals(entry.f1) : entry.f1 != null) {
                return false;
            }
            if (f2 != null ? !f2.equals(entry.f2) : entry.f2 != null) {
                return false;
            }
            return f3 != null ? f3.equals(entry.f3) : entry.f3 == null;

        }

        @Override
        public int hashCode() {
            int result = f1 != null ? f1.hashCode() : 0;
            result = 31 * result + (f2 != null ? f2.hashCode() : 0);
            result = 31 * result + (f3 != null ? f3.hashCode() : 0);
            result = 31 * result + (has1 ? 1 : 0);
            result = 31 * result + (has2 ? 1 : 0);
            result = 31 * result + (has3 ? 1 : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "f1='" + f1 + '\'' +
                    ", f2='" + f2 + '\'' +
                    ", f3='" + f3 + '\'' +
                    '}';
        }
    }

    private static class InfectedYtRowMapper implements YtRowMapper<Infected> {
        private Infected infected = new Infected();
        int lines = 0;

        @Override
        public void nextField(String name, InputStream data) {
            byte[] value;
            try {
                value = IOUtils.toByteArray(data);
            } catch (IOException e) {
                throw new RuntimeException("Unable to read data", e);
            }
            switch (name) {
                case COLUMN_CHAIN: infected.chain = value; break;
                case COLUMN_DOWNLOAD_TIME: infected.downloadTime = value; break;
                case COLUMN_FIRST_CHECK: infected.firstCheck = value; break;
                case COLUMN_HOST_TYPE: infected.hostType = value; break;
                case COLUMN_IS_MAIN_URL: infected.isMainUrl = value; break;
                case COLUMN_SOURCE: infected.source = value; break;
                case COLUMN_THREAT: infected.thread = value; break;
                case COLUMN_URL: infected.url = value; break;
                case COLUMN_VERDICT: infected.verdict = value; break;
            }
        }

        @Override
        public Infected rowEnd() {
            Infected result = infected;
            infected = new Infected();
            lines++;
            return result;
        }

        @Override
        public List<String> getColumns() {
            return Lists.newArrayList(
                    COLUMN_CHAIN,
                    COLUMN_DOWNLOAD_TIME,
                    COLUMN_FIRST_CHECK,
                    COLUMN_HOST_TYPE,
                    COLUMN_IS_MAIN_URL,
                    COLUMN_SOURCE,
                    COLUMN_THREAT,
                    COLUMN_URL,
                    COLUMN_VERDICT
            );
        }
    }
}
