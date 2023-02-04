package ru.yandex.solomon.codec.serializer;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.misc.codec.Hex;
import ru.yandex.misc.codec.HexAssert;

/**
 * @author Stepan Koltsov
 */
public class ProtobufVarintTest {

    private interface WriteInt {
        int write(byte[] array, int offset, int value);
    }

    private interface ReadInt {
        ProtobufVarint.ReadIntResult read(byte[] array, int offset);
    }

    private interface WriteLong {
        int write(byte[] array, int offset, long value);
    }

    private interface ReadLong {
        ProtobufVarint.ReadLongResult read(byte[] array, int offset);
    }

    private void testVarint32Impl(int value, String expected, WriteInt write, ReadInt read) {
        byte[] expectedBytes = Hex.decodeHr(expected);

        byte[] array = Cf.ByteArray.filled(14, (byte) 0x33);
        Assert.assertEquals(expectedBytes.length, write.write(array, 2, value));

        byte[] expectedArray = Cf.ByteArray.filled(14, (byte) 0x33);
        System.arraycopy(expectedBytes, 0, expectedArray, 2, expectedBytes.length);
        HexAssert.assertArraysEqual(expectedArray, array);

        ProtobufVarint.ReadIntResult readResult = read.read(array, 2);
        Assert.assertEquals(value, readResult.value);
        Assert.assertEquals(expectedBytes.length, readResult.length);
    }

    private void testVarint64Impl(long value, String expected, WriteLong write, ReadLong read) {
        byte[] expectedBytes = Hex.decodeHr(expected);

        byte[] array = Cf.ByteArray.filled(14, (byte) 0x33);
        Assert.assertEquals(expectedBytes.length, write.write(array, 2, value));

        byte[] expectedArray = Cf.ByteArray.filled(14, (byte) 0x33);
        System.arraycopy(expectedBytes, 0, expectedArray, 2, expectedBytes.length);
        HexAssert.assertArraysEqual(expectedArray, array);

        ProtobufVarint.ReadLongResult readResult = read.read(array, 2);
        Assert.assertEquals(value, readResult.value);
        Assert.assertEquals(expectedBytes.length, readResult.length);
    }

    private void testVarint32(int value, String expectedSigned, String expectedUnsigned) {
        testVarint32Impl(value, expectedSigned,
            ProtobufVarint::writeSignedVarint32, ProtobufVarint::readSignedVarint32);
        testVarint32Impl(value, expectedUnsigned,
            ProtobufVarint::writeUnsignedVarint32, ProtobufVarint::readUnsignedVarint32);

        Assert.assertEquals(Hex.decodeHr(expectedSigned).length,
            ProtobufVarint.signedVarint32EncodedLength(value));
        Assert.assertEquals(Hex.decodeHr(expectedUnsigned).length,
            ProtobufVarint.unsignedVarint32EncodedLength(value));
    }

    private void testVarint64(long value, String expected) {
        testVarint64Impl(value, expected,
            ProtobufVarint::writeSignedVarint64, ProtobufVarint::readSignedVarint64);
        testVarint64Impl(value, expected,
            ProtobufVarint::writeUnsignedVarint64, ProtobufVarint::readUnsignedVarint64);

        Assert.assertEquals(Hex.decodeHr(expected).length,
            ProtobufVarint.signedVarint64EncodedLength(value));
        Assert.assertEquals(Hex.decodeHr(expected).length,
            ProtobufVarint.unsignedVarint64EncodedLength(value));
    }

    private void testVarint3264(int value, String expected) {
        testVarint32(value, expected, expected);
        testVarint64(value, expected);
    }


    @Test
    public void varint3264() {
        testVarint3264(0, "00");
        testVarint3264(2, "02");
        testVarint3264(1020, "fc 07");
        testVarint3264(0x7fffffff, "ff ff ff ff 07");
    }

    @Test
    public void varint32() {
        testVarint32(-1, "ff ff ff ff ff ff ff ff ff 01", "ff ff ff ff 0f");
    }

    @Test
    public void varint64() {
        testVarint64(-1, "ff ff ff ff ff ff ff ff ff 01");
    }

    @Test
    public void maxSingleByteValue() {
        testVarint3264(ProtobufVarint.MAX_SINGLE_BYTE_VALUE, "7f");
        testVarint3264(ProtobufVarint.MAX_SINGLE_BYTE_VALUE + 1, "80 01");
    }
}
