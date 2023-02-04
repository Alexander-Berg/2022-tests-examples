package ru.yandex.solomon.labels.query.matchers;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Oleg Baryshnikov
 */
public class GlobMatcherTest {

    @Test
    public void matchesExact() {
        GlobMatcher matcher = new ExactGlobMatcher("exact");
        assertTrue(matcher.matches("exact"));
        assertFalse(matcher.matches("not exact"));
    }

    @Test
    public void matchesGlob() {
        GlobMatcher matcher = new SingleGlobMatcher("glob-*");
        assertFalse(matcher.matches("glob"));
        assertTrue(matcher.matches("glob-"));
        assertTrue(matcher.matches("glob-1"));
    }

    @Test
    public void matchesMultiGlob() {
        GlobMatcher matcher = new MultiGlobMatcher("glob-*|exact");
        assertFalse(matcher.matches("glob"));
        assertTrue(matcher.matches("glob-"));
        assertTrue(matcher.matches("glob-1"));
        assertTrue(matcher.matches("exact"));
        assertFalse(matcher.matches("not exact"));
    }

    @Test
    public void fromExact() {
        GlobMatcher actual = GlobMatcher.from("exact");
        GlobMatcher expected = new ExactGlobMatcher("exact");
        assertEquals(expected, actual);
    }

    @Test
    public void fromSingleGlob() {
        GlobMatcher actual = GlobMatcher.from("glob-*");
        GlobMatcher expected = new SingleGlobMatcher("glob-*");
        assertEquals(expected, actual);
    }

    @Test
    public void fromMultiGlob() {
        GlobMatcher actual = GlobMatcher.from("glob-*|exact");
        GlobMatcher expected = new MultiGlobMatcher("glob-*|exact");
        assertEquals(expected, actual);
    }
}
