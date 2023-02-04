package ru.yandex.webmaster3.core.util;

import com.google.common.primitives.UnsignedLong;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author avhaliullin
 */
public class CHIntHashTest {
    @Test
    public void testHash(){
        Assert.assertEquals(0, chHash(13729));
        Assert.assertEquals(1, chHash(124919));
        Assert.assertEquals(2, chHash(97869));
        Assert.assertEquals(3, chHash(19319));
        Assert.assertEquals(4, chHash(6669));
        Assert.assertEquals(5, chHash(29819));
        Assert.assertEquals(6, chHash(6969));
        Assert.assertEquals(7, chHash(44139));
        Assert.assertEquals(8, chHash(809));
        Assert.assertEquals(9, chHash(83309));
    }

    private static int chHash(long uid) {
        UnsignedLong hash = CHIntHash.intHash64Unsigned(uid);
        return hash.mod(UnsignedLong.valueOf(10)).intValue();
    }
}
