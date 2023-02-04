package ru.yandex.realty.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

/**
 * Created by vicdev on 16.08.17.
 */
public class IsSelectedMatcher extends TypeSafeMatcher<WebElement> {

    private IsSelectedMatcher() {
    }

    @Override
    protected boolean matchesSafely(WebElement element) {
        try {
            return element.getAttribute("class").contains("_selected");
        } catch (WebDriverException e) {
            return false;
        }
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("element is selected");
    }

    @Override
    public void describeMismatchSafely(WebElement element, Description mismatchDescription) {
        mismatchDescription.appendText("element ").appendValue(element).appendText(" is not selected");
    }

    @Factory
    public static Matcher<WebElement> isSelected() {
        return new IsSelectedMatcher();
    }
}
