package ru.yandex.wmtools.common.data.info;

import org.junit.Test;
import ru.yandex.wmtools.common.error.UserException;

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;

/**
 * @author avhaliullin
 */
public class IPInfoTest {
    @Test
    public void testIPv4() throws UserException {
        assertEquals(new IPv4Info(ipv4longRepr(127, 0, 0, 1), 32), IPInfo.createFromString("127.0.0.1"));
        assertEquals(new IPv4Info(ipv4longRepr(255, 255, 255, 255), 32), IPInfo.createFromString("255.255.255.255", 32));
        assertEquals(new IPv4Info(ipv4longRepr(255, 255, 255, 0), 24), IPInfo.createFromString("255.255.255.255", 24));
        assertEquals(new IPv4Info(ipv4longRepr(192, 168, 0, 1), 32), IPInfo.createFromString("::ffff:192.168.0.1"));
    }

    @Test
    public void testIPv6() throws UserException {
        assertEquals(new IPv6Info(ipv6bigIntRepr(0xabcd, 0xeffe, 0, 0, 0, 0, 0x1, 0x1234), 128), IPInfo.createFromString("abcd:effe:0:0:0:0:1:1234"));
        assertEquals(new IPv6Info(ipv6bigIntRepr(0xabcd, 0xeffe, 0, 0, 0, 0, 0x1, 0x1234), 128), IPInfo.createFromString("abcd:effe::1:1234"));
        assertEquals(new IPv6Info(ipv6bigIntRepr(0xabcd, 0xeffe, 0, 0, 0, 0, 0, 0), 128), IPInfo.createFromString("abcd:effe::"));
        assertEquals(new IPv6Info(ipv6bigIntRepr(0, 0, 0, 0, 0, 0, 0x1, 0x1234), 128), IPInfo.createFromString("::1:1234"));
        assertEquals(new IPv6Info(ipv6bigIntRepr(0, 0, 0, 0, 0, 0, 0x1, 0), 112), IPInfo.createFromString("::1:1234", 112));
        assertEquals(new IPv6Info(ipv6bigIntRepr(0, 0, 0, 0, 0, 0, 0, 0), 128), IPInfo.createFromString("::"));
    }

    private long ipv4longRepr(int part1, int part2, int part3, int part4) {
        return ((long) part1 << 24) + (part2 << 16) + (part3 << 8) + part4;
    }

    private BigInteger ipv6bigIntRepr(int part1, int part2, int part3, int part4, int part5, int part6, int part7, int part8) {
        return BigInteger.valueOf(part1).shiftLeft(16)
                .add(BigInteger.valueOf(part2)).shiftLeft(16)
                .add(BigInteger.valueOf(part3)).shiftLeft(16)
                .add(BigInteger.valueOf(part4)).shiftLeft(16)
                .add(BigInteger.valueOf(part5)).shiftLeft(16)
                .add(BigInteger.valueOf(part6)).shiftLeft(16)
                .add(BigInteger.valueOf(part7)).shiftLeft(16)
                .add(BigInteger.valueOf(part8));
    }
}
