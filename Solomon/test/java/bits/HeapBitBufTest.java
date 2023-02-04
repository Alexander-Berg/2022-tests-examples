package ru.yandex.solomon.codec.bits;

/**
 * @author Vladimir Gordiychuk
 */
public class HeapBitBufTest extends AbstractBitBufTest {

    @Override
    protected BitBuf allocate() {
        return new HeapBitBuf();
    }

    @Override
    protected BitBuf allocate(byte[] array, long expectedBitLength) {
        return new HeapBitBuf(array, expectedBitLength);
    }
}
