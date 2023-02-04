package ru.auto.tests.desktop.matchers;

import lombok.Setter;
import lombok.experimental.Accessors;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.TypeSafeMatcher;
import org.openqa.selenium.WebDriver;
import ru.lanwen.diff.uri.UriDiffer;
import ru.lanwen.diff.uri.core.UriDiff;
import ru.lanwen.diff.uri.core.filters.UriDiffFilter;

import java.util.ArrayList;
import java.util.List;

@Accessors(chain = true)
public class UrlMatcher extends TypeSafeMatcher<WebDriver> {

    @Setter
    private List<UriDiffFilter> filters;
    private String expectedUrl;
    private UriDiff changes;

    private UrlMatcher(String url) {
        this.filters = new ArrayList<>();
        this.expectedUrl = url;
    }

    @Override
    protected boolean matchesSafely(WebDriver webDriver) {
        String actualUrl = webDriver.getCurrentUrl();
        changes = UriDiffer.diff().expected(expectedUrl).actual(actualUrl).filter(filters).changes();

        return !changes.hasChanges();
    }

    @Override
    protected void describeMismatchSafely(WebDriver webDriver, Description mismatchDescription) {
        mismatchDescription.appendText(String.format("Получили url: %s", changes.report()));
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(String.format("Должен быть url %s", expectedUrl));
    }

    @Factory
    public static UrlMatcher hasNoDiffWithUrl(String url) {
        return new UrlMatcher(url);
    }
}
