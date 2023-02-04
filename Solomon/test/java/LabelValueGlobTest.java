package ru.yandex.solomon.labels;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Sergey Polovko
 */
public class LabelValueGlobTest {

    @Test
    public void glob() throws Exception {
        Assert.assertTrue(LabelValueGlob.match("a", "a"));
        Assert.assertTrue(LabelValueGlob.match("ab", "ab"));
        Assert.assertTrue(LabelValueGlob.match("ab?", "abc"));
        Assert.assertTrue(LabelValueGlob.match("a?c", "abc"));
        Assert.assertTrue(LabelValueGlob.match("?bc", "abc"));
        Assert.assertTrue(LabelValueGlob.match("??c", "abc"));
        Assert.assertTrue(LabelValueGlob.match("a??", "abc"));
        Assert.assertTrue(LabelValueGlob.match("???", "abc"));

        Assert.assertTrue(LabelValueGlob.match("", ""));
        Assert.assertTrue(LabelValueGlob.match("*", ""));
        Assert.assertTrue(LabelValueGlob.match("**", ""));
        Assert.assertTrue(LabelValueGlob.match("a*", "abc"));
        Assert.assertTrue(LabelValueGlob.match("ab*", "abc"));
        Assert.assertTrue(LabelValueGlob.match("a*c", "abc"));
        Assert.assertTrue(LabelValueGlob.match("a*c", "aaaccc"));
        Assert.assertTrue(LabelValueGlob.match("*bc", "abc"));
        Assert.assertTrue(LabelValueGlob.match("*c", "abc"));
        Assert.assertTrue(LabelValueGlob.match("*", "abc"));
        Assert.assertTrue(LabelValueGlob.match("*ac", "ac"));
        Assert.assertTrue(LabelValueGlob.match("a*c*de", "abcdfbcde"));
        Assert.assertTrue(LabelValueGlob.match("a*d*g", "abcdefg"));
        Assert.assertTrue(LabelValueGlob.match("a**g", "abcdefg"));
        Assert.assertTrue(LabelValueGlob.match("a***", "a"));
        Assert.assertTrue(LabelValueGlob.match("a***", "abcdefg"));
        Assert.assertTrue(LabelValueGlob.match("**a", "a"));

        Assert.assertFalse(LabelValueGlob.match("", "a"));
        Assert.assertFalse(LabelValueGlob.match("a", ""));
        Assert.assertFalse(LabelValueGlob.match("b", "a"));
        Assert.assertFalse(LabelValueGlob.match("bb?", "abc"));
        Assert.assertFalse(LabelValueGlob.match("?bb", "abc"));
        Assert.assertFalse(LabelValueGlob.match("b?b", "abc"));
        Assert.assertFalse(LabelValueGlob.match("b??", "abc"));
        Assert.assertFalse(LabelValueGlob.match("ab??", "abc"));
        Assert.assertFalse(LabelValueGlob.match("??b", "abc"));

        Assert.assertFalse(LabelValueGlob.match("b*", "abc"));
        Assert.assertFalse(LabelValueGlob.match("bb*", "abc"));
        Assert.assertFalse(LabelValueGlob.match("b*c", "abc"));
        Assert.assertFalse(LabelValueGlob.match("b*c", "bcde"));
        Assert.assertFalse(LabelValueGlob.match("*bb", "abc"));
        Assert.assertFalse(LabelValueGlob.match("*d", "abc"));
        Assert.assertFalse(LabelValueGlob.match("a*z*g", "abcdefg"));
        Assert.assertFalse(LabelValueGlob.match("a*b", "abbe"));
        Assert.assertFalse(LabelValueGlob.match("a*c*g", "abcdefghi"));
    }

    @Test
    public void isGlob() throws Exception {
        Assert.assertTrue(LabelValueGlob.isGlob("solomon-*"));
        Assert.assertTrue(LabelValueGlob.isGlob("solomon-*-00"));
        Assert.assertFalse(LabelValueGlob.isGlob("solomon-man-00"));
    }
}
