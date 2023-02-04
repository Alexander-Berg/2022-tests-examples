package ru.yandex.webmaster3.storage.util.yt;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.util.ByteStreamUtil;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.storage.util.LenvalStreamParser;
import ru.yandex.webmaster3.storage.util.clickhouse2.ByteConvertUtils;

/**
 * @author aherman
 */
public class YtSnappyCompressedStreamTest {
    private static final byte[] EMPTY_STREAM =
            new byte[]{0x53, 0x6e, 0x61, 0x70, 0x01, 0x00, 0x00, 0x00, 0x00, (byte) 0x80};

    private static final long[] STREAM = new long[] {
            0x536e617001000000L, 0x0080d20401b40ef0L, 0x7b13000000687474L, 0x703a2f2f30336f6eL,
            0x6c696e652e636f6dL, 0xd40000000a8001d1L, 0x80d0b5d0b3d0b8d1L, 0x81d182d180d0b8d1L,
            0x80d183d18ed186d1L, 0x83d18f20d0b0d180L, 0xd182d0b8d0bcd0b8L, 0xd0b820d181d0b5d1L,
            0x80d0b4d186d0b020L, 0xd0b7d0b4d0bed180L, 0xd0bed0b2d18bd185L, 0x20d0bbd18ed0b4d0L,
            0xb5d0b920d0bad0beL, 0xd0bbd0be20094104L, 0xd182054104b92005L, 0x2a1488d0b520d0bdL,
            0x053b10bcd18b123fL, 0x4a9c00f03c2f6e65L, 0x77732f7a68656c75L, 0x646f63686b6f7679L,
            0x655f617269746d69L, 0x692f323031352d36L, 0x2d32372d39303232L, 0x39180120002a0a08L,
            0x0410011800200628L, 0x001356ef0000a201L, 0xef1043d0b4d0b201L, 0x7f04bfd005a014d1L,
            0x81d0bad0b8018e1cL, 0xb020d182d0b5d181L, 0x0106091008d0b1d0L, 0x01ed18b5d0bcd0b5L,
            0xd0bd05b12123088cL, 0x124b62b10058676cL, 0x7570796a5f6b6f6eL, 0x6563686e6f5f766fL,
            0x70726f735f010a18L, 0x736a6f5f7a686501L, 0xbd38322d31302d32L, 0x342d333234301801L,
            0x2001bd040b1001bdL, 0x0420285ebd0000c2L, 0x01bd006901b604d1L, 0x87019204d18301a4L,
            0x60b520d0b4d0b0d1L, 0x8ed18220d0b8d0bdL, 0xd0b2d0b0d0bbd0b8L, 0xd0b419ad14d0bfd1L,
            0x80d0b80539008205L, 0xcf0cb820d0b725f3L, 0x10bdd0b8d18f0146L, 0x14b020d0bed0b401L,
            0x412820d0b3d0bbd0L, 0xb0d0b7124562e300L, 0x7070726f70616c6fL, 0x5f7a72656e69655fL,
            0x6e615f6f646e6f6dL, 0x5f676c617a7501deL, 0x08332d3401dd0c34L, 0x3938370ddd7e9a01L,
            0x00d901dd0061217dL, 0x01be002005ad010bL, 0x08b520d0257b2577L, 0x1880d183d0b0d186L,
            0x458705e300b501f6L, 0x00ba4d7f008c0114L, 0x14b720d0b7d0b005L, 0xfe41d204be2005fdL,
            0x08bed18505e008b0L, 0x126466d500e06f63L, 0x68656d755f696465L, 0x745f6b726f765f69L,
            0x7a5f7a61646e6567L, 0x6f5f70726f686f64L, 0x615f766f5f767265L, 0x6d79615f6d656e73L,
            0x7472756174736945L, 0x8e28342d382d3132L, 0x2d3332333731d109L, 0xf4000762d1010066L,
            0x01f40813d0bf25b2L, 0x30b5d18620d184d0L, 0xbed182d0be12660fL, 0x031873686973686bL,
            0x6921742870616c74L, 0x7361685f72756b21L, 0x742c342d312d3135L, 0x2d31313737312d75L,
            0x4952001962810000L, 0x6b0181202ad0bed0L, 0xbfd183d18529791cL, 0xb3d183d0b1d18b20L,
            0x412524b2d0bed0b7L, 0xd0bbd0b520850208L, 0xb0122d623e0161a7L, 0x207479795f6e616cL,
            0x6574212708727475L, 0x328600002f628600L, 0x00840186002d4177L, 0x50d18ed0bad0bed1L,
            0x84d0b0d0b620d0bbL, 0xd0bed0bdd0b3058aL, 0x41d410bad0bed0b3L, 0x61b30cd18c123762L,
            0x890038676c79756bL, 0x6f66617a685f6c6fL, 0x6e67211a64352d37L, 0x2d322d3931323030L,
            0x180420022a0a0802L, 0x1002180120052801L, 0x0c0004050c0c0828L, 0x041356340400aa01L,
            0x9f102fd0bad0b881L, 0xb51cd187d0bdd0b8L, 0xd0ba41a220bdd0b5L, 0xd0b2d0bcd0b0a51eL,
            0x10b7d0b8d180a108L, 0x14d0b0d0bd126762L, 0xa10008706f64212dL, 0x85d4106e6179615fL,
            0x010e5c657a615f70L, 0x6f76797368656e6eL, 0x615f706e65766d61L, 0x74697a415f187961L,
            0x5f6b697368814e08L, 0x696b6101d121eb10L, 0x332d313131a25f03L, 0x008701c52022d0b8L,
            0xd0b3d180d18b01bdL, 0x18bed0bcd0bed189L, 0x613608b4d0b601b4L, 0x61fe04125166b800L,
            0xb06572656e6f735fL, 0x6d6573796163686eL, 0x79685f735f706f6dL, 0x6f7368636879755fL,
            0x6f6b5f7a68616e69L, 0x6e2f32303133610eL, 0x1c342d3636313618L, 0x028dde1002180020L,
            0x1f620602008f01a2L, 0x083ad0b129e10cd1L, 0x88d0b861fc21f40cL, 0xbbd18cd1c56c00bdL,
            0x81a7adb5c5450485L, 0x2049af14d0b0d185L, 0x124162ba002e3403L, 0x2c6f6c6f7679685fL,
            0x6775626168214b28L, 0x332d31312d31332dL, 0x3931367136200410L, 0x0118002005280000L,
            0x0000L,
    };
    private static WebmasterHostId EXPECTED_KEY = IdUtils.stringToHostId("http:03online.com:80");
    private static int[] EXPECTED_VALUE_SIZE = new int[] {
            212, 162, 194, 217, 102, 107, 132, 170, 135, 143,
    };

    @Test
    public void testEmptyStream() throws Exception {
        byte[] data = EMPTY_STREAM;

        YtSnappyCompressedStream stream = new YtSnappyCompressedStream(new ByteArrayInputStream(data));
        Assert.assertEquals(-1, stream.read());
    }

    @Test
    public void testNormalStream() throws Exception {
        byte[] data = new byte[STREAM.length * 8];
        for (int i = 0; i < STREAM.length; i++) {
            ByteStreamUtil.writeLongBE(data, i * 8, STREAM[i]);
        }
        YtSnappyCompressedStream stream = new YtSnappyCompressedStream(new ByteArrayInputStream(data));
        int size = IOUtils.copy(stream, new NullOutputStream());
        Assert.assertEquals(1844, size);

        stream = new YtSnappyCompressedStream(new ByteArrayInputStream(data));
        LenvalStreamParser parser = new LenvalStreamParser(stream);
        MutableInt count = new MutableInt();
        parser.process(new LenvalStreamParser.EntryProcessor() {
            @Override
            public void processKey(InputStream is) throws Exception {
                Assert.assertEquals(EXPECTED_KEY, ByteConvertUtils.parseUrlAsHostId(is));
            }

            @Override
            public void processValue(InputStream is) throws Exception {
                int valueSize = IOUtils.copy(is, new NullOutputStream());
                Assert.assertEquals(EXPECTED_VALUE_SIZE[count.intValue()], valueSize);
                count.increment();
            }
        });
        Assert.assertEquals(10, count.intValue());
    }
}