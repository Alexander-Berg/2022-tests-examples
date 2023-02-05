package ru.yandex.disk.client;

import com.yandex.disk.client.exceptions.WebdavException;
import org.junit.Test;
import ru.yandex.disk.test.Reflector;
import ru.yandex.disk.test.TestCase2;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class IndexParserTest extends TestCase2 {

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        Reflector.scrub(this);
    }

    @Test
    public void testReadIntEncoding() throws IOException {

        InputStream is624485 = new ByteArrayInputStream(new byte[]{(byte) 0xE5, (byte) 0x8E, (byte) 0x26});
        InputStream is123456789 = new ByteArrayInputStream(new byte[]{(byte) 149, (byte) 154, (byte) 239, (byte) 58, (byte) 0xFF});
        InputStream is2345767585 = new ByteArrayInputStream(new byte[]{(byte)161, (byte)165, (byte)198, (byte)222, (byte)8});

        IndexParser indexParser = new IndexParser();
        try {
            assertEquals(624485, indexParser.parseLEB128(is624485));
            assertEquals(123456789, indexParser.parseLEB128(is123456789));
            assertEquals(2345767585L, indexParser.parseLEB128(is2345767585));
        } finally {
            is624485.close();
            is123456789.close();
            is2345767585.close();
        }
    }

    @Test
    public void testMediaExtentionsParsing() throws Exception {
        final BufferedInputStream is = new BufferedInputStream(getClass().getResourceAsStream("/com/yandex/disk/client/mediaExt.bin"),1024);

        final ArrayList<IndexItem> itemsFromIndex = new ArrayList<>();

        IndexParser indexParser = new IndexParser();
        indexParser.parse(is, new IndexParsingHandler() {
            @Override
            public void setNextEtag(String path, String etag) {
            }

            @Override
            public void handleItem(IndexItem item) throws IOException, WebdavException {
                itemsFromIndex.add(item);
            }
        });
        is.close();

        IndexItem checkItem = null;
        for (IndexItem item : itemsFromIndex) {
            if(item.getFullPath().equals("Добро Пожаловать.pdf")){
                checkItem = item;
            }
        }
        assertEquals(1361365512000L, checkItem.getLastModified());
        assertEquals(0, checkItem.getEtime());
        assertEquals(true, checkItem.hasThumbnail());
        assertEquals("document", checkItem.getMediaType());
        assertEquals("application/pdf", checkItem.getMimeType());
    }
}