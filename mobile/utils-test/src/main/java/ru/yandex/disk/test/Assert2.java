package ru.yandex.disk.test;

import junit.framework.Assert;
import org.hamcrest.Matcher;

public class Assert2 {

    public static void assertEquals(int expected, int actual) {
        Assert.assertEquals(expected, actual);
    }

    public static void assertEquals(long expected, long actual) {
        Assert.assertEquals(expected, actual);
    }

    public static void assertEquals(Object expected, Object actual) {
        Assert.assertEquals(expected, actual);
    }

    public static void assertEquals(String reason, Object expected, Object actual) {
        Assert.assertEquals(reason, expected, actual);
    }

    public static void assertTrue(boolean condition) {
        Assert.assertTrue(condition);
    }

    public static void assertTrue(String reason, boolean condition) {
        Assert.assertTrue(reason, condition);
    }

    public static void assertFalse(boolean condition) {
        Assert.assertFalse(condition);
    }

    public static void assertFalse(String reason, boolean condition) {
        Assert.assertFalse(reason, condition);
    }

    public static void fail() {
        Assert.fail();
    }

    public static void fail(String reason) {
        Assert.fail(reason);
    }

    public static void assertNotNull(Object object) {
        Assert.assertNotNull(object);
    }

    public static void assertNull(Object object) {
        Assert.assertNull(object);
    }

    public static <T> void assertThat(T actual, Matcher<? super T> matcher) {
        org.hamcrest.MatcherAssert.assertThat(actual, matcher);
    }

    public static <T> void assertThat(String reason, T actual, Matcher<? super T> matcher) {
        org.hamcrest.MatcherAssert.assertThat(reason, actual, matcher);
    }

    public static void assertThat(String reason, boolean assertion) {
        org.hamcrest.MatcherAssert.assertThat(reason, assertion);
    }
}
