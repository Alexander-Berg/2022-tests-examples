package ru.yandex.realty.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.openqa.selenium.WebDriver;

/**
 * Created by vicdev on 26.05.17.
 */
public class CurrentUrlMatcher extends TypeSafeMatcher<WebDriver> {
    private Matcher matcher;

    private CurrentUrlMatcher(Matcher matcher) {
        this.matcher = matcher;
    }

    @Override
    protected boolean matchesSafely(WebDriver driver) {
        return matcher.matches(driver.getCurrentUrl());
    }

    @Override
    protected void describeMismatchSafely(WebDriver driver, Description mismatchDescription) {
        mismatchDescription.appendText(String.format("Получили url: %s", driver.getCurrentUrl()));
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(String.format("Ожидали: %s", matcher.toString()));
    }

    @Factory
    public static CurrentUrlMatcher shouldUrl(Matcher matcher) {
        return new CurrentUrlMatcher(matcher);
    }

}
