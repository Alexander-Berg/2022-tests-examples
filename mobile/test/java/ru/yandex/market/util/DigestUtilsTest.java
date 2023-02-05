package ru.yandex.market.util;

import junit.framework.Assert;

import org.junit.Test;

import ru.yandex.market.BaseTest;
import ru.yandex.market.utils.DigestUtils;

public class DigestUtilsTest extends BaseTest {

    @Test
    public void md5Hex() {
        Assert.assertEquals("d41d8cd98f00b204e9800998ecf8427e", DigestUtils.md5Hex(""));
        Assert.assertEquals("7215ee9c7d9dc229d2921a40e899ec5f", DigestUtils.md5Hex(" "));
        Assert.assertEquals("c4ca4238a0b923820dcc509a6f75849b", DigestUtils.md5Hex("1"));
        Assert.assertEquals("827ccb0eea8a706c4c34a16891f84e7b", DigestUtils.md5Hex("12345"));
    }

    @Test
    public void sha1Hex() {
        Assert.assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", DigestUtils.sha1Hex(""));
        Assert.assertEquals("b858cb282617fb0956d960215c8e84d1ccf909c6", DigestUtils.sha1Hex(" "));
        Assert.assertEquals("356a192b7913b04c54574d18c28d46e6395428ab", DigestUtils.sha1Hex("1"));
        Assert.assertEquals("8cb2237d0679ca88db6464eac60da96345513964",
                DigestUtils.sha1Hex("12345"));
    }

    @Test
    public void hex() {
        Assert.assertEquals("", DigestUtils.hex("".getBytes()));
        Assert.assertEquals("20", DigestUtils.hex(" ".getBytes()));
        Assert.assertEquals("31", DigestUtils.hex("1".getBytes()));
        Assert.assertEquals("3132333435", DigestUtils.hex("12345".getBytes()));
    }

    @Test
    public void base58Sha1String() {
        Assert.assertEquals("3xXCtDCbAJJFUNiMAeZs8QdAqpTA",
                DigestUtils.base58Sha1String("fakeHash"));
        Assert.assertEquals("4H6mHmeuu6NGkDtNr8Yp476fLWDm",
                DigestUtils.base58Sha1String("23427062-91491"));
    }

}