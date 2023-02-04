package ru.yandex.realty.matchers;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.regex.Pattern;

/**
 * @author kantemirov
 */
public class FindPatternMatcher extends TypeSafeMatcher<String> {
    private final Pattern pattern;

    public FindPatternMatcher(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    protected boolean matchesSafely(String item) {
        return pattern.matcher(item).find();
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a string matching the pattern '" + pattern + "'");
    }

    public static Matcher<String> findPattern(Pattern pattern) {
        return new FindPatternMatcher(pattern);
    }

    public static Matcher<String> findPattern(String regex) {
        return new FindPatternMatcher(Pattern.compile(regex));
    }
}
