package ru.yandex.solomon.labels;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

import static ru.yandex.solomon.labels.InterpolatedString.isInterpolatedString;

/**
 * @author Stepan Koltsov
 */
public class InterpolatedStringTest {

    @Test
    public void testToString() {
        Assert.assertEquals("{{aa}}bb", InterpolatedString.parse("{{aa}}bb").toString());
    }

    @Test
    public void eval() {
        Assert.assertEquals("aa", InterpolatedString.parse("aa").eval(x -> { throw new AssertionError(); }));
        Assert.assertEquals("aaBB", InterpolatedString.parse("aa{{bb}}").eval(String::toUpperCase));
        Assert.assertEquals("AAbb", InterpolatedString.parse("{{aa}}bb").eval(String::toUpperCase));
        Assert.assertEquals("AAbbCCdd", InterpolatedString.parse("{{aa}}bb{{cc}}dd").eval(String::toUpperCase));
    }

    @Test
    public void interpolatedStringCheck() throws Exception {
        Assert.assertTrue(isInterpolatedString("aa{{bb}}"));
        Assert.assertTrue(isInterpolatedString("{{aa}}bb{{cc}}dd"));
        Assert.assertFalse(isInterpolatedString("test"));
        Assert.assertFalse(isInterpolatedString("test test test"));
    }

    @Test
    public void partialEval() {
        Assert.assertEquals(InterpolatedString.parse("aaBBcc{{dd}}"), InterpolatedString.parse("aa{{bb}}cc{{dd}}").partialEval(s -> {
            switch (s) {
                case "bb": return Optional.of(s.toUpperCase());
                default: return Optional.empty();
            }
        }));
    }

}
