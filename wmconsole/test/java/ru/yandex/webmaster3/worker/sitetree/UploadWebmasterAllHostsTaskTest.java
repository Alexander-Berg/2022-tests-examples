package ru.yandex.webmaster3.worker.sitetree;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.webmaster3.storage.util.yt.TableWriter;
import ru.yandex.webmaster3.storage.util.yt.YtException;
import ru.yandex.webmaster3.storage.util.yt.YtPath;

/**
 * Created by ifilippov5 on 20.12.16.
 */
public class UploadWebmasterAllHostsTaskTest {

    private UploadWebmasterHostsCommonService uploadWebmasterHostsCommonService = new UploadWebmasterHostsCommonService();

    @Test
    public void testGetOldNodes() {
        DateTime today = DateTime.parse("20161212", UploadWebmasterHostsCommonService.DATE_FORMAT_IN_NODE_NAME);
        List<YtPath> nodes = Arrays.asList(
                YtPath.fromString("cluster://...../archive/webmaster-all-hosts.20161212"),
                YtPath.fromString("cluster://archive.1/h.osts.20161213"),
                YtPath.fromString("cluster://../export/archive.2/webmaster-all-hosts.20161128"),
                YtPath.fromString("cluster://export/archive.2/webmaster-all-hosts.20161129"),
                YtPath.fromString("cluster://export/archive/webmaster-all-hosts.20161128"),
                YtPath.fromString("cluster://../export/archive/webmaster-all-hosts.20151231"));

        List<YtPath> oldNodes = Arrays.asList(
                YtPath.fromString("cluster://../export/archive/webmaster-all-hosts.20151231"));

        Assert.assertEquals(oldNodes, uploadWebmasterHostsCommonService.getOldNodes(nodes, today));
    }

    @Ignore
    @Test
    public void testTableCreatedToday() {
        List<YtPath> nodes = Arrays.asList(
                YtPath.fromString("cluster://...../archive/webmaster-all-hosts.20170620"),
                YtPath.fromString("cluster://archive.1/h.osts.20170619"));

        Assert.assertEquals(true, uploadWebmasterHostsCommonService.tableCreatedToday(nodes.get(0)));
        Assert.assertEquals(false, uploadWebmasterHostsCommonService.tableCreatedToday(nodes.get(1)));
    }

    @Test
    public void testUploadHosts() {
        BufferedReader hr = new HostsReader(new String[]{"host1", "host2", "host3"});
        MockTableWriterImpl tw = new MockTableWriterImpl();

        uploadWebmasterHostsCommonService.uploadHosts(hr, tw, new String[]{"col1", "col2", "col3"}, () -> {});

        Assert.assertTrue(tw.values.containsKey("col1"));
        Assert.assertTrue(tw.values.containsKey("col2"));
        Assert.assertTrue(tw.values.containsKey("col3"));
        Assert.assertEquals(tw.values.get("col1"), "host1");
        Assert.assertEquals(tw.values.get("col2"), "host2");
        Assert.assertEquals(tw.values.get("col3"), "host3");
        Assert.assertEquals(tw.values.size(), 3);
    }

    public static class HostsReader extends BufferedReader {
        private final String[] hosts;
        private int currentPos;

        HostsReader(String[] hosts) {
            super(EasyMock.createMock(Reader.class));
            this.hosts = hosts;
            this.currentPos = 0;
        }

        @Override
        public String readLine() {
            if (currentPos >= hosts.length) return null;
            return hosts[currentPos++];
        }
    }

    public static class MockTableWriterImpl implements TableWriter {
        final Map<String, Object> values = new HashMap<>();

        @Override
        public TableWriter column(String name, Short value) {
            return null;
        }

        @Override
        public TableWriter column(String name, Integer value) {
            return null;
        }

        @Override
        public TableWriter column(String name, Long value) {
            return null;
        }

        @Override
        public TableWriter column(String name, Boolean value) {
            values.put(name, value);
            return this;
        }

        @Override
        public TableWriter column(String name, String value) {
            values.put(name, value);
            return this;
        }

        @Override
        public TableWriter column(String name, byte[] bytes) {
            return this;
        }

        @Override
        public TableWriter columnObject(String name, Object object) {
            return null;
        }

        @Override
        public TableWriter rowEnd() throws YtException {
            return null;
        }
    }
}
