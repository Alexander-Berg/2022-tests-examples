package ru.yandex.realty.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.openqa.selenium.WebDriver;
import ru.lanwen.diff.uri.UriDiffer;
import ru.lanwen.diff.uri.core.UriDiff;
import ru.lanwen.diff.uri.core.filters.AnyParamValueFilter;
import ru.lanwen.diff.uri.core.filters.UriDiffFilter;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static ru.lanwen.diff.uri.core.filters.AnyParamValueFilter.param;

/**
 * Created by kopitsa on 10.07.17.
 */
public class UrlMatcher extends TypeSafeMatcher<WebDriver> {

    private String expectedUrl;
    private List<UriDiffFilter> filters;
    private UriDiff changes;

    private UrlMatcher(String url, List<UriDiffFilter> filters) {
        this.filters = filters;
        this.expectedUrl = url;
    }

    @Override
    protected boolean matchesSafely(WebDriver webDriver) {
        String actualUrl = webDriver.getCurrentUrl();
        changes = UriDiffer.diff()
                .expected(expectedUrl).actual(actualUrl).filter(filters).changes();

        return !changes.hasChanges();
    }

    @Override
    protected void describeMismatchSafely(WebDriver webDriver, Description mismatchDescription) {
        mismatchDescription.appendText(String.format("Получили отчет: %s", changes.report()));
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(String.format("Должен быть url %s", expectedUrl));
    }

    @Factory
    public static UrlMatcher hasNoDiffWithUrl(String url, List<UriDiffFilter> filters) {
        return new UrlMatcher(url, filters);
    }

    @Override
    public String toString() {
        return "Проверяем, что находимся на урле " + expectedUrl;
    }
}
