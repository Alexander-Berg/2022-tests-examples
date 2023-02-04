package ru.yandex.realty.matchers;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.regex.Pattern;

/**
 * @author kantemirov
 */
public class MatchPatternMatcher extends TypeSafeMatcher<String> {
    private final Pattern pattern;

    public MatchPatternMatcher(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    protected boolean matchesSafely(String item) {
        return pattern.matcher(item).matches();
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a string matching the pattern '" + pattern + "'");
    }

    public static Matcher<String> matchesPattern(Pattern pattern) {
        return new MatchPatternMatcher(pattern);
    }

    public static Matcher<String> matchesPattern(String regex) {
        return new MatchPatternMatcher(Pattern.compile(regex));
    }
}
