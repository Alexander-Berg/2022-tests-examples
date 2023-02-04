package ru.yandex.solomon.codec.serializer;

import java.util.Arrays;

import com.google.protobuf.ByteString;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

/**
 * @author Stepan Koltsov
 */
public class ByteStringsStockpileTest {

    private void testSplit(int maxSize, byte[] input, byte[]... expected) {
        ByteString[] actual = ByteStringsStockpile.split(ByteString.copyFrom(input), maxSize);
        ByteString[] expectedBs = Arrays.stream(expected).map(ByteString::copyFrom).toArray(ByteString[]::new);
        assertArrayEquals(expectedBs, actual);
    }

    @Test
    public void split() {
        testSplit(3, new byte[] {});
        testSplit(3, new byte[] { 1 }, new byte[] { 1 });
        testSplit(3, new byte[] { 1, 2 }, new byte[] { 1, 2 });
        testSplit(3, new byte[] { 1, 2, 3 }, new byte[] { 1, 2, 3 });
        testSplit(3, new byte[] { 1, 2, 3, 4 }, new byte[] { 1, 2, 3 }, new byte[] { 4 });
        testSplit(3, new byte[] { 1, 2, 3, 4, 5 }, new byte[] { 1, 2, 3 }, new byte[] { 4, 5 });
        testSplit(3, new byte[] { 1, 2, 3, 4, 5, 6 }, new byte[] { 1, 2, 3 }, new byte[] { 4, 5, 6 });
        testSplit(3, new byte[] { 1, 2, 3, 4, 5, 6, 7 }, new byte[] { 1, 2, 3 }, new byte[] { 4, 5, 6 }, new byte[] { 7 });
    }

    @Test
    public void splitToOne() {
        ByteString expected = ByteString.copyFromUtf8("example");
        ByteString[] result = ByteStringsStockpile.split(expected, expected.size() * 2);
        assertArrayEquals(new ByteString[]{expected}, result);
    }

    @Test
    public void splitWhenSizeEqualToMaxSize() {
        ByteString expected = ByteString.copyFromUtf8("example");
        ByteString[] result = ByteStringsStockpile.split(expected, expected.size());
        assertArrayEquals(new ByteString[]{expected}, result);
    }

    @Test
    public void splitWhenSizeMoreByOneByte() {
        ByteString expected = ByteString.copyFromUtf8("example");
        ByteString[] result = ByteStringsStockpile.split(expected, expected.size() - 1);

        assertArrayEquals(new ByteString[]{
            expected.substring(0, expected.size() - 1),
            expected.substring(expected.size() - 1)}, result);
    }
}
