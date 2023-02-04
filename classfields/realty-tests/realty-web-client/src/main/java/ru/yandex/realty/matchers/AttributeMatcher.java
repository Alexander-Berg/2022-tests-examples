package ru.yandex.realty.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.openqa.selenium.WebElement;
import ru.yandex.qatools.htmlelements.matchers.common.HasAttributeMatcher;

import static org.hamcrest.CoreMatchers.containsString;

/**
 * Created by ivanvan on 08.08.17.
 */
public class AttributeMatcher extends TypeSafeMatcher<WebElement> {
    private final String attributeName;
    private final Matcher<String> attributeValueMatcher;

    public AttributeMatcher(String attributeName, Matcher<String> attributeValueMatcher) {
        this.attributeName = attributeName;
        this.attributeValueMatcher = attributeValueMatcher;
    }

    public boolean matchesSafely(WebElement item) {
        return this.attributeValueMatcher.matches(item.getAttribute(this.attributeName));
    }

    public void describeTo(Description description) {
        description.appendText("attribute ").appendValue(this.attributeName).appendText(" ")
                .appendDescriptionOf(this.attributeValueMatcher);
    }

    protected void describeMismatchSafely(WebElement item, Description mismatchDescription) {
        mismatchDescription.appendText("attribute ").appendValue(this.attributeName).appendText(" of element ").
                appendValue(item).appendText(" was ").appendValue(item.getAttribute(this.attributeName));
    }

    @Factory
    public static Matcher<WebElement> hasHref(Matcher<String> attributeValueMatcher) {
        return new HasAttributeMatcher("href", attributeValueMatcher);
    }

    @Factory
    public static Matcher<WebElement> isChecked() {
        return new HasAttributeMatcher("class", containsString("_checked"));
    }

    @Factory
    public static Matcher<WebElement> isDisabled() {
        return new HasAttributeMatcher("class", containsString("_disabled"));
    }

    @Factory
    public static Matcher<WebElement> isActive() {
        return new HasAttributeMatcher("class", containsString("_active"));
    }
}
