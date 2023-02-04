package ru.yandex.webmaster3.storage.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.google.common.primitives.Bytes;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author aherman
 */
public class LenvalStreamParserTest {

    @Test
    public void testProcess() throws Exception {
        byte key[] = new byte[] {0x1, 0x2, 0x3, 0x4, 0x5};
        byte value[] = new byte[] {0x6, 0x7, 0x8};

        byte stream[] = Bytes.concat(
                new byte[] {0x5, 0x0, 0x0, 0x0},
                key,

                new byte[] {0x3, 0x0, 0x0, 0x0},
                value
        );
        LenvalStreamParser parser = new LenvalStreamParser(new ByteArrayInputStream(stream));
        ByteArrayOutputStream keyStream = new ByteArrayOutputStream();
        ByteArrayOutputStream valueStream = new ByteArrayOutputStream();
        parser.process(new LenvalStreamParser.EntryProcessor() {
            @Override
            public void processKey(InputStream is) {
                try {
                    IOUtils.copy(is, keyStream);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void processValue(InputStream is) {
                try {
                    IOUtils.copy(is, valueStream);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        Assert.assertArrayEquals(key, keyStream.toByteArray());
        Assert.assertArrayEquals(value, valueStream.toByteArray());
    }

    @Test
    public void testBoundedInputStream() throws Exception {
        byte[] stream = new byte[] {
                0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9
        };
        byte[] expected = new byte[] {
                0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7
        };

        LenvalStreamParser.BoundedInputStream is = new LenvalStreamParser.BoundedInputStream(new ByteArrayInputStream(stream), 7);
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        IOUtils.copy(is, actual);
        Assert.assertArrayEquals(expected, actual.toByteArray());
    }

    @Test
    public void testSaveLoad() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte key[] = new byte[] {0x1, 0x2, 0x3, 0x4, 0x5};
        byte value[] = new byte[] {0x6, 0x7, 0x8};

        LenvalSaver.writeBytes(baos, key);
        LenvalSaver.writeBytes(baos, value);

        LenvalStreamParser parser = new LenvalStreamParser(new ByteArrayInputStream(baos.toByteArray()));
        ByteArrayOutputStream keyStream = new ByteArrayOutputStream();
        ByteArrayOutputStream valueStream = new ByteArrayOutputStream();
        parser.process(new LenvalStreamParser.EntryProcessor() {
            @Override
            public void processKey(InputStream is) {
                try {
                    IOUtils.copy(is, keyStream);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void processValue(InputStream is) {
                try {
                    IOUtils.copy(is, valueStream);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        Assert.assertArrayEquals(key, keyStream.toByteArray());
        Assert.assertArrayEquals(value, valueStream.toByteArray());
    }
}
