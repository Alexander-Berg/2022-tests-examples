package ru.yandex.webmaster3.storage.util.yt;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Ignore;
import ru.yandex.common.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * @author aherman
 */
public class TableWriterImplTest {
    public static final String C_INT = "fInt";
    public static final String C_LONG = "fLong";
    public static final String C_STRING = "fString";

    @Ignore
    public void simpleRows() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        YtCypressServiceImpl.TableWriterImpl tw = new YtCypressServiceImpl.TableWriterImpl(
                new CloseTrackingOutputStream(baos)
        );

        tw.column(C_INT, 1)
                .column(C_LONG, 2L)
                .column(C_STRING, "3 str")
                .rowEnd();

        tw.column(C_INT, 4)
                .column(C_LONG, 5L)
                .column(C_STRING, "6 str")
                .rowEnd();
        tw.close();

        ArrayList<String> lines = Lists.newArrayList(
                IOUtils.readLines(new ByteArrayInputStream(baos.toByteArray()), StandardCharsets.UTF_8));

        Assert.assertEquals(2, lines.size());
        {
            Entry entry = YtService.OM.readValue(lines.get(0), Entry.class);
            Assert.assertEquals(1, entry.fInt);
            Assert.assertEquals(2L, entry.fLong);
            Assert.assertEquals("3 str", entry.fString);
        }

        {
            Entry entry = YtService.OM.readValue(lines.get(1), Entry.class);
            Assert.assertEquals(4, entry.fInt);
            Assert.assertEquals(5L, entry.fLong);
            Assert.assertEquals("6 str", entry.fString);
        }
    }

    @Ignore
    public void newLineRows() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        YtCypressServiceImpl.TableWriterImpl tw = new YtCypressServiceImpl.TableWriterImpl(
                new CloseTrackingOutputStream(baos)
        );

        tw.column(C_STRING, "1 2 3\n4 5 6").rowEnd();
        tw.column(C_STRING, "7 8 9\n10 11 12").rowEnd();
        tw.close();

        ArrayList<String> lines = Lists.newArrayList(
                IOUtils.readLines(new ByteArrayInputStream(baos.toByteArray()), StandardCharsets.UTF_8));

        Assert.assertEquals(2, lines.size());
        Assert.assertEquals("{\"fString\":\"1 2 3\\n4 5 6\"}", lines.get(0));
        Assert.assertEquals("{\"fString\":\"7 8 9\\n10 11 12\"}", lines.get(1));
    }

    private static class Entry {
        private int fInt;
        private long fLong;
        private String fString;

        public int getfInt() {
            return fInt;
        }

        public void setfInt(int fInt) {
            this.fInt = fInt;
        }

        public long getfLong() {
            return fLong;
        }

        public void setfLong(long fLong) {
            this.fLong = fLong;
        }

        public String getfString() {
            return fString;
        }

        public void setfString(String fString) {
            this.fString = fString;
        }
    }

    private static class CloseTrackingOutputStream extends OutputStream {
        private boolean closed = false;
        private final OutputStream delegate;

        private CloseTrackingOutputStream(OutputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public void write(int b) throws IOException {
            assertClosed();
            delegate.write(b);
        }

        @Override
        public void close() throws IOException {
            assertClosed();
            closed = true;
            delegate.close();
        }

        private void assertClosed() throws IOException {
            if (closed) {
                throw new IOException("Stream is closed");
            }
        }
    }
}
