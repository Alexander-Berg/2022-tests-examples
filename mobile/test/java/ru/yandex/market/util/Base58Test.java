package ru.yandex.market.util;

import junit.framework.Assert;

import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import ru.yandex.market.BaseTest;
import ru.yandex.market.utils.Base58;
import ru.yandex.market.utils.DigestUtils;

public class Base58Test extends BaseTest {

    @Test
    public void testEmpty() {
        Assert.assertEquals("", new Base58("").encode());
        Assert.assertEquals("", new Base58(new byte[]{}).encode());
        Assert.assertEquals("", new Base58((String) null).encode());
        Assert.assertEquals("", new Base58((byte[]) null).encode());
    }

    @Test
    public void testSimple() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        Assert.assertEquals("3xXCtDCbAJJFUNiMAeZs8QdAqpTA",
                new Base58(DigestUtils.sha1Bytes("fakeHash")).encode());
        Assert.assertEquals("4H6mHmeuu6NGkDtNr8Yp476fLWDm",
                new Base58(DigestUtils.sha1Bytes("23427062-91491")).encode());
    }
}