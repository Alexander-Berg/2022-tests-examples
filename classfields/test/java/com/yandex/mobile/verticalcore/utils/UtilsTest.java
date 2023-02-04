package com.yandex.mobile.verticalcore.utils;

import org.junit.Assert;
import org.junit.Test;

/**
 * TODO: Add destription
 *
 * @author ironbcc on 05.07.16.
 */
public class UtilsTest {
    @Test
    public void isEmptyTest() {
        Object o = "abc";
        Assert.assertEquals(false, Utils.isEmpty(o));
        Object o1 = "";
        Assert.assertEquals(true, Utils.isEmpty(o1));
        Object o2 = new StringBuilder(10);
        Assert.assertEquals(true, Utils.isEmpty(o2));
        Assert.assertEquals(true, Utils.isEmpty((Object[]) null));
        CharSequence s = "abc";
        Assert.assertEquals(false, Utils.isEmpty(s));
        CharSequence s1 = "";
        Assert.assertEquals(true, Utils.isEmpty(s1));
        CharSequence s2 = "";
        Assert.assertEquals(true, Utils.isEmpty(s2));

        try {
            Utils.isEmpty(new Object());
            Assert.assertEquals(true, false);
        } catch (IllegalArgumentException e) {
            Assert.assertEquals(true, true);
        }

        Assert.assertEquals(true, Utils.isEmpty(new Object[0]));
        Assert.assertEquals(false, Utils.isEmpty(new Object[1]));
    }
}
