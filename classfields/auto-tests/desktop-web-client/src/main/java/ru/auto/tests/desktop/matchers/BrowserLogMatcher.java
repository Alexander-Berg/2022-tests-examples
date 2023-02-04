package ru.auto.tests.desktop.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

public class BrowserLogMatcher extends TypeSafeMatcher<LogEntries> {

    private final Matcher<String> elementMatcher;

    public BrowserLogMatcher(Matcher<String> elementMatcher) {
        this.elementMatcher = elementMatcher;
    }

    @Override
    protected boolean matchesSafely(LogEntries logEntries) {
        for (LogEntry logEntry : logEntries) {
            if (this.elementMatcher.matches(logEntry.getMessage())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a browser logs containing ").appendDescriptionOf(elementMatcher);
    }

    @Override
    protected void describeMismatchSafely(LogEntries logEntries, Description mismatchDescription) {
        mismatchDescription.appendText("was browser logs ").appendValue(logEntries.toJson().toString());
    }

    @Factory
    public static BrowserLogMatcher hasLogItem(String source, String message) {
        return new BrowserLogMatcher(allOf(
                containsString(source),
                containsString(message)
        ));
    }

    @Factory
    public static BrowserLogMatcher hasLogItem(Matcher<String> elementMatcher) {
        return new BrowserLogMatcher(elementMatcher);
    }
}
