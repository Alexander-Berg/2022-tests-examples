package ru.yandex.webmaster3.storage.util.yt;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
public class SchemafulDsvParser2Test {
    private static final Logger log = LoggerFactory.getLogger(SchemafulDsvParser2Test.class);

    @Test
    public void testRead() throws Exception {
        byte[] data = (
                "0"
                + "\\0"
                + "1"
                + "\\n"
                + "2"
                + "\\t"
                + "3"
                + "\\\\"
                + "4"
                + "\t"
                + "5"
                + "\n"
                + "678"
                + "\t"
                + "901"
                + "\n").getBytes();
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        SchemafulDsvParser2<Entry> parser = new SchemafulDsvParser2<>(bais, getMapper(), 5);

        Entry entry;

        Assert.assertTrue(parser.hasNext());
        entry = parser.next();
        Assert.assertEquals("0\0" + "1\n2\t3\\4", entry.key);
        Assert.assertEquals("5", entry.value);

        Assert.assertTrue(parser.hasNext());
        entry = parser.next();
        Assert.assertEquals("678", entry.key);
        Assert.assertEquals("901", entry.value);

        Assert.assertFalse(parser.hasNext());
    }

    private YtRowMapper<Entry> getMapper() {
        return new YtRowMapper<Entry>() {
            private Entry entry;

            @Override
            public void nextField(String name, InputStream data) {
                if ("key".equals(name)) {
                    if (entry == null) {
                        entry = new Entry();
                    }
                    try {
                        byte[] b = new byte[16];
                        int s = IOUtils.read(data, b);
                        entry.key = new String(b, 0, s);
                    } catch (IOException e) {
                        Assert.fail("Exception in key: " + e.getMessage());
                    }
                } else if ("value".equals(name)) {
                    try {
                        byte[] b = new byte[16];
                        int s = IOUtils.read(data, b);
                        entry.value = new String(b, 0, s);
                    } catch (IOException e) {
                        Assert.fail("Exception in value: " + e.getMessage());
                    }
                }
            }

            @Override
            public Entry rowEnd() {
                Entry res = entry;
                entry = null;
                return res;
            }

            @Override
            public List<String> getColumns() {
                return Lists.newArrayList("key", "value");
            }
        };
    }

    private static class Entry {
        String key;
        String value;
    }

    @Test
    public void testStream() throws Exception {
        byte[] data = (
                "0"
                        + "\\0"
                        + "1"
                        + "\\n"
                        + "2"
                        + "\\t"
                        + "3"
                        + "\\\\"
                        + "4"
                        + "\t"
                        + "5"
                        + "\n"
                        + "678"
                        + "\t"
                        + "901"
                        + "\n").getBytes();

        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        SchemafulDsvParser2<Entry> parser = new SchemafulDsvParser2<>(bais, getMapper(), 5);

        Assert.assertEquals(-1, parser.read());
        parser.state = SchemafulDsvParser2.State.COLUMN_START;
        Assert.assertEquals('0', parser.read());
        Assert.assertEquals(SchemafulDsvParser2.State.COLUMN_START, parser.state);
        Assert.assertEquals('\0', parser.read());
        Assert.assertEquals(SchemafulDsvParser2.State.COLUMN_START, parser.state);
        Assert.assertEquals('1', parser.read());
        Assert.assertEquals(SchemafulDsvParser2.State.COLUMN_START, parser.state);
        Assert.assertEquals('\n', parser.read());
        Assert.assertEquals(SchemafulDsvParser2.State.COLUMN_START, parser.state);
        Assert.assertEquals('2', parser.read());
        Assert.assertEquals(SchemafulDsvParser2.State.COLUMN_START, parser.state);
        Assert.assertEquals('\t', parser.read());
        Assert.assertEquals(SchemafulDsvParser2.State.COLUMN_START, parser.state);
        Assert.assertEquals('3', parser.read());
        Assert.assertEquals(SchemafulDsvParser2.State.COLUMN_START, parser.state);
        Assert.assertEquals('\\', parser.read());
        Assert.assertEquals(SchemafulDsvParser2.State.COLUMN_START, parser.state);
        Assert.assertEquals('4', parser.read());
        Assert.assertEquals(SchemafulDsvParser2.State.COLUMN_START, parser.state);
        Assert.assertEquals(-1, parser.read());
        Assert.assertEquals(SchemafulDsvParser2.State.COLUMN_END, parser.state);
        Assert.assertEquals(-1, parser.read());
        Assert.assertEquals(SchemafulDsvParser2.State.COLUMN_END, parser.state);

        parser.state = SchemafulDsvParser2.State.COLUMN_START;
        Assert.assertEquals('5', parser.read());
        Assert.assertEquals(SchemafulDsvParser2.State.COLUMN_START, parser.state);
        Assert.assertEquals(-1, parser.read());
        Assert.assertEquals(SchemafulDsvParser2.State.ROW_END, parser.state);
        Assert.assertEquals(-1, parser.read());
        Assert.assertEquals(SchemafulDsvParser2.State.ROW_END, parser.state);

        parser.state = SchemafulDsvParser2.State.COLUMN_START;
        Assert.assertEquals('6', parser.read());
        Assert.assertEquals(SchemafulDsvParser2.State.COLUMN_START, parser.state);
        Assert.assertEquals('7', parser.read());
        Assert.assertEquals(SchemafulDsvParser2.State.COLUMN_START, parser.state);
        Assert.assertEquals('8', parser.read());
        Assert.assertEquals(SchemafulDsvParser2.State.COLUMN_START, parser.state);
        Assert.assertEquals(-1, parser.read());
        Assert.assertEquals(SchemafulDsvParser2.State.COLUMN_END, parser.state);
        Assert.assertEquals(-1, parser.read());
        Assert.assertEquals(SchemafulDsvParser2.State.COLUMN_END, parser.state);

        parser.state = SchemafulDsvParser2.State.COLUMN_START;
        Assert.assertEquals('9', parser.read());
        Assert.assertEquals(SchemafulDsvParser2.State.COLUMN_START, parser.state);
        Assert.assertEquals('0', parser.read());
        Assert.assertEquals(SchemafulDsvParser2.State.COLUMN_START, parser.state);
        Assert.assertEquals('1', parser.read());
        Assert.assertEquals(SchemafulDsvParser2.State.COLUMN_START, parser.state);
        Assert.assertEquals(-1, parser.read());
        Assert.assertEquals(SchemafulDsvParser2.State.ROW_END, parser.state);
        Assert.assertEquals(-1, parser.read());
        Assert.assertEquals(SchemafulDsvParser2.State.ROW_END, parser.state);
    }

    @Test
    public void testStream2() throws Exception {
        byte[] data = (
                "0"
                        + "\\0"
                        + "1"
                        + "\\n"
                        + "2"
                        + "\\t"
                        + "3"
                        + "\\\\"
                        + "4"
                        + "\t"
                        + "5"
                        + "\n"
                        + "678"
                        + "\t"
                        + "901"
                        + "\n").getBytes();

        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        SchemafulDsvParser2<Entry> parser = new SchemafulDsvParser2<>(bais, getMapper(), 5);
        int copied;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.state = SchemafulDsvParser2.State.COLUMN_START;
        copied = IOUtils.copy(parser, baos);
        Assert.assertEquals(9, copied);
        Assert.assertEquals("0\0" + "1\n2\t3\\4", baos.toString());
        Assert.assertEquals(SchemafulDsvParser2.State.COLUMN_END, parser.state);
        baos.reset();
        Assert.assertEquals(0, IOUtils.copy(parser, baos));
        Assert.assertEquals(SchemafulDsvParser2.State.COLUMN_END, parser.state);

        parser.state = SchemafulDsvParser2.State.COLUMN_START;
        copied = IOUtils.copy(parser, baos);
        Assert.assertEquals(1, copied);
        Assert.assertEquals("5", baos.toString());
        Assert.assertEquals(SchemafulDsvParser2.State.ROW_END, parser.state);
        baos.reset();
        Assert.assertEquals(0, IOUtils.copy(parser, baos));
        Assert.assertEquals(SchemafulDsvParser2.State.ROW_END, parser.state);

        parser.state = SchemafulDsvParser2.State.COLUMN_START;
        copied = IOUtils.copy(parser, baos);
        Assert.assertEquals(3, copied);
        Assert.assertEquals("678", baos.toString());
        Assert.assertEquals(SchemafulDsvParser2.State.COLUMN_END, parser.state);
        baos.reset();
        Assert.assertEquals(0, IOUtils.copy(parser, baos));
        Assert.assertEquals(SchemafulDsvParser2.State.COLUMN_END, parser.state);

        parser.state = SchemafulDsvParser2.State.COLUMN_START;
        copied = IOUtils.copy(parser, baos);
        Assert.assertEquals(3, copied);
        Assert.assertEquals("901", baos.toString());
        Assert.assertEquals(SchemafulDsvParser2.State.ROW_END, parser.state);
        baos.reset();
        Assert.assertEquals(0, IOUtils.copy(parser, baos));
        Assert.assertEquals(SchemafulDsvParser2.State.ROW_END, parser.state);
    }

    @Test
    public void testParseSimple() throws Exception {
        Entry2[] expected = new Entry2[] {
                new Entry2("1", "", "22"),
                new Entry2("", "333", "4444"),
                new Entry2("\t", "\n", "\0"),
        };

        byte[] data = (
                "1"     + "\t"  +  ""   + "\t"  + "22"      + "\n" +
                        ""      + "\t"  + "333" + "\t"  + "4444"    + "\n" +
                        "\\t"   + "\t"  + "\\n" + "\t"  + "\\0"     + "\n"
        ).getBytes();

        class Mapper implements YtRowMapper<Entry2> {
            private Entry2 entry;

            @Override
            public void nextField(String name, InputStream data) {
                if (entry == null) {
                    entry = new Entry2();
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
            public Entry2 rowEnd() {
                Entry2 result = this.entry;
                this.entry = null;
                return result;
            }

            @Override
            public List<String> getColumns() {
                return Lists.newArrayList("1", "2", "3");
            }
        }

        SchemafulDsvParser<Entry2> parser = new SchemafulDsvParser<>(new ByteArrayInputStream(data), new Mapper());

        Entry2 entry;

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

    private static class Entry2 {
        String f1;
        String f2;
        String f3;

        boolean has1 = false;
        boolean has2 = false;
        boolean has3 = false;

        public Entry2() {
        }

        public Entry2(String f1, String f2, String f3) {
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

            Entry2 entry = (Entry2) o;

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

    private static final String COLUMN_URL = "url";
    private static final String COLUMN_VERDICT = "verdict";
    private static final String COLUMN_THREAT = "threat";
    private static final String COLUMN_SOURCE = "source";
    private static final String COLUMN_IS_MAIN_URL = "is_main_url";
    private static final String COLUMN_HOST_TYPE = "host_type";
    private static final String COLUMN_FIRST_CHECK = "first_check";
    private static final String COLUMN_DOWNLOAD_TIME = "download_time";
    private static final String COLUMN_CHAIN = "chain";

}
